param([string]$Destination)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
if (-not $Destination) { $Destination = Join-Path (Split-Path $project -Parent) 'deadline-lab.zip' }
$Destination = [System.IO.Path]::GetFullPath($Destination)
$files = @()
foreach ($folder in @('src', 'docs', 'scripts')) {
    $path = Join-Path $project $folder
    if (Test-Path -LiteralPath $path) {
        $files += Get-ChildItem -LiteralPath $path -Recurse -File |
            Where-Object { $_.Name -notin @('product-regression-failure.png', 'product-regression-failure.json') }
    }
}
$previews = Join-Path $project 'previews'
if (Test-Path -LiteralPath $previews) {
    $currentChecks = @('product-overview-desktop.png', 'product-overview-mobile-kk.png',
        'product-data-mobile-kk.png', 'product-formulas-mobile-kk.png',
        'product-lab-comparison-desktop.png', 'product-pair-mobile-kk.png')
    $files += Get-ChildItem -LiteralPath $previews -File |
        Where-Object { $_.Name -like 'real-*.png' -or $_.Name -in $currentChecks }
}
foreach ($name in @('pom.xml', 'package.json', 'README.md', 'SITE_HANDOFF.md', 'Dockerfile', 'compose.yaml', '.env.example', '.gitignore', '.dockerignore', 'start-local.cmd', 'app.jar')) {
    $path = Join-Path $project $name
    if (Test-Path -LiteralPath $path) { $files += Get-Item -LiteralPath $path }
}
if (-not (Test-Path -LiteralPath (Join-Path $project 'app.jar'))) { throw 'Build and copy app.jar before packaging.' }
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stream = [System.IO.File]::Open($Destination, [System.IO.FileMode]::Create)
$archive = New-Object System.IO.Compression.ZipArchive($stream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($project.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, ('deadline-lab/' + $relative)) | Out-Null
    }
} finally { $archive.Dispose(); $stream.Dispose() }
$archive = [System.IO.Compression.ZipFile]::OpenRead($Destination)
try {
    foreach ($entry in $archive.Entries) {
        if ($entry.FullName -match '/(local\.env|\.env|\.runtime|target)(/|$)') { throw 'Private/runtime file found in package.' }
    }
    Write-Host "Package verified: $($archive.Entries.Count) entries, no local.env, .env, .runtime or target."
    Write-Host $Destination
} finally { $archive.Dispose() }
