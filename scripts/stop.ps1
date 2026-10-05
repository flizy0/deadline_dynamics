param([string]$RuntimeDirectory, [switch]$AppOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $RuntimeDirectory) { $RuntimeDirectory = Join-Path $projectRoot '.runtime' }
$RuntimeDirectory = [System.IO.Path]::GetFullPath($RuntimeDirectory)
$pidFile = Join-Path $RuntimeDirectory 'app.pid'
if (Test-Path -LiteralPath $pidFile) {
    $appProcessId = [int](Get-Content -LiteralPath $pidFile -Raw)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $appProcessId" -ErrorAction SilentlyContinue
    if ($process -and $process.Name -eq 'java.exe' -and $process.CommandLine.Contains($projectRoot)) {
        Stop-Process -Id $appProcessId
        Remove-Item -LiteralPath $pidFile
    }
}
$marker = Join-Path $RuntimeDirectory 'deadline-runtime.json'
if (-not $AppOnly -and (Test-Path -LiteralPath $marker)) {
    $owner = Get-Content -LiteralPath $marker -Raw | ConvertFrom-Json
    $expectedData = Join-Path $RuntimeDirectory 'postgres-data'
    if ($owner.project -ne $projectRoot -or $owner.data -ne $expectedData) { throw 'Runtime ownership mismatch.' }
    & (Join-Path $owner.postgresBin 'pg_ctl.exe') stop -D $expectedData -m fast -w
}
