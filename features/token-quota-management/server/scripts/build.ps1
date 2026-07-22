$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Out = Join-Path $Root 'build/classes'
if (Test-Path -LiteralPath $Out) {
    Remove-Item -LiteralPath $Out -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $Out | Out-Null
$Sources = Get-ChildItem -Path (Join-Path $Root 'backend/src/main/java') -Recurse -Filter '*.java'
javac --add-modules jdk.httpserver -Xlint:all -Werror -d $Out $Sources.FullName
if ($LASTEXITCODE -ne 0) { throw "Java main compilation failed with exit code $LASTEXITCODE" }

$CppOut = Join-Path $Root 'build/cpp'
if (Test-Path -LiteralPath $CppOut) {
    Remove-Item -LiteralPath $CppOut -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $CppOut | Out-Null
g++ -std=c++20 -Wall -Wextra -Wpedantic -Werror `
    -I (Join-Path $Root 'cpp-client/include') `
    -c (Join-Path $Root 'cpp-client/src/token_monitor_client.cpp') `
    -o (Join-Path $CppOut 'token_monitor_client.o')
if ($LASTEXITCODE -ne 0) { throw "C++ library compilation failed with exit code $LASTEXITCODE" }

g++ -std=c++20 -Wall -Wextra -Wpedantic -Werror `
    -I (Join-Path $Root 'cpp-client/include') `
    (Join-Path $Root 'cpp-client/src/token_monitor_client.cpp') `
    (Join-Path $Root 'cpp-client/tests/token_monitor_client_test.cpp') `
    -o (Join-Path $CppOut 'token_monitor_client_test.exe')
if ($LASTEXITCODE -ne 0) { throw "C++ test compilation failed with exit code $LASTEXITCODE" }

Write-Host 'Build passed.'
