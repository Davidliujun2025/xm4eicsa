param(
    [int]$BackendPort = 18080,
    [int]$FrontendPort = 15173,
    [int]$WorkbenchPort = 15174,
    [switch]$ReuseExisting
)

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$schemaPath = (Resolve-Path (Join-Path $projectRoot "application\src\test\resources\test-schema.sql")).Path.Replace("\", "/")

function Resolve-CommandPath {
    param([string[]]$Names)

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }
    return $null
}

function Resolve-MavenPath {
    if ($env:XM4_MAVEN_CMD -and (Test-Path -LiteralPath $env:XM4_MAVEN_CMD)) {
        return (Resolve-Path -LiteralPath $env:XM4_MAVEN_CMD).Path
    }

    $command = Resolve-CommandPath -Names @("mvn.cmd", "mvn")
    if ($command) {
        return $command
    }

    $wrapperRoot = Join-Path $env:USERPROFILE ".m2\wrapper\dists"
    if (Test-Path -LiteralPath $wrapperRoot) {
        $wrapper = Get-ChildItem -LiteralPath $wrapperRoot -Recurse -Filter "mvn.cmd" -File -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($wrapper) {
            return $wrapper.FullName
        }
    }

    throw "Maven was not found. Install Maven 3.9+ or set XM4_MAVEN_CMD to mvn.cmd."
}

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return (Resolve-Path -LiteralPath $env:JAVA_HOME).Path
    }

    $searchRoots = @(
        (Join-Path $env:USERPROFILE ".cache\carepilot-tools\jdk21"),
        (Join-Path $env:USERPROFILE ".jdks"),
        "C:\Program Files\Java"
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path -LiteralPath $root)) {
            continue
        }
        $java = Get-ChildItem -LiteralPath $root -Recurse -Filter "java.exe" -File -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -like "*\bin\java.exe" } |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($java) {
            return (Split-Path -Parent (Split-Path -Parent $java.FullName))
        }
    }

    $javaCommand = Resolve-CommandPath -Names @("java.exe", "java")
    if ($javaCommand) {
        return (Split-Path -Parent (Split-Path -Parent $javaCommand))
    }

    throw "JDK 21 was not found. Install JDK 21 or set JAVA_HOME."
}

function Install-FrontendDependencies {
    param([string]$AppDirectory)

    $viteCommand = Join-Path $AppDirectory "node_modules\.bin\vite.cmd"
    if (Test-Path -LiteralPath $viteCommand) {
        return
    }

    Write-Output "INSTALLING_DEPENDENCIES=$AppDirectory"
    Push-Location $AppDirectory
    try {
        & $npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            throw "npm ci failed in $AppDirectory"
        }
    } finally {
        Pop-Location
    }
}

function Test-PortInUse {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

$maven = Resolve-MavenPath
$npm = Resolve-CommandPath -Names @("npm.cmd", "npm")
if (-not $npm) {
    throw "npm was not found. Install Node.js 20+ and npm."
}

$env:JAVA_HOME = Resolve-JavaHome
$env:NPM_CONFIG_CACHE = Join-Path $env:LOCALAPPDATA "xm4eicsa-npm-cache"
$env:SERVER_PORT = [string]$BackendPort
$env:SPRING_PROFILES_ACTIVE = "dev"
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
    if (-not $ReuseExisting) {
        throw "Backend port $BackendPort is already in use. Stop the existing process, choose another port, or pass -ReuseExisting."
    }
    Write-Output "BACKEND_ALREADY_RUNNING=true"
} else {
    Write-Output "BUILDING_BACKEND_MODULES=true"
    & $maven -f (Join-Path $projectRoot "pom.xml") install -DskipTests --no-transfer-progress
    if ($LASTEXITCODE -ne 0) {
        throw "Backend module build failed."
    }

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
Install-FrontendDependencies -AppDirectory (Join-Path $projectRoot "features\login\ui")
if (Test-PortInUse -Port $FrontendPort) {
    if (-not $ReuseExisting) {
        throw "Login frontend port $FrontendPort is already in use. Stop the existing process, choose another port, or pass -ReuseExisting."
    }
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
Install-FrontendDependencies -AppDirectory (Join-Path $projectRoot "features\ai-chat-workbench\ui\client")
if (Test-PortInUse -Port $WorkbenchPort) {
    if (-not $ReuseExisting) {
        throw "Workbench port $WorkbenchPort is already in use. Stop the existing process, choose another port, or pass -ReuseExisting."
    }
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
