param(
    [switch]$InstallMissing,
    [switch]$AcceptAndroidLicenses,
    [switch]$PersistUserEnv,
    [string]$SdkRoot = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }),
    [string]$CommandLineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LocalPropertiesPath = Join-Path $RepoRoot "local.properties"
$RequiredPackages = @(
    "platforms;android-36",
    "build-tools;36.0.0",
    "platform-tools"
)

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Test-Command {
    param([string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-NativeLogged {
    param(
        [string]$FilePath,
        [string[]]$ArgumentList = @(),
        [string]$InputText = $null
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        if ($null -ne $InputText) {
            $InputText | & $FilePath @ArgumentList 2>&1 | ForEach-Object { Write-Host $_ }
        } else {
            & $FilePath @ArgumentList 2>&1 | ForEach-Object { Write-Host $_ }
        }

        if ($LASTEXITCODE -ne 0) {
            throw "$FilePath failed with exit code $LASTEXITCODE."
        }
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
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

function Ensure-Jdk {
    Write-Step "Checking JDK 17"
    $javaHome = Find-JavaHome
    if ($javaHome) {
        $env:JAVA_HOME = $javaHome
        $env:PATH = "$(Join-Path $javaHome "bin");$env:PATH"
        Write-Host "JAVA_HOME=$javaHome"
        Invoke-NativeLogged -FilePath (Join-Path $javaHome "bin\java.exe") -ArgumentList @("-version")
        return $javaHome
    }

    if (-not $InstallMissing) {
        throw "JDK 17 was not found. Install it manually, or run this script with -InstallMissing."
    }

    if (-not (Test-Command "winget")) {
        throw "JDK 17 is missing and winget is unavailable. Install Eclipse Temurin JDK 17, then rerun this script."
    }

    Write-Host "Installing Eclipse Temurin JDK 17 with winget..."
    Invoke-NativeLogged -FilePath "winget" -ArgumentList @(
        "install",
        "--id",
        "EclipseAdoptium.Temurin.17.JDK",
        "--exact",
        "--accept-package-agreements",
        "--accept-source-agreements",
        "--disable-interactivity"
    )

    $javaHome = Find-JavaHome
    if (-not $javaHome) {
        throw "JDK install finished, but JAVA_HOME could not be resolved. Open a new PowerShell window or set JAVA_HOME manually."
    }

    $env:JAVA_HOME = $javaHome
    $env:PATH = "$(Join-Path $javaHome "bin");$env:PATH"
    Write-Host "JAVA_HOME=$javaHome"
    return $javaHome
}

function Find-SdkManager {
    param([string]$Root)

    $paths = @(
        (Join-Path $Root "cmdline-tools\latest\bin\sdkmanager.bat"),
        (Join-Path $Root "cmdline-tools\bin\sdkmanager.bat"),
        (Join-Path $Root "tools\bin\sdkmanager.bat")
    )

    foreach ($path in $paths) {
        if (Test-Path $path) {
            return (Resolve-Path $path).Path
        }
    }

    return $null
}

function Install-CommandLineTools {
    param([string]$Root)

    Write-Step "Installing Android command-line tools"
    New-Item -ItemType Directory -Force -Path $Root | Out-Null

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) "phonepad-android-cmdline-tools"
    $zipPath = Join-Path $tempRoot "commandlinetools-win.zip"
    $extractRoot = Join-Path $tempRoot "extract"

    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $tempRoot, $extractRoot | Out-Null

    Write-Host "Downloading $CommandLineToolsUrl"
    $null = Invoke-WebRequest -Uri $CommandLineToolsUrl -OutFile $zipPath
    $null = Expand-Archive -LiteralPath $zipPath -DestinationPath $extractRoot -Force

    $source = Join-Path $extractRoot "cmdline-tools"
    $target = Join-Path $Root "cmdline-tools\latest"
    if (Test-Path $target) {
        throw "Android command-line tools target already exists: $target"
    }

    New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
    Move-Item -LiteralPath $source -Destination $target
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}

function Ensure-AndroidSdk {
    Write-Step "Checking Android SDK"
    $resolvedSdkRoot = $SdkRoot
    New-Item -ItemType Directory -Force -Path $resolvedSdkRoot | Out-Null
    $resolvedSdkRoot = (Resolve-Path $resolvedSdkRoot).Path

    $sdkManager = Find-SdkManager -Root $resolvedSdkRoot
    if (-not $sdkManager) {
        if (-not $InstallMissing) {
            throw "Android sdkmanager was not found under $resolvedSdkRoot. Install Android Studio/SDK, or run this script with -InstallMissing."
        }

        Install-CommandLineTools -Root $resolvedSdkRoot
        $sdkManager = Find-SdkManager -Root $resolvedSdkRoot
        if (-not $sdkManager) {
            throw "Android command-line tools were installed, but sdkmanager.bat was not found."
        }
    }

    $env:ANDROID_HOME = $resolvedSdkRoot
    $env:ANDROID_SDK_ROOT = $resolvedSdkRoot
    $env:PATH = "$(Join-Path $resolvedSdkRoot "platform-tools");$env:PATH"

    Write-Host "ANDROID_HOME=$resolvedSdkRoot"
    Write-Host "sdkmanager=$sdkManager"

    if ($InstallMissing) {
        Write-Step "Installing Android SDK packages"
        if ($AcceptAndroidLicenses) {
            Invoke-NativeLogged -FilePath $sdkManager -ArgumentList @("--licenses") -InputText "y`ny`ny`ny`ny`ny`ny`ny`ny`ny`n"
        }
        Invoke-NativeLogged -FilePath $sdkManager -ArgumentList $RequiredPackages
    }

    return $resolvedSdkRoot
}

function Write-LocalProperties {
    param([string]$Root)

    Write-Step "Writing local.properties"
    $gradleSdkPath = $Root.Replace("\", "\\")
    if ($gradleSdkPath -match "^[A-Za-z]:") {
        $gradleSdkPath = $gradleSdkPath.Insert(1, "\")
    }
    $sdkLine = "sdk.dir=$gradleSdkPath"

    if (Test-Path $LocalPropertiesPath) {
        $lines = Get-Content -LiteralPath $LocalPropertiesPath
        $updated = $false
        $nextLines = foreach ($line in $lines) {
            if ($line -match "^sdk\.dir=") {
                $updated = $true
                $sdkLine
            } else {
                $line
            }
        }

        if (-not $updated) {
            $nextLines += $sdkLine
        }

        Set-Content -LiteralPath $LocalPropertiesPath -Value $nextLines -Encoding ASCII
    } else {
        Set-Content -LiteralPath $LocalPropertiesPath -Value @(
            "# Local Android SDK path. This file is intentionally ignored by git.",
            $sdkLine
        ) -Encoding ASCII
    }

    Write-Host $sdkLine
}

function Set-PersistentEnvironment {
    param(
        [string]$JavaHome,
        [string]$AndroidHome
    )

    if (-not $PersistUserEnv) {
        return
    }

    Write-Step "Persisting user environment variables"
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $JavaHome, "User")
    [Environment]::SetEnvironmentVariable("ANDROID_HOME", $AndroidHome, "User")
    [Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $AndroidHome, "User")
    Write-Host "Open a new PowerShell window to pick up persisted environment variables."
}

$javaHome = Ensure-Jdk
$androidHome = Ensure-AndroidSdk
Write-LocalProperties -Root $androidHome
Set-PersistentEnvironment -JavaHome $javaHome -AndroidHome $androidHome

Write-Step "Verifying Gradle wrapper"
& (Join-Path $RepoRoot "gradlew.bat") --version

Write-Host ""
Write-Host "Windows development setup is ready."
Write-Host "Next: .\scripts\build-windows.ps1 -Target all"
