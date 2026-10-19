param([Parameter(Mandatory)][string]$InputApk, [Parameter(Mandatory)][string]$OutputApk, [ValidatePattern('^[a-z][a-z0-9-]*$')][string]$KeyName = 'notiguard')
$ErrorActionPreference = 'Stop'
$taskKeys = Join-Path $env:LOCALAPPDATA 'NotiGuardBuild\keys'
New-Item -ItemType Directory -Force -Path $taskKeys | Out-Null
$taskKeystore = Join-Path $taskKeys ($KeyName + '.jks')
# Each app keeps its own key; the original NotiGuard key keeps its historical password file name.
$taskPasswordFile = Join-Path $taskKeys $(if ($KeyName -eq 'notiguard') { 'password.dpapi' } else { $KeyName + '-password.dpapi' })
$taskSubject = $(if ($KeyName -eq 'notiguard') { 'CN=NotiGuard local build' } else { 'CN=NotiGuard ' + $KeyName + ' local build' })
Add-Type -AssemblyName System.Security
if (!(Test-Path -LiteralPath $taskPasswordFile)) {
    if (Test-Path -LiteralPath $taskKeystore) { throw 'Keystore exists without its password; do not replace it.' }
    $taskRandom = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    $taskProtected = [System.Security.Cryptography.ProtectedData]::Protect([System.Text.Encoding]::UTF8.GetBytes($taskRandom), $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
    [System.IO.File]::WriteAllBytes($taskPasswordFile, $taskProtected)
}
$taskPlain = [System.Security.Cryptography.ProtectedData]::Unprotect([System.IO.File]::ReadAllBytes($taskPasswordFile), $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
$env:NOTIGUARD_KEY_PASSWORD = [System.Text.Encoding]::UTF8.GetString($taskPlain)
try {
    if (!(Test-Path -LiteralPath $taskKeystore)) {
        & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -genkeypair -keystore $taskKeystore -alias $KeyName -storepass:env NOTIGUARD_KEY_PASSWORD -keypass:env NOTIGUARD_KEY_PASSWORD -dname $taskSubject -validity 10000 -keyalg RSA -keysize 3072
        if ($LASTEXITCODE -ne 0) { throw 'Failed to create local signing key' }
    }
    $taskSigner = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools\36.0.0\apksigner.bat'
    & $taskSigner sign --ks $taskKeystore --ks-key-alias $KeyName --ks-pass env:NOTIGUARD_KEY_PASSWORD --key-pass env:NOTIGUARD_KEY_PASSWORD --out $OutputApk $InputApk
    if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
    & $taskSigner verify --verbose --print-certs $OutputApk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
} finally { Remove-Item Env:NOTIGUARD_KEY_PASSWORD -ErrorAction SilentlyContinue; [Array]::Clear($taskPlain, 0, $taskPlain.Length) }
