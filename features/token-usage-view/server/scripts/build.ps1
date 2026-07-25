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

Write-Host 'Build passed.'
