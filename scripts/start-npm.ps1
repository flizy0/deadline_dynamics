$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$port = 8085
$configPath = Join-Path $projectRoot 'local.env'

if (Test-Path -LiteralPath $configPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($configPath)) {
        if ($line -match '^\s*SERVER_PORT\s*=\s*(\d+)\s*$') {
            $port = [int]$Matches[1]
        }
    }
}

$listeners = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique)
if ($listeners.Count -gt 0) {
    $processes = @($listeners | ForEach-Object {
        Get-CimInstance Win32_Process -Filter "ProcessId = $_" -ErrorAction SilentlyContinue
    })
    $isOwnApp = $processes.Count -eq 1 -and $processes[0].Name -eq 'java.exe' -and
        $processes[0].CommandLine.Contains($projectRoot)
    if (-not $isOwnApp) {
        throw "Port $port is occupied by another process. Existing processes were not changed."
    }

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$port/" -TimeoutSec 10
        if ($response.StatusCode -ne 200) { throw 'Unexpected HTTP status.' }
    } catch {
        throw "The Deadline Dynamics process owns port $port but is not responding. Check .runtime/application-error.log."
    }

    Write-Host "Deadline Dynamics is already running: http://localhost:$port"
    exit 0
}

& (Join-Path $PSScriptRoot 'start.ps1') -Background
