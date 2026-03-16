param(
    [string]$NotifyBaseUrl = $env:ALIPAY_CALLBACK_BASE_URL
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot 'backend\mall-backend'

if (-not $NotifyBaseUrl) {
    Write-Host 'Missing notify base URL for local callback tunnel.' -ForegroundColor Red
    Write-Host 'Pass -NotifyBaseUrl or set ALIPAY_CALLBACK_BASE_URL before starting.' -ForegroundColor Yellow
    Write-Host 'Example:' -ForegroundColor Yellow
    Write-Host '  .\\start-backend.ps1 -NotifyBaseUrl https://your-tunnel-domain.example.com' -ForegroundColor Cyan
    exit 1
}

$normalizedNotifyBaseUrl = $NotifyBaseUrl.TrimEnd('/')
[Environment]::SetEnvironmentVariable('ALIPAY_NOTIFY_URL', "$normalizedNotifyBaseUrl/api/payments/callback/alipay", 'Process')

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host 'Maven (mvn) was not found in PATH.' -ForegroundColor Red
    exit 1
}

Write-Host 'Starting backend with ALIPAY_NOTIFY_URL override...' -ForegroundColor Green
Write-Host "Backend directory: $backendDir" -ForegroundColor Cyan

Set-Location $backendDir
mvn spring-boot:run
