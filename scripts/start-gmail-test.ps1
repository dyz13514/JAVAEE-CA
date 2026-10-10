# Credentials are prompted locally and are never written to a configuration file.
$ErrorActionPreference = 'Stop'
$senderAddress = Read-Host 'Gmail sender address (example@gmail.com)'
if ($senderAddress -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') {
    throw 'Please enter a valid sender email address.'
}
$appPassword = Read-Host 'Google app password (not your Google login password)' -AsSecureString
$mailEnvironment = @{
    SPRING_PROFILES_ACTIVE = 'mail'
    CATS_MAIL_ENABLED = 'true'
    SMTP_HOST = 'smtp.gmail.com'
    SMTP_PORT = '587'
    SMTP_USERNAME = $senderAddress
    SMTP_AUTH = 'true'
    SMTP_STARTTLS = 'true'
    CATS_MAIL_FROM = $senderAddress
    CATS_BASE_URL = 'http://localhost:8080'
}
if ($env:SPRING_PROFILES_ACTIVE) {
    $mailEnvironment.SPRING_PROFILES_ACTIVE = ((($env:SPRING_PROFILES_ACTIVE -split ',') + 'mail') | Select-Object -Unique) -join ','
}
$previousEnvironment = @{}
$passwordPointer = [IntPtr]::Zero
try {
    $previousEnvironment.SMTP_PASSWORD = [Environment]::GetEnvironmentVariable('SMTP_PASSWORD', 'Process')
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($appPassword)
    $env:SMTP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer).Replace(' ', '')
    if (-not $env:SMTP_PASSWORD) { throw 'An app password is required.' }
    foreach ($setting in $mailEnvironment.GetEnumerator()) {
        $previousEnvironment[$setting.Key] = [Environment]::GetEnvironmentVariable($setting.Key, 'Process')
        [Environment]::SetEnvironmentVariable($setting.Key, $setting.Value, 'Process')
    }
    Push-Location (Split-Path $PSScriptRoot -Parent)
    try {
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'Application startup failed; inspect the Maven output.' }
    } finally { Pop-Location }
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    foreach ($setting in $previousEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($setting.Key, $setting.Value, 'Process')
    }
    $appPassword.Dispose()
}
