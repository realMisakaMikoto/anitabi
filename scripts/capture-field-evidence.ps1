[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("gnss", "oem", "transit")]
    [string]$Scenario,

    [ValidateRange(1, 1440)]
    [int]$DurationMinutes = 60,

    [ValidateRange(10, 600)]
    [int]$IntervalSeconds = 60,

    [string]$Serial,

    [string]$OutputDirectory,

    [string]$AdbPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$packageName = "cn.anitabi.navigator"
$releaseSha256 = "e3d36b47695b452978680726c5eb09133e04c0f207149a6324f3e08ac8f9a9ec"

function Resolve-AdbPath {
    param([string]$RequestedPath)

    if ($RequestedPath) {
        if (-not (Test-Path -LiteralPath $RequestedPath -PathType Leaf)) {
            throw "adb was not found at the requested path: $RequestedPath"
        }
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }

    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $wingetPath = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $wingetPath -PathType Leaf) {
        return (Resolve-Path -LiteralPath $wingetPath).Path
    }

    throw "adb was not found. Install Android SDK Platform-Tools or pass -AdbPath."
}

function Invoke-AdbText {
    param([string[]]$Arguments)

    $result = & $script:adbExecutable -s $script:deviceSerial @Arguments 2>&1
    return (($result | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine)
}

function Save-AdbCapture {
    param(
        [string]$Name,
        [string[]]$Arguments
    )

    Invoke-AdbText -Arguments $Arguments |
        Set-Content -LiteralPath (Join-Path $script:evidenceDirectory $Name) -Encoding UTF8
}

function Select-EvidenceLines {
    param(
        [string]$Text,
        [string[]]$Patterns
    )

    $matches = foreach ($line in ($Text -split "`r?`n")) {
        foreach ($pattern in $Patterns) {
            if ($line -match $pattern) {
                $line.Trim()
                break
            }
        }
    }
    return (($matches | Select-Object -Unique) -join " | ")
}

$script:adbExecutable = Resolve-AdbPath -RequestedPath $AdbPath
$deviceOutput = & $script:adbExecutable devices -l 2>&1
$connectedDevices = @(
    $deviceOutput |
        ForEach-Object { $_.ToString() } |
        Where-Object { $_ -match "^(\S+)\s+device(?:\s|$)" } |
        ForEach-Object { ($_ -split "\s+")[0] }
)

if ($Serial) {
    if ($Serial -notin $connectedDevices) {
        throw "The requested device is not connected and authorized: $Serial"
    }
    $script:deviceSerial = $Serial
} elseif ($connectedDevices.Count -eq 1) {
    $script:deviceSerial = $connectedDevices[0]
} elseif ($connectedDevices.Count -eq 0) {
    throw "No authorized Android device is connected. Unlock the phone and authorize USB debugging."
} else {
    throw "More than one Android device is connected. Pass -Serial explicitly."
}

if ($Scenario -eq "oem" -and $DurationMinutes -lt 120) {
    throw "The OEM scenario requires at least 120 minutes."
}

if (-not $OutputDirectory) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "anitabi-field-evidence\$stamp-$Scenario"
}
$script:evidenceDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $script:evidenceDirectory -Force | Out-Null

$state = (Invoke-AdbText -Arguments @("get-state")).Trim()
if ($state -ne "device") {
    throw "ADB device state is not ready: $state"
}

$packageDump = Invoke-AdbText -Arguments @("shell", "dumpsys", "package", $packageName)
if ($packageDump -notmatch "versionName=0\.2\.0") {
    throw "The connected phone does not have Anitabi Navigator v0.2.0 installed."
}

$packagePath = Invoke-AdbText -Arguments @("shell", "pm", "path", $packageName)
$baseApkPath = @(
    $packagePath -split "`r?`n" |
        Where-Object { $_ -match "^package:(.+/base\.apk)$" } |
        ForEach-Object { $_.Substring("package:".Length).Trim() }
) | Select-Object -First 1
if (-not $baseApkPath) {
    throw "The installed base APK path could not be resolved."
}
$installedHash = (Invoke-AdbText -Arguments @("shell", "sha256sum", $baseApkPath)).Trim().Split(" ")[0].ToLowerInvariant()
if ($installedHash -ne $releaseSha256) {
    throw "The installed APK is not the exact public v0.2.0 release asset."
}

$mockApps = Invoke-AdbText -Arguments @("shell", "cmd", "appops", "query-op", "android:mock_location", "allow")
$mockQueryFailed = $mockApps -match "(?i)error|unknown operation|not found"
$mockAllowed = $mockApps -match "(?im)^\s*(?!No operations)(?!Error:)(\S+)"
if ($Scenario -eq "gnss" -and $mockQueryFailed) {
    $mockApps | Set-Content -LiteralPath (Join-Path $script:evidenceDirectory "preflight-mock-location.txt") -Encoding UTF8
    throw "Mock-location access could not be verified on this device."
}
if ($Scenario -eq "gnss" -and $mockAllowed) {
    $mockApps | Set-Content -LiteralPath (Join-Path $script:evidenceDirectory "preflight-mock-location.txt") -Encoding UTF8
    throw "Mock-location access is still allowed. Disable the selected mock-location app before a real GNSS run."
}

$initialServices = Invoke-AdbText -Arguments @("shell", "dumpsys", "activity", "services", $packageName)
$initialNotifications = Invoke-AdbText -Arguments @("shell", "dumpsys", "notification", "--noredact")
if ($initialServices -notmatch "NavigationService") {
    throw "Start continuous navigation in the app before running this collector."
}
if ($initialNotifications -notmatch [regex]::Escape($packageName)) {
    throw "The navigation notification is not visible. Start continuous navigation and check notification permission."
}

