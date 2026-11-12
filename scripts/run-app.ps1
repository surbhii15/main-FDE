# Builds and runs the API on http://localhost:8080 (Swagger UI at /swagger-ui.html).
#   .\scripts\run-app.ps1          - run with mvn spring-boot:run
#   .\scripts\run-app.ps1 -Test    - run the test suite instead
param([switch]$Test)

. "$PSScriptRoot\env.ps1"
Push-Location (Join-Path $PSScriptRoot '..\bookstore-api')
try {
    if ($Test) { .\mvnw.cmd -B clean test } else { .\mvnw.cmd -B spring-boot:run }
} finally {
    Pop-Location
}
