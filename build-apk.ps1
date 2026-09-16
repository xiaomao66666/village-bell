param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $javacCommand = Get-Command javac -ErrorAction Stop
    $JavaHome = Split-Path (Split-Path $javacCommand.Source -Parent) -Parent
}
$projectRoot = $PSScriptRoot
$toolRoot = Join-Path $projectRoot '.tools'
$androidJar = Join-Path $toolRoot 'platform\android-35\android.jar'
$buildTools = Join-Path $toolRoot 'build-tools\android-15'
$outputRoot = Join-Path $projectRoot 'build\native'
$distRoot = Join-Path $projectRoot 'dist'
if (!(Test-Path -LiteralPath $androidJar) -or !(Test-Path -LiteralPath "$buildTools\aapt2.exe")) {
    throw 'Missing SDK. Run setup-tools.ps1 first, or use Android Studio with SDK 35.'
}
if (!(Test-Path -LiteralPath "$JavaHome\bin\javac.exe")) { throw 'Pass -JavaHome with a JDK installation.' }
foreach ($dir in @($outputRoot, "$outputRoot\generated", "$outputRoot\classes", "$outputRoot\dex", $distRoot)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}
function Run-Checked([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE): $Exe" }
}
Run-Checked "$buildTools\aapt2.exe" @('compile','--dir',"$projectRoot\app\src\main\res",'-o',"$outputRoot\resources.zip")
$manifest = [xml](Get-Content -LiteralPath "$projectRoot\app\src\main\AndroidManifest.xml" -Raw)
$manifest.DocumentElement.SetAttribute('package','cn.villagebell')
$manifest.Save("$outputRoot\AndroidManifest.xml")
Run-Checked "$buildTools\aapt2.exe" @('link','-o',"$outputRoot\unsigned.apk",'-I',$androidJar,'--manifest',"$outputRoot\AndroidManifest.xml",'--java',"$outputRoot\generated",'--min-sdk-version','26','--target-sdk-version','35','--version-code','1','--version-name','0.1.0',"$outputRoot\resources.zip")
$sources = @(Get-ChildItem -LiteralPath "$projectRoot\app\src\main\java" -Recurse -Filter '*.java') + @(Get-ChildItem -LiteralPath "$outputRoot\generated" -Recurse -Filter '*.java')
$sourceLines = $sources | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
[IO.File]::WriteAllLines("$outputRoot\sources.txt", [string[]]$sourceLines, [Text.UTF8Encoding]::new($false))
Run-Checked "$JavaHome\bin\javac.exe" @('--release','8','-encoding','UTF-8','-classpath',$androidJar,'-d',"$outputRoot\classes","@$outputRoot\sources.txt")
Run-Checked "$JavaHome\bin\jar.exe" @('cf',"$outputRoot\classes.jar",'-C',"$outputRoot\classes",'.')
Run-Checked "$JavaHome\bin\java.exe" @('-cp',"$buildTools\lib\d8.jar",'com.android.tools.r8.D8','--min-api','26','--lib',$androidJar,'--output',"$outputRoot\dex","$outputRoot\classes.jar")
Run-Checked "$JavaHome\bin\jar.exe" @('uf',"$outputRoot\unsigned.apk",'-C',"$outputRoot\dex",'classes.dex')
Run-Checked "$buildTools\zipalign.exe" @('-f','4',"$outputRoot\unsigned.apk","$outputRoot\aligned.apk")
$keyFile = Join-Path $toolRoot 'development.keystore'
if (!(Test-Path -LiteralPath $keyFile)) {
    Run-Checked "$JavaHome\bin\keytool.exe" @('-genkeypair','-keystore',$keyFile,'-storepass','android','-keypass','android','-alias','villagebell','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Village Bell Development')
}
$apk = Join-Path $distRoot 'village-bell-0.1.0.apk'
Run-Checked "$JavaHome\bin\java.exe" @('-jar',"$buildTools\lib\apksigner.jar",'sign','--ks',$keyFile,'--ks-key-alias','villagebell','--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,"$outputRoot\aligned.apk")
Run-Checked "$JavaHome\bin\java.exe" @('-jar',"$buildTools\lib\apksigner.jar",'verify','--verbose',$apk)
(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash | Set-Content -LiteralPath "$apk.sha256" -Encoding ascii
Write-Output "APK: $apk"
