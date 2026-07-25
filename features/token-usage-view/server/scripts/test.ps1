$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')

$Out = Join-Path $Root 'build/classes'
$Tests = Get-ChildItem -Path (Join-Path $Root 'backend/src/test/java') -Recurse -Filter '*.java'
javac --add-modules jdk.httpserver -Xlint:all -Werror -cp $Out -d $Out $Tests.FullName
if ($LASTEXITCODE -ne 0) { throw "Java test compilation failed with exit code $LASTEXITCODE" }
java --add-modules jdk.httpserver -ea -cp $Out tokenmonitor.TokenMonitorTests
if ($LASTEXITCODE -ne 0) { throw "Java tests failed with exit code $LASTEXITCODE" }
Write-Host 'All tests passed.'
