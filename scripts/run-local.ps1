param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $key, $value = $line -split "=", 2
        [Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim(), "Process")
    }
}

$mvn = ".\.tools\apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    $mvn = "mvn"
}

& $mvn spring-boot:run
