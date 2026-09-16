param([string]$JavaHome = $env:JAVA_HOME, [string]$SamplePath)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $javacCommand = Get-Command javac -ErrorAction Stop
    $JavaHome = Split-Path (Split-Path $javacCommand.Source -Parent) -Parent
}
$root = $PSScriptRoot
New-Item -ItemType Directory -Force -Path "$root\.tools" | Out-Null
$jar = Join-Path $root '.tools\json-20250517.jar'
if (!(Test-Path -LiteralPath $jar)) {
    Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile $jar
}
if ((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne '3EA61B2A06E31EDF1C91134FE9106B0EBB16628BE169F3DB75BC7A2B06B45796') {
    throw 'JSON test dependency checksum mismatch'
}
$out = Join-Path $root 'build\tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
& "$JavaHome\bin\javac.exe" --release 8 -encoding UTF-8 -cp $jar -d $out "$root\app\src\main\java\cn\villagebell\Village.java" "$root\app\src\main\java\cn\villagebell\Dashboard.java" "$root\tests\ParserTest.java" "$root\tests\DashboardTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
$arguments = @('-cp',"$jar;$out",'ParserTest',"$root\tests\fixtures\village-demo.json")
if ($SamplePath) { $arguments += (Resolve-Path -LiteralPath $SamplePath).Path }
& "$JavaHome\bin\java.exe" @arguments
if ($LASTEXITCODE -ne 0) { throw 'Parser tests failed' }
& "$JavaHome\bin\java.exe" -cp "$jar;$out" DashboardTest
if ($LASTEXITCODE -ne 0) { throw 'Dashboard tests failed' }
