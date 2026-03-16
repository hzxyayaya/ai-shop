param(
    [string]$FrontendHost = 'localhost',
    [int]$FrontendPort = 5173,
    [int]$BackendPort = 8080,
    [int]$TunnelWaitSeconds = 25,
    [int]$BackendWaitSeconds = 20
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outputDir = Join-Path $root 'output'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$frontendStdout = Join-Path $outputDir "frontend-$runId.log"
$frontendStderr = Join-Path $outputDir "frontend-$runId.err.log"
$tunnelLog = Join-Path $outputDir 'localtunnel.log'
$tunnelErr = Join-Path $outputDir 'localtunnel.err.log'
$tunnelUrlFile = Join-Path $outputDir 'localtunnel.url.txt'

if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

function Stop-DebugProcesses {
    $patterns = @('localtunnel', 'start-backend.ps1', 'spring-boot:run', 'frontend')
    $processes = Get-CimInstance Win32_Process | Where-Object {
        $commandLine = $_.CommandLine
        ($_.Name -match 'node|cmd|powershell|java') -and ($patterns | Where-Object { $commandLine -match [regex]::Escape($_) })
    }
    foreach ($process in $processes) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Wait-ForBackend {
    $deadline = (Get-Date).AddSeconds($BackendWaitSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$BackendPort/api/products?page=1&pageSize=1" -TimeoutSec 5 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    throw "Backend did not become ready on port $BackendPort"
}

Stop-DebugProcesses
Start-Sleep -Seconds 2

Write-Output 'Starting localtunnel...'
Start-Process -FilePath 'powershell.exe' `
    -ArgumentList '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $root 'scripts\start-localtunnel.ps1') `
    -WindowStyle Hidden `
    -Wait | Out-Null
$notifyBaseUrl = Get-Content $tunnelUrlFile -Raw
Start-Sleep -Seconds 3

Write-Output "Starting backend with notify URL: $notifyBaseUrl"
Start-Process -FilePath 'powershell.exe' `
    -ArgumentList '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $root 'scripts\start-backend-tunneled.ps1'), '-NotifyBaseUrl', $notifyBaseUrl `
    -WindowStyle Hidden `
    -Wait | Out-Null
Wait-ForBackend

Write-Output 'Starting frontend...'
$frontendCommand = "Set-Location '$($frontendDir.Replace("'", "''"))'; npm run dev -- --host $FrontendHost --port $FrontendPort *> '$($frontendStdout.Replace("'", "''"))'"
Start-Process -FilePath 'powershell.exe' `
    -ArgumentList '-ExecutionPolicy', 'Bypass', '-Command', $frontendCommand `
    -WindowStyle Hidden | Out-Null
Start-Sleep -Seconds 5

Write-Output ''
Write-Output 'Payment debug environment started.'
Write-Output "Frontend: http://$FrontendHost`:$FrontendPort"
Write-Output "Notify tunnel: $notifyBaseUrl"
Write-Output ''
Write-Output "Logs:"
Write-Output "  Tunnel out:   $tunnelLog"
Write-Output "  Tunnel err:   $tunnelErr"
Write-Output "  Frontend out: $frontendStdout"
Write-Output "  Frontend err: $frontendStderr"
