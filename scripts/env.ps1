# Puts the portable JDK, Maven and PostgreSQL (in %LOCALAPPDATA%\capstone-tools) on PATH for this shell.
# If you installed them system-wide instead, you don't need this file.
$tools = Join-Path $env:LOCALAPPDATA 'capstone-tools'
$jdk = Get-ChildItem $tools -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) {
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$($jdk.FullName)\bin;$tools\apache-maven-3.9.9\bin;$tools\pgsql\bin;$env:Path"
    Write-Host "Using JDK at $($jdk.FullName)"
} else {
    Write-Host "Portable tools not found in $tools - using whatever java/mvn/psql are on PATH."
}
$env:PGDATA_DIR = Join-Path $tools 'pgdata'
