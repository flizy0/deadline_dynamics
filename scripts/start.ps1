param(
    [string]$JavaHome,
    [string]$MavenHome,
    [string]$PostgresBin,
    [string]$RuntimeDirectory,
    [switch]$SkipBuild,
    [switch]$Background
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $RuntimeDirectory) { $RuntimeDirectory = Join-Path $projectRoot '.runtime' }
$RuntimeDirectory = [System.IO.Path]::GetFullPath($RuntimeDirectory)
New-Item -ItemType Directory -Force -Path $RuntimeDirectory | Out-Null

function New-Secret {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

$configPath = Join-Path $projectRoot 'local.env'
if (-not (Test-Path -LiteralPath $configPath)) {
    $newConfig = @(
        'DATABASE_URL=jdbc:postgresql://127.0.0.1:55432/deadline_lab'
        'DB_USERNAME=deadline'
        ('DB_PASSWORD=' + (New-Secret))
        'ADMIN_USERNAME=admin'
        ('ADMIN_PASSWORD=' + (New-Secret))
        'SERVER_PORT=8085'
        'SERVER_ADDRESS=127.0.0.1'
    )
    [System.IO.File]::WriteAllLines($configPath, $newConfig, (New-Object System.Text.UTF8Encoding $false))
}
foreach ($line in [System.IO.File]::ReadAllLines($configPath)) {
    if ($line.Trim() -and -not $line.TrimStart().StartsWith('#')) {
        $pair = $line.Split([char[]]@('='), 2, [StringSplitOptions]::None)
        if ($pair.Length -eq 2) { [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1], 'Process') }
    }
}

if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $JavaHome) {
    $candidate = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) { $JavaHome = $candidate.FullName }
}
if ($JavaHome) { $java = Join-Path $JavaHome 'bin\java.exe'; $env:JAVA_HOME = $JavaHome }
else { $java = (Get-Command java -ErrorAction SilentlyContinue).Source }
if (-not $java -or -not (Test-Path -LiteralPath $java)) { throw 'Java 21+ not found. Set JAVA_HOME or pass -JavaHome.' }

$jar = Join-Path $projectRoot 'app.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    $jar = Join-Path $projectRoot 'target\deadline-lab-1.0.0.jar'
}
if (-not $SkipBuild -and -not (Test-Path -LiteralPath (Join-Path $projectRoot 'app.jar'))) {
    if ($MavenHome) { $maven = Join-Path $MavenHome 'bin\mvn.cmd' }
    else { $maven = (Get-Command mvn -ErrorAction SilentlyContinue).Source }
    if (-not $maven) {
        $maven = Get-ChildItem -Path "$env:ProgramFiles\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd" -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $maven) { throw 'Maven not found. Open pom.xml in IntelliJ, build package, then use -SkipBuild.' }
    Push-Location $projectRoot
    try {
        & $maven "-Dmaven.repo.local=$RuntimeDirectory\m2" package
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
    } finally { Pop-Location }
}
if (-not (Test-Path -LiteralPath $jar)) { throw 'Application JAR not found. Build with Maven first.' }

