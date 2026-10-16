param([switch]$RecordDependencyChecksums)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskVersionMatch = [regex]::Match((Get-Content -LiteralPath (Join-Path $taskRoot 'app\build.gradle') -Raw), "versionName\s+'([A-Za-z0-9._-]+)'")
if (!$taskVersionMatch.Success) { throw 'Cannot read app version' }
$taskOutputName = 'NotiGuard-' + $taskVersionMatch.Groups[1].Value + '.apk'
$taskRuntime = Join-Path $env:LOCALAPPDATA 'NotiGuardBuild'
# A fresh directory prevents a deleted source file from surviving into a later APK.
$taskStage = Join-Path $taskRuntime ('project-' + $taskVersionMatch.Groups[1].Value + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
$taskJdk = Get-ChildItem -LiteralPath $taskRuntime -Directory | Where-Object Name -Like 'jdk-*' | Select-Object -First 1
if ($null -eq $taskJdk) { throw 'Install a JDK 17 in %LOCALAPPDATA%\NotiGuardBuild or build in Android Studio with JDK 17.' }
$env:JAVA_HOME = $taskJdk.FullName
# ASCII staging path avoids Java launcher failures for combining Unicode in Windows paths.
New-Item -ItemType Directory -Force -Path $taskStage | Out-Null
& robocopy $taskRoot $taskStage /E /XD .git .tools .gradle build dist /XF '*.apk' /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Source staging failed: $LASTEXITCODE" }
Push-Location -LiteralPath $taskStage
try {
    if ($RecordDependencyChecksums) { & '.\gradlew.bat' --no-daemon --write-verification-metadata sha256 testDebugUnitTest lintDebug assembleRelease }
    else { & '.\gradlew.bat' --no-daemon testDebugUnitTest lintDebug assembleRelease }
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    if ($RecordDependencyChecksums) { Copy-Item -LiteralPath 'gradle\verification-metadata.xml' -Destination (Join-Path $taskRoot 'gradle\verification-metadata.xml') }
    $taskDist = Join-Path $taskRoot 'dist'; New-Item -ItemType Directory -Force -Path $taskDist | Out-Null
    [System.IO.File]::WriteAllText((Join-Path $taskDist 'latest-build-path.txt'), $taskStage, [System.Text.UTF8Encoding]::new($false))
    $taskSigned = Join-Path (Join-Path $taskStage 'app\build\outputs\apk\release') $taskOutputName
    & (Join-Path $taskRoot 'scripts\sign.ps1') -InputApk (Join-Path $taskStage 'app\build\outputs\apk\release\app-release-unsigned.apk') -OutputApk $taskSigned
    Copy-Item -LiteralPath $taskSigned -Destination (Join-Path $taskDist $taskOutputName)
    Copy-Item -LiteralPath 'app\build\reports\lint-results-debug.html' -Destination (Join-Path $taskDist 'lint-results.html')
    Get-FileHash -LiteralPath (Join-Path $taskDist $taskOutputName) -Algorithm SHA256
} finally { Pop-Location }
