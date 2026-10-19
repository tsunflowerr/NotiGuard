$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskVersionMatch = [regex]::Match((Get-Content -LiteralPath (Join-Path $taskRoot 'keeper\build.gradle') -Raw), "versionName\s+'([A-Za-z0-9._-]+)'")
if (!$taskVersionMatch.Success) { throw 'Cannot read keeper version' }
$taskOutputName = 'NotiGuard-Keeper-' + $taskVersionMatch.Groups[1].Value + '.apk'
$taskRuntime = Join-Path $env:LOCALAPPDATA 'NotiGuardBuild'
# A fresh directory prevents a deleted source file from surviving into a later APK.
$taskStage = Join-Path $taskRuntime ('keeper-' + $taskVersionMatch.Groups[1].Value + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
$taskJdk = Get-ChildItem -LiteralPath $taskRuntime -Directory | Where-Object Name -Like 'jdk-*' | Select-Object -First 1
if ($null -eq $taskJdk) { throw 'Install a JDK 17 in %LOCALAPPDATA%\NotiGuardBuild or build in Android Studio with JDK 17.' }
$env:JAVA_HOME = $taskJdk.FullName
if (!$env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
New-Item -ItemType Directory -Force -Path $taskStage | Out-Null
& robocopy $taskRoot $taskStage /E /XD .git .tools .gradle build dist /XF '*.apk' /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Source staging failed: $LASTEXITCODE" }
Push-Location -LiteralPath $taskStage
try {
    & '.\gradlew.bat' --no-daemon :keeper:testDebugUnitTest :keeper:lintDebug :keeper:assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    $taskDist = Join-Path $taskRoot 'dist'; New-Item -ItemType Directory -Force -Path $taskDist | Out-Null
    $taskSigned = Join-Path (Join-Path $taskStage 'keeper\build\outputs\apk\release') $taskOutputName
    & (Join-Path $taskRoot 'scripts\sign.ps1') -InputApk (Join-Path $taskStage 'keeper\build\outputs\apk\release\keeper-release-unsigned.apk') -OutputApk $taskSigned -KeyName keeper
    Copy-Item -LiteralPath $taskSigned -Destination (Join-Path $taskDist $taskOutputName)
    Copy-Item -LiteralPath 'keeper\build\reports\lint-results-debug.html' -Destination (Join-Path $taskDist 'keeper-lint-results.html')
    Get-FileHash -LiteralPath (Join-Path $taskDist $taskOutputName) -Algorithm SHA256
} finally { Pop-Location }
