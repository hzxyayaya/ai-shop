param(
    [Parameter(Mandatory = $true)]
    [string]$NotifyBaseUrl
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outputDir = Join-Path $root 'output'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$stdoutPath = Join-Path $outputDir "backend-tunnel-$runId.log"
$stderrPath = Join-Path $outputDir "backend-tunnel-$runId.err.log"
$startScript = Join-Path $root 'start-backend.ps1'

if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

Start-Process -FilePath 'powershell.exe' `
    -ArgumentList '-ExecutionPolicy','Bypass','-File',"`"$startScript`"","-NotifyBaseUrl",$NotifyBaseUrl `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath `
    -WorkingDirectory $root `
    -WindowStyle Hidden

Write-Output "backend start requested, log=$stdoutPath"