$metadata = [ordered]@{
    scenario = $Scenario
    serial = $script:deviceSerial
    package = $packageName
    startedAt = (Get-Date).ToString("o")
    durationMinutes = $DurationMinutes
    intervalSeconds = $IntervalSeconds
    adb = $script:adbExecutable
    releaseSha256 = $releaseSha256
}
$metadata | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $script:evidenceDirectory "metadata.json") -Encoding UTF8
$packageDump | Set-Content -LiteralPath (Join-Path $script:evidenceDirectory "package-start.txt") -Encoding UTF8
$mockApps | Set-Content -LiteralPath (Join-Path $script:evidenceDirectory "mock-location-start.txt") -Encoding UTF8

$endAt = [DateTimeOffset]::Now.AddMinutes($DurationMinutes)
$rows = New-Object System.Collections.Generic.List[object]
$sampleNumber = 0

do {
    $sampleNumber++
    $sampledAt = [DateTimeOffset]::Now
    $pidText = (Invoke-AdbText -Arguments @("shell", "pidof", $packageName)).Trim()
    $services = Invoke-AdbText -Arguments @("shell", "dumpsys", "activity", "services", $packageName)
    $notifications = Invoke-AdbText -Arguments @("shell", "dumpsys", "notification", "--noredact")
    $location = Invoke-AdbText -Arguments @("shell", "dumpsys", "location")
    $deviceIdle = Invoke-AdbText -Arguments @("shell", "dumpsys", "deviceidle")
    $power = Invoke-AdbText -Arguments @("shell", "dumpsys", "power")
    $battery = Invoke-AdbText -Arguments @("shell", "dumpsys", "battery")
    $crashes = Invoke-AdbText -Arguments @("logcat", "-d", "-b", "crash", "-v", "threadtime")

    $row = [pscustomobject]@{
        sample = $sampleNumber
        timestamp = $sampledAt.ToString("o")
        pid = $pidText
        servicePresent = [bool]($services -match "NavigationService")
        notificationPresent = [bool](
            $notifications -match [regex]::Escape($packageName) -and
            $notifications -match "continuous_navigation|NotificationRecord\(0x.*pkg=$([regex]::Escape($packageName))"
        )
        appCrashPresent = [bool]($crashes -match [regex]::Escape($packageName))
        location = Select-EvidenceLines -Text $location -Patterns @(
            [regex]::Escape($packageName),
            "(?i)mock",
            "(?i)last location",
            "(?i)gps provider"
        )
        idle = Select-EvidenceLines -Text $deviceIdle -Patterns @(
            "mState=",
            "mLightState=",
            "mScreenOn=",
            "mCharging=",
            "mNetworkConnected="
        )
        power = Select-EvidenceLines -Text $power -Patterns @(
            "mWakefulness=",
            "Wakefulness:",
            "mIsPowered="
        )
        battery = Select-EvidenceLines -Text $battery -Patterns @("level:", "status:", "powered:")
    }
    $rows.Add($row)
    $row | Export-Csv -LiteralPath (Join-Path $script:evidenceDirectory "samples.csv") -NoTypeInformation -Append -Encoding UTF8

    Write-Host ("[{0}] sample={1} pid={2} service={3} notification={4} crash={5}" -f `
        $sampledAt.ToString("HH:mm:ss"),
        $sampleNumber,
        $pidText,
        $row.servicePresent,
        $row.notificationPresent,
        $row.appCrashPresent)

    if ([DateTimeOffset]::Now -lt $endAt) {
        $remainingSeconds = [Math]::Max(0, [int][Math]::Ceiling(($endAt - [DateTimeOffset]::Now).TotalSeconds))
        Start-Sleep -Seconds ([Math]::Min($IntervalSeconds, $remainingSeconds))
    }
} while ([DateTimeOffset]::Now -lt $endAt)

Save-AdbCapture -Name "services-end.txt" -Arguments @("shell", "dumpsys", "activity", "services", $packageName)
Save-AdbCapture -Name "notifications-end.txt" -Arguments @("shell", "dumpsys", "notification", "--noredact")
Save-AdbCapture -Name "location-end.txt" -Arguments @("shell", "dumpsys", "location")
Save-AdbCapture -Name "deviceidle-end.txt" -Arguments @("shell", "dumpsys", "deviceidle")
Save-AdbCapture -Name "power-end.txt" -Arguments @("shell", "dumpsys", "power")
Save-AdbCapture -Name "battery-end.txt" -Arguments @("shell", "dumpsys", "battery")
Save-AdbCapture -Name "batterystats-end.txt" -Arguments @("shell", "dumpsys", "batterystats", $packageName)
Save-AdbCapture -Name "connectivity-end.txt" -Arguments @("shell", "dumpsys", "connectivity")
Save-AdbCapture -Name "crash-buffer-end.txt" -Arguments @("logcat", "-d", "-b", "crash", "-v", "threadtime")

$summary = [ordered]@{
    scenario = $Scenario
    finishedAt = (Get-Date).ToString("o")
    samples = $rows.Count
    observedPids = @($rows | Select-Object -ExpandProperty pid -Unique | Where-Object { $_ })
    serviceMissingSamples = @($rows | Where-Object { -not $_.servicePresent }).Count
    notificationMissingSamples = @($rows | Where-Object { -not $_.notificationPresent }).Count
    appCrashSamples = @($rows | Where-Object { $_.appCrashPresent }).Count
    automatedResult = "EVIDENCE_CAPTURED_MANUAL_REVIEW_REQUIRED"
}
$summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $script:evidenceDirectory "summary.json") -Encoding UTF8

Write-Host "Evidence captured at: $script:evidenceDirectory"
Write-Host "Review the field checklist before recording a pass."
