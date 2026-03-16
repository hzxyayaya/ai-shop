param(
    [string]$Subdomain = ("ai-shop-" + (Get-Date -Format 'MMddHHmmss'))
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outputDir = Join-Path $root 'output'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$logPath = Join-Path $outputDir "localtunnel-$runId.log"
$errPath = Join-Path $outputDir "localtunnel-$runId.err.log"
$latestUrlPath = Join-Path $outputDir 'localtunnel.url.txt'
$latestLogPath = Join-Path $outputDir 'localtunnel.path.txt'

if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

if (Test-Path $latestUrlPath) {
    Remove-Item $latestUrlPath -Force
}

if (Test-Path $latestLogPath) {
    Remove-Item $latestLogPath -Force
}

$npx = (Get-Command npx.cmd -ErrorAction Stop).Source

Start-Process -FilePath $npx `
    -ArgumentList 'localtunnel','--port','8080','--subdomain',$Subdomain `
    -RedirectStandardOutput $logPath `
    -RedirectStandardError $errPath `
    -WindowStyle Hidden

$url = "https://$Subdomain.loca.lt"
Set-Content -Path $latestUrlPath -Value $url
Set-Content -Path $latestLogPath -Value $logPath
Write-Output "localtunnel started: $url"
