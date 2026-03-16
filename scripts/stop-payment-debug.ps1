$ErrorActionPreference = 'SilentlyContinue'

$processes = Get-CimInstance Win32_Process | Where-Object {
    ($_.Name -match 'node|cmd|powershell|java') -and (
        $_.CommandLine -match 'localtunnel' -or
        $_.CommandLine -match 'start-backend\.ps1' -or
        $_.CommandLine -match 'spring-boot:run' -or
        $_.CommandLine -match 'frontend' -and $_.CommandLine -match 'vite'
    )
}

foreach ($process in $processes) {
    Stop-Process -Id $process.ProcessId -Force
}

Write-Output "Stopped $($processes.Count) payment debug process(es)."
