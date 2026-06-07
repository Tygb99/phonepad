param(
    [ValidateSet("test", "debug", "lint", "all")]
    [string]$Target = "all",
    [switch]$NoDaemon,
    [string[]]$GradleArgs = @()
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$GradleWrapper = Join-Path $RepoRoot "gradlew.bat"
$LocalPropertiesPath = Join-Path $RepoRoot "local.properties"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return (Resolve-Path $env:JAVA_HOME).Path
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand -and $javaCommand.Source) {
        $javaBin = Split-Path $javaCommand.Source -Parent
        $javaHome = Split-Path $javaBin -Parent
        if (Test-Path (Join-Path $javaHome "bin\java.exe")) {
            return (Resolve-Path $javaHome).Path
        }
    }

    $candidateRoots = @(
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Microsoft\jdk-*"
    )

    foreach ($root in $candidateRoots) {
        $candidate = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } |
            Sort-Object FullName -Descending |
            Select-Object -First 1

        if ($candidate) {
            return $candidate.FullName
        }
    }

    return $null
}

function Get-SdkRoot {
    if ($env:ANDROID_HOME) {
        return $env:ANDROID_HOME
    }
    if ($env:ANDROID_SDK_ROOT) {
        return $env:ANDROID_SDK_ROOT
    }
    if (Test-Path $LocalPropertiesPath) {
        $sdkLine = Get-Content -LiteralPath $LocalPropertiesPath |
            Where-Object { $_ -match "^sdk\.dir=" } |
            Select-Object -First 1
        if ($sdkLine) {
            $sdkPath = $sdkLine -replace "^sdk\.dir=", ""
            $sdkPath = $sdkPath.Replace("\:", ":")
            $sdkPath = $sdkPath.Replace("\\", "\")
            return $sdkPath.Replace("/", "\")
        }
    }
    return $null
}

function Assert-Environment {
    Write-Step "Checking Windows development environment"

    if (-not (Test-Path $GradleWrapper)) {
        throw "gradlew.bat was not found at $GradleWrapper."
    }

    $javaHome = Find-JavaHome
    if (-not $javaHome) {
        throw "Java was not found. Run .\scripts\setup-windows-dev.ps1 first."
    }

    $sdkRoot = Get-SdkRoot
    if (-not $sdkRoot -or -not (Test-Path $sdkRoot)) {
        throw "Android SDK was not found. Run .\scripts\setup-windows-dev.ps1 first."
    }

    $env:JAVA_HOME = $javaHome
    $env:PATH = "$(Join-Path $javaHome "bin");$env:PATH"
    $env:ANDROID_HOME = $sdkRoot
    $env:ANDROID_SDK_ROOT = $sdkRoot
    $env:PATH = "$(Join-Path $sdkRoot "platform-tools");$env:PATH"

    Write-Host "ANDROID_HOME=$sdkRoot"
    Write-Host "JAVA_HOME=$javaHome"

    return $sdkRoot
}

function Get-GradleTasks {
    switch ($Target) {
        "test" { return @(":app:testDebugUnitTest") }
        "debug" { return @(":app:assembleDebug") }
        "lint" { return @(":app:lintDebug") }
        "all" { return @(":app:testDebugUnitTest", ":app:assembleDebug", ":app:lintDebug") }
    }
}

function Assert-NoInternetPermissionInSource {
    Write-Step "Checking source manifests for INTERNET permission"
    $matches = Get-ChildItem -Path (Join-Path $RepoRoot "app\src") -Recurse -Filter "*.xml" |
        Select-String -Pattern "android.permission.INTERNET"

    if ($matches) {
        $matches | ForEach-Object { Write-Host ("{0}:{1}: {2}" -f $_.Path, $_.LineNumber, $_.Line) }
        throw "INTERNET permission found in source XML."
    }

    Write-Host "No INTERNET permission found in source XML."
}

function Assert-NoInternetPermissionInApk {
    param([string]$SdkRoot)

    if ($Target -ne "debug" -and $Target -ne "all") {
        return
    }

    $apkPath = Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        throw "Expected debug APK was not found: $apkPath"
    }

    $aapt = Get-ChildItem -Path (Join-Path $SdkRoot "build-tools") -Recurse -Filter "aapt.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1

    if (-not $aapt) {
        Write-Warning "aapt.exe was not found, so built APK permission inspection was skipped."
        return
    }

    Write-Step "Checking built APK permissions"
    $permissions = & $aapt.FullName dump permissions $apkPath
    if ($permissions -match "android.permission.INTERNET") {
        throw "INTERNET permission found in built APK."
    }

    Write-Host "No INTERNET permission found in built APK."
}

$sdkRoot = Assert-Environment
Assert-NoInternetPermissionInSource

$tasks = Get-GradleTasks
$args = @()
if ($NoDaemon) {
    $args += "--no-daemon"
}

$hideLocalPropertiesForLint = ($Target -eq "lint" -or $Target -eq "all") -and (Test-Path $LocalPropertiesPath)
if ($hideLocalPropertiesForLint -and $GradleArgs -notcontains "--no-configuration-cache") {
    # Android Lint inspects local.properties on Windows and can flag the ignored
    # SDK path after Java properties unescaping. ANDROID_HOME is set above, so
    # lint can run without the local file.
    $args += "--no-configuration-cache"
}

$args += $tasks
$args += $GradleArgs

Write-Step "Running Gradle: $($args -join ' ')"
$localPropertiesBackupPath = $null
if ($hideLocalPropertiesForLint) {
    $localPropertiesBackupPath = "$LocalPropertiesPath.phonepad-lint-backup"
    if (Test-Path $localPropertiesBackupPath) {
        throw "Refusing to overwrite existing backup file: $localPropertiesBackupPath"
    }
    Move-Item -LiteralPath $LocalPropertiesPath -Destination $localPropertiesBackupPath
}

$gradleExitCode = 1
Push-Location $RepoRoot
try {
    & $GradleWrapper @args
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
    if ($localPropertiesBackupPath -and (Test-Path $localPropertiesBackupPath)) {
        Move-Item -LiteralPath $localPropertiesBackupPath -Destination $LocalPropertiesPath -Force
    }
}

if ($gradleExitCode -ne 0) {
    throw "Gradle failed with exit code $gradleExitCode."
}

Assert-NoInternetPermissionInApk -SdkRoot $sdkRoot

Write-Host ""
Write-Host "Windows build target '$Target' completed."
