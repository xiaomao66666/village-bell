$ErrorActionPreference = 'Stop'
$toolRoot = Join-Path $PSScriptRoot '.tools'
New-Item -ItemType Directory -Force -Path $toolRoot | Out-Null
# Official Google SDK packages, pinned to the checksums in repository2-3.xml.
$packages = @(
    @{Name='platform'; File='platform-35_r02.zip'; Hash='0bb560a90a7a2cbd0dd8348224d518b638fe7949'},
    @{Name='build-tools'; File='build-tools_r35_windows.zip'; Hash='af059bb67cf7786f45ee0db85e2d24985df1b4b6'},
    @{Name='platform-tools'; File='platform-tools_r37.0.1-win.zip'; Hash='e03e78b1d80b396f1c3358e31251cb31740e1110'}
)
foreach ($package in $packages) {
    $zip = Join-Path $toolRoot $package.File
    if (!(Test-Path -LiteralPath $zip)) { Invoke-WebRequest ('https://dl.google.com/android/repository/' + $package.File) -OutFile $zip }
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA1).Hash -ne $package.Hash) { throw "SDK checksum mismatch: $zip" }
    Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path $toolRoot $package.Name) -Force
    Write-Output ($package.Name + ' ready')
}
