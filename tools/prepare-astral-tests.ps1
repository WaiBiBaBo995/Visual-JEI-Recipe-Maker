$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$dependencyDirectory = Join-Path $taskRoot 'build/verification/deps/astral'
New-Item -ItemType Directory -Path $dependencyDirectory -Force | Out-Null
$dependencies = @(
    @('AstralSorcery-2.0.2.39.jar', 'https://www.cursemaven.com/curse/maven/astral-sorcery-241721/9148506/astral-sorcery-241721-9148506.jar'),
    @('ObserverLib-1.10.3.31.jar', 'https://maven.hellfiredev.net/hellfirepvp/observerlib/ObserverLib/1.10.3.31/ObserverLib-1.10.3.31.jar'),
    @('curios-neoforge-9.5.1+1.21.1.jar', 'https://maven.theillusivec4.top/top/theillusivec4/curios/curios-neoforge/9.5.1+1.21.1/curios-neoforge-9.5.1+1.21.1.jar')
)
foreach ($dependency in $dependencies) {
    $target = Join-Path $dependencyDirectory $dependency[0]
    if (-not (Test-Path -LiteralPath $target)) {
        Invoke-WebRequest -Uri $dependency[1] -OutFile $target
    }
    $archive = [System.IO.Compression.ZipFile]::OpenRead($target)
    try {
        if ($archive.Entries.Count -lt 2) { throw "Invalid dependency: $target" }
    } finally { $archive.Dispose() }
    Get-FileHash -LiteralPath $target -Algorithm SHA256 | Select-Object Hash, Path
}
