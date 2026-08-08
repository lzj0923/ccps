[CmdletBinding(SupportsShouldProcess = $true)]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-DirectorySizeBytes {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) { return [int64]0 }
    $sum = Get-ChildItem -LiteralPath $Path -Recurse -File -Force -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum
    if ($null -eq $sum.Sum) { return [int64]0 }
    return [int64]$sum.Sum
}

function Assert-TargetPath {
    param(
        [Parameter(Mandatory = $true)][string]$Target,
        [Parameter(Mandatory = $true)][string]$AllowedRoot
    )
    $targetFull = [System.IO.Path]::GetFullPath($Target).TrimEnd('\')
    $rootFull = [System.IO.Path]::GetFullPath($AllowedRoot).TrimEnd('\')
    if (-not $targetFull.StartsWith($rootFull + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Unsafe target path: $targetFull"
    }
}

function Test-IsJunction {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return $false }
    $item = Get-Item -LiteralPath $Path -Force
    return [bool]($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint)
}

function Move-ToJunction {
    [CmdletBinding(SupportsShouldProcess = $true)]
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Target,
        [Parameter(Mandatory = $true)][string]$AllowedRoot,
        [Parameter(Mandatory = $true)][string]$Label
    )

    Assert-TargetPath -Target $Target -AllowedRoot $AllowedRoot

    if (Test-IsJunction -Path $Source) {
        Write-Host "[SKIP] $Label is already redirected: $Source" -ForegroundColor DarkGray
        return
    }

    $sourceExists = Test-Path -LiteralPath $Source -PathType Container
    $targetExists = Test-Path -LiteralPath $Target -PathType Container

    if (-not $sourceExists -and $targetExists) {
        if ($PSCmdlet.ShouldProcess($Source, "Create junction to $Target")) {
            New-Item -ItemType Junction -Path $Source -Target $Target | Out-Null
            Write-Host "[LINK] $Label -> $Target" -ForegroundColor Green
        }
        return
    }

    if (-not $sourceExists) {
        Write-Host "[SKIP] $Label source does not exist: $Source" -ForegroundColor DarkGray
        return
    }

    if ($targetExists) {
        throw "Target already exists. Nothing was overwritten: $Target"
    }

    $targetParent = Split-Path -Parent $Target
    New-Item -ItemType Directory -Path $targetParent -Force | Out-Null

    if (-not $PSCmdlet.ShouldProcess($Source, "Move to $Target and create a junction")) { return }

    Write-Host "[MOVE] $Label" -ForegroundColor Cyan
    Move-Item -LiteralPath $Source -Destination $Target
    try {
        New-Item -ItemType Junction -Path $Source -Target $Target | Out-Null
        if (-not (Test-IsJunction -Path $Source)) {
            throw "Junction verification failed: $Source"
        }
        Write-Host "[OK]   $Source -> $Target" -ForegroundColor Green
    }
    catch {
        Write-Warning "Could not create the junction. Rolling this item back."
        if (Test-IsJunction -Path $Source) {
            Remove-Item -LiteralPath $Source -Force
        }
        if (-not (Test-Path -LiteralPath $Source) -and (Test-Path -LiteralPath $Target)) {
            Move-Item -LiteralPath $Target -Destination $Source
        }
        throw
    }
}

$allProcesses = Get-Process -ErrorAction SilentlyContinue
$codexMainProcesses = $allProcesses |
    Where-Object { $_.ProcessName -match '^(codex|codex-code-mode-host)$' }
if ($codexMainProcesses) {
    Write-Host ''
    Write-Host 'Codex is still running. Close every Codex window and run this script again.' -ForegroundColor Yellow
    Write-Host ('Running processes: ' + (($codexMainProcesses.ProcessName | Sort-Object -Unique) -join ', '))
    exit 2
}

$codexRunnerProcesses = $allProcesses |
    Where-Object { $_.ProcessName -match '^codex-command-runner-' }
if ($codexRunnerProcesses) {
    Write-Host ('Stopping leftover Codex command runners: ' + (($codexRunnerProcesses.ProcessName | Sort-Object -Unique) -join ', ')) -ForegroundColor Yellow
    $codexRunnerProcesses | Stop-Process -Force
    Start-Sleep -Milliseconds 800
    $remainingRunners = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.ProcessName -match '^codex-command-runner-' }
    if ($remainingRunners) {
        throw 'Some Codex command runners could not be stopped. End them in Task Manager and run this script again.'
    }
}

$dDrive = Get-PSDrive -Name D -PSProvider FileSystem -ErrorAction Stop
$dVolume = Get-Volume -DriveLetter D -ErrorAction Stop
if ($dVolume.FileSystem -ne 'NTFS') {
    throw "Drive D must use NTFS for directory junctions. Current filesystem: $($dVolume.FileSystem)"
}

$userProfile = [Environment]::GetFolderPath('UserProfile')
$roaming = [Environment]::GetFolderPath('ApplicationData')
$local = [Environment]::GetFolderPath('LocalApplicationData')
$userName = [Environment]::UserName

$codexRoot = "D:\CodexData\$userName"
$devCacheRoot = "D:\DevCache\$userName"
$moves = @(
    [pscustomobject]@{ Label = 'Codex home and sessions'; Source = (Join-Path $userProfile '.codex'); Target = (Join-Path $codexRoot 'home'); Root = $codexRoot },
    [pscustomobject]@{ Label = 'Codex roaming data'; Source = (Join-Path $roaming 'Codex'); Target = (Join-Path $codexRoot 'roaming'); Root = $codexRoot },
    [pscustomobject]@{ Label = 'npm cache'; Source = (Join-Path $local 'npm-cache'); Target = (Join-Path $devCacheRoot 'npm'); Root = $devCacheRoot },
    [pscustomobject]@{ Label = 'Maven repository'; Source = (Join-Path $userProfile '.m2\repository'); Target = (Join-Path $devCacheRoot 'maven'); Root = $devCacheRoot }
)

$requiredBytes = [int64]0
foreach ($move in $moves) {
    if (-not (Test-IsJunction -Path $move.Source) -and -not (Test-Path -LiteralPath $move.Target)) {
        $requiredBytes += Get-DirectorySizeBytes -Path $move.Source
    }
}
$safetyBytes = [int64](512MB)
if ($dDrive.Free -lt ($requiredBytes + $safetyBytes)) {
    throw ('Not enough free space on D:. Required about {0:N2} GB plus 0.5 GB safety margin; free {1:N2} GB.' -f ($requiredBytes / 1GB), ($dDrive.Free / 1GB))
}

Write-Host ''
Write-Host 'Codex global data migration' -ForegroundColor White
Write-Host ('Data to move: {0:N2} GB' -f ($requiredBytes / 1GB))
Write-Host "Codex target: $codexRoot"
Write-Host "Tool cache target: $devCacheRoot"
Write-Host 'Windows TEMP is intentionally unchanged.'
Write-Host ''

foreach ($move in $moves) {
    Move-ToJunction -Source $move.Source -Target $move.Target -AllowedRoot $move.Root -Label $move.Label -WhatIf:$WhatIfPreference
}

Write-Host ''
Write-Host 'Migration completed. Start Codex normally; all original C: paths now redirect to D:.' -ForegroundColor Green
Write-Host 'If Codex works normally, no further action is required.'
