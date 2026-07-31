param(
    [int]$BackendPort = 18080,
    [int]$FrontendPort = 15173,
    [int]$WorkbenchPort = 15174
)

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$maven = "C:\Users\86138\AppData\Local\Temp\codex-java-toolchain\maven2\apache-maven-3.9.9\bin\mvn.cmd"
$npm = "C:\Program Files\nodejs\npm.cmd"
$schemaPath = (Resolve-Path (Join-Path $projectRoot "application\src\test\resources\test-schema.sql")).Path.Replace("\", "/")

function Test-PortInUse {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

$env:JAVA_HOME = "C:\Users\86138\AppData\Local\Temp\codex-java-toolchain\jdk2\jdk-21.0.12+8"
$env:SERVER_PORT = [string]$BackendPort
$env:SPRING_DATASOURCE_URL = "jdbc:h2:mem:xm4web;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = "org.h2.Driver"
$env:SPRING_DATASOURCE_USERNAME = "sa"
$env:SPRING_DATASOURCE_PASSWORD = ""
$env:SPRING_FLYWAY_ENABLED = "false"
$env:SPRING_SQL_INIT_MODE = "always"
$env:SPRING_SQL_INIT_SCHEMA_LOCATIONS = "file:$schemaPath"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "create-drop"
$env:SPRING_DATA_REDIS_REPOSITORIES_ENABLED = "false"
$env:CORS_ALLOWED_ORIGINS = "http://127.0.0.1:$FrontendPort,http://127.0.0.1:$WorkbenchPort"

if (Test-PortInUse -Port $BackendPort) {
    Write-Output "BACKEND_ALREADY_RUNNING=true"
} else {
    $backendOut = Join-Path $projectRoot "application\target\local-integration.out.log"
    $backendErr = Join-Path $projectRoot "application\target\local-integration.err.log"
    $backend = Start-Process `
        -FilePath $maven `
        -ArgumentList @("-f", "application/pom.xml", "spring-boot:run", "-Dspring-boot.run.useTestClasspath=true") `
        -WorkingDirectory $projectRoot `
        -RedirectStandardOutput $backendOut `
        -RedirectStandardError $backendErr `
        -WindowStyle Hidden `
        -PassThru
    Write-Output "BACKEND_PID=$($backend.Id)"
}

$env:VITE_API_BASE_URL = "http://127.0.0.1:$BackendPort/api/v1"
$env:VITE_WORKBENCH_URL = "http://127.0.0.1:$WorkbenchPort/"
if (Test-PortInUse -Port $FrontendPort) {
    Write-Output "FRONTEND_ALREADY_RUNNING=true"
} else {
    $frontendOut = Join-Path $projectRoot "features\login\ui\local-integration.out.log"
    $frontendErr = Join-Path $projectRoot "features\login\ui\local-integration.err.log"
    $frontend = Start-Process `
        -FilePath $npm `
        -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1", "--port", [string]$FrontendPort) `
        -WorkingDirectory (Join-Path $projectRoot "features\login\ui") `
        -RedirectStandardOutput $frontendOut `
        -RedirectStandardError $frontendErr `
        -WindowStyle Hidden `
        -PassThru
    Write-Output "FRONTEND_PID=$($frontend.Id)"
}

$env:VITE_API_PROXY_TARGET = "http://127.0.0.1:$BackendPort"
if (Test-PortInUse -Port $WorkbenchPort) {
    Write-Output "WORKBENCH_ALREADY_RUNNING=true"
} else {
    $workbenchOut = Join-Path $projectRoot "features\ai-chat-workbench\ui\client\local-integration.out.log"
    $workbenchErr = Join-Path $projectRoot "features\ai-chat-workbench\ui\client\local-integration.err.log"
    $workbench = Start-Process `
        -FilePath $npm `
        -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1", "--port", [string]$WorkbenchPort) `
        -WorkingDirectory (Join-Path $projectRoot "features\ai-chat-workbench\ui\client") `
        -RedirectStandardOutput $workbenchOut `
        -RedirectStandardError $workbenchErr `
        -WindowStyle Hidden `
        -PassThru
    Write-Output "WORKBENCH_PID=$($workbench.Id)"
}

Write-Output "BACKEND_URL=http://127.0.0.1:$BackendPort"
Write-Output "FRONTEND_URL=http://127.0.0.1:$FrontendPort"
Write-Output "WORKBENCH_URL=http://127.0.0.1:$WorkbenchPort"