# The dedicated local cluster never touches an existing PostgreSQL service.
$localDatabase = $env:DATABASE_URL -match '^jdbc:postgresql://127\.0\.0\.1:55432/deadline_lab$'
if ($localDatabase) {
    if (-not $PostgresBin) {
        $pg = Get-ChildItem -Path "$env:ProgramFiles\PostgreSQL\*\bin\pg_ctl.exe" -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($pg) { $PostgresBin = Split-Path $pg.FullName -Parent }
    }
    if (-not $PostgresBin -or -not (Test-Path -LiteralPath (Join-Path $PostgresBin 'pg_ctl.exe'))) {
        throw 'PostgreSQL binaries not found. Pass -PostgresBin or configure an existing database in local.env.'
    }
    $dataDirectory = Join-Path $RuntimeDirectory 'postgres-data'
    $marker = Join-Path $RuntimeDirectory 'deadline-runtime.json'
    if (-not (Test-Path -LiteralPath (Join-Path $dataDirectory 'PG_VERSION'))) {
        $occupied = Get-NetTCPConnection -State Listen -LocalPort 55432 -ErrorAction SilentlyContinue
        if ($occupied) { throw 'Port 55432 is in use. Set DATABASE_URL to your own database; no existing service was changed.' }
        $passwordFile = Join-Path $RuntimeDirectory 'postgres-init-password.tmp'
        [System.IO.File]::WriteAllText($passwordFile, $env:DB_PASSWORD, (New-Object System.Text.UTF8Encoding $false))
        try {
            & (Join-Path $PostgresBin 'initdb.exe') -D $dataDirectory --username=$env:DB_USERNAME --encoding=UTF8 --locale=C --auth=scram-sha-256 --pwfile=$passwordFile
            if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL initialization failed.' }
        } finally { Remove-Item -LiteralPath $passwordFile -ErrorAction SilentlyContinue }
        @{project=$projectRoot; postgresBin=$PostgresBin; data=$dataDirectory} | ConvertTo-Json |
            Set-Content -LiteralPath $marker -Encoding UTF8
    }
    if (-not (Test-Path -LiteralPath $marker)) { throw 'Unrecognized PostgreSQL directory. Refusing to start it.' }
    $owner = Get-Content -LiteralPath $marker -Raw | ConvertFrom-Json
    if ($owner.project -ne $projectRoot -or $owner.data -ne $dataDirectory) { throw 'Runtime directory belongs to another project.' }
    & (Join-Path $PostgresBin 'pg_ctl.exe') status -D $dataDirectory | Out-Null
    if ($LASTEXITCODE -ne 0) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') start -D $dataDirectory -l (Join-Path $RuntimeDirectory 'postgres.log') -o '-p 55432 -h 127.0.0.1' -w
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. Check postgres.log.' }
    }
    $env:PGPASSWORD = $env:DB_PASSWORD
    try {
        $exists = & (Join-Path $PostgresBin 'psql.exe') -h 127.0.0.1 -p 55432 -U $env:DB_USERNAME -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='deadline_lab'"
        if ($LASTEXITCODE -ne 0) { throw 'Cannot authenticate to local PostgreSQL. Check local.env.' }
        if ([string]::IsNullOrWhiteSpace($exists) -or $exists.Trim() -ne '1') {
            & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p 55432 -U $env:DB_USERNAME deadline_lab
            if ($LASTEXITCODE -ne 0) { throw 'Database creation failed.' }
        }
    } finally { Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue }
}

$port = [int]$env:SERVER_PORT
if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
    throw "Port $port is occupied. Change SERVER_PORT in local.env. Existing processes were not stopped."
}
Write-Host "Application: http://localhost:$port"
Write-Host "Admin credentials: $configPath (do not share this file)."
if ($Background) {
    $runtimeJar = Join-Path $RuntimeDirectory 'application.jar'
    $emptyInput = Join-Path $RuntimeDirectory 'empty-input.txt'
    if (-not (Test-Path -LiteralPath $emptyInput)) {
        [System.IO.File]::WriteAllText($emptyInput, '')
    }
    Copy-Item -LiteralPath $jar -Destination $runtimeJar -Force
    $process = Start-Process -FilePath $java -ArgumentList @('-jar', ('"' + $runtimeJar + '"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardInput $emptyInput `
        -RedirectStandardOutput (Join-Path $RuntimeDirectory 'application.log') `
        -RedirectStandardError (Join-Path $RuntimeDirectory 'application-error.log')
    [System.IO.File]::WriteAllText((Join-Path $RuntimeDirectory 'app.pid'), [string]$process.Id)
    Write-Host 'Started in background. Use scripts/stop.ps1 with the same RuntimeDirectory to stop.'
} else {
    & $java -jar $jar
}
