$ErrorActionPreference = 'Stop'
$portProject = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$mekVersion = '1.20.1-10.4.16.80'
$mekDirectory = Join-Path $portProject "build/verification/maven/mekanism/Mekanism/$mekVersion"
New-Item -ItemType Directory -Force -Path $mekDirectory | Out-Null
$mekJar = Join-Path $mekDirectory "Mekanism-$mekVersion.jar"
$expectedHash = '3B0D191FA4A45E662F9725991D76A30192F30186ABB2BA694B873B5F7B9358D2'
if (!(Test-Path -LiteralPath $mekJar)) {
    Invoke-WebRequest "https://modmaven.dev/mekanism/Mekanism/$mekVersion/Mekanism-$mekVersion.jar" -OutFile $mekJar
}
if ((Get-FileHash -LiteralPath $mekJar -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Mekanism test dependency has an unexpected checksum.'
}
$mekPom = Join-Path $mekDirectory "Mekanism-$mekVersion.pom"
"<project><modelVersion>4.0.0</modelVersion><groupId>mekanism</groupId><artifactId>Mekanism</artifactId><version>$mekVersion</version></project>" | Set-Content -LiteralPath $mekPom -Encoding UTF8
Write-Output "Ready: Mekanism $mekVersion for Forge test runs."
