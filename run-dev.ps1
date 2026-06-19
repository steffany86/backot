$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaOpts = "-Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2 -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2"

$env:JAVA_TOOL_OPTIONS = $javaOpts
Write-Host "JAVA_TOOL_OPTIONS=$env:JAVA_TOOL_OPTIONS"

& "$root\\mvnw.cmd" -q spring-boot:run
