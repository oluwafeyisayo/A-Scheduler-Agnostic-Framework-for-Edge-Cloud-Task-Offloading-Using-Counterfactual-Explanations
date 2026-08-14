<#
Stage C network-sensitivity runner for EdgeCloudSim.

Creates and runs only the two new Stage C conditions:
  CONSTRAINED: WLAN=100 Mbps, WAN=8 Mbps, delay=0.2 s
  SEVERE:      WLAN=50 Mbps,  WAN=4 Mbps, delay=0.4 s

The NORMAL condition is reused from Stage A and is not regenerated.

Expected MainApp argument order:
  1. simulation configuration file
  2. output folder
  3. iteration number

Run this script from the EdgeCloudSim project root, or pass -ProjectRoot.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$ProjectRoot = (Get-Location).Path,

    [Parameter(Mandatory = $false)]
    [string]$BaseConfigRelativePath = "scripts\default_config.properties",

    [Parameter(Mandatory = $false)]
    [string]$MainClass =
        "edu.boun.edgecloudsim.applications.counterfactual_offloading.MainApp",

    [Parameter(Mandatory = $false)]
    [string]$ClassPath = "",

    [Parameter(Mandatory = $false)]
    [ValidateRange(1, 1000)]
    [int]$StartIteration = 1,

    [Parameter(Mandatory = $false)]
    [ValidateRange(1, 1000)]
    [int]$EndIteration = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($EndIteration -lt $StartIteration) {
    throw "EndIteration must be greater than or equal to StartIteration."
}

$ProjectRoot = (Resolve-Path $ProjectRoot).Path
$BaseConfigPath = Join-Path $ProjectRoot $BaseConfigRelativePath

if (-not (Test-Path -LiteralPath $BaseConfigPath -PathType Leaf)) {
    throw "Base configuration file was not found: $BaseConfigPath"
}

$BinPath = Join-Path $ProjectRoot "bin"

if (-not (Test-Path -LiteralPath $BinPath -PathType Container)) {
    throw "Compiled class folder was not found: $BinPath. Build the project in Eclipse first."
}

# Build the Java classpath automatically. The project does not need a root-level
# lib folder. All JAR files stored anywhere under the Eclipse project are added.
if ([string]::IsNullOrWhiteSpace($ClassPath)) {
    $classPathEntries = New-Object System.Collections.Generic.List[string]
    $classPathEntries.Add($BinPath)

    $jarFiles = Get-ChildItem `
        -LiteralPath $ProjectRoot `
        -Recurse `
        -File `
        -Filter "*.jar" `
        -ErrorAction SilentlyContinue

    foreach ($jarFile in $jarFiles) {
        if (-not $classPathEntries.Contains($jarFile.FullName)) {
            $classPathEntries.Add($jarFile.FullName)
        }
    }

    if ($classPathEntries.Count -eq 1) {
        Write-Warning "No JAR files were found under the project folder. Eclipse may be using external libraries; in that case pass -ClassPath explicitly."
    }

    $ClassPath = [string]::Join(
        [System.IO.Path]::PathSeparator,
        $classPathEntries
    )
}

Write-Host "Java classpath prepared with $($ClassPath.Split([System.IO.Path]::PathSeparator).Count) entries."

$JavaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $JavaCommand) {
    throw "Java was not found on PATH."
}

$Schedulers =
    "RANDOM,ROUND_ROBIN,LEAST_LOADED,MULTI_OBJECTIVE,PPO"

$Scenario = "TWO_TIER_WITH_EO"
$DeviceCount = 500

$Conditions = @(
    [pscustomobject]@{
        Name = "CONSTRAINED"
        FolderName = "constrained"
        WlanBandwidth = "100"
        WanBandwidth = "8"
        PropagationDelay = "0.2"
    },
    [pscustomobject]@{
        Name = "SEVERE"
        FolderName = "severe"
        WlanBandwidth = "50"
        WanBandwidth = "4"
        PropagationDelay = "0.4"
    }
)

function Set-PropertyValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Content,

        [Parameter(Mandatory = $true)]
        [string]$Key,

        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $escapedKey = [regex]::Escape($Key)
    $pattern = "(?m)^\s*$escapedKey\s*=.*$"

    if (-not [regex]::IsMatch($Content, $pattern)) {
        throw "Required property '$Key' was not found in the base configuration."
    }

    return [regex]::Replace(
        $Content,
        $pattern,
        "$Key=$Value",
        1
    )
}

function New-StageCConfig {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseContent,

        [Parameter(Mandatory = $true)]
        [pscustomobject]$Condition,

        [Parameter(Mandatory = $true)]
        [string]$DestinationPath
    )

    $content = $BaseContent

    # Fixed Stage C workload and scheduler matrix.
    $content = Set-PropertyValue $content `
        "min_number_of_mobile_devices" "$DeviceCount"
    $content = Set-PropertyValue $content `
        "max_number_of_mobile_devices" "$DeviceCount"
    $content = Set-PropertyValue $content `
        "mobile_device_counter_size" "$DeviceCount"
    $content = Set-PropertyValue $content `
        "orchestrator_policies" "$Schedulers"
    $content = Set-PropertyValue $content `
        "simulation_scenarios" "$Scenario"

    # Stage C network variables.
    $content = Set-PropertyValue $content `
        "wlan_bandwidth" $Condition.WlanBandwidth
    $content = Set-PropertyValue $content `
        "wan_bandwidth" $Condition.WanBandwidth

    # EdgeCloudSim uses the original misspelling "propogation".
    $content = Set-PropertyValue $content `
        "wan_propogation_delay" $Condition.PropagationDelay

    $destinationDirectory = Split-Path -Parent $DestinationPath
    New-Item -ItemType Directory `
        -Path $destinationDirectory `
        -Force | Out-Null

    [System.IO.File]::WriteAllText(
        $DestinationPath,
        $content,
        [System.Text.UTF8Encoding]::new($false)
    )
}

function Test-ProvenancePairCount {
    param(
        [Parameter(Mandatory = $true)]
        [string]$DecisionLogDirectory,

        [Parameter(Mandatory = $true)]
        [int]$Iteration
    )

    if (-not (Test-Path -LiteralPath $DecisionLogDirectory -PathType Container)) {
        return [pscustomobject]@{
            DecisionFiles = 0
            CandidateFiles = 0
            Valid = $false
        }
    }

    $allCsvFiles = Get-ChildItem `
        -LiteralPath $DecisionLogDirectory `
        -File `
        -Filter "*.csv"

    $iterationToken = "_500DEVICES_ITE$Iteration"

    $decisionFiles = @(
        $allCsvFiles | Where-Object {
            $_.Name -like "offloading_decisions_*${iterationToken}.csv" -and
            $_.Name -notlike "*_candidates.csv"
        }
    )

    $candidateFiles = @(
        $allCsvFiles | Where-Object {
            $_.Name -like "offloading_decisions_*${iterationToken}_candidates.csv"
        }
    )

    return [pscustomobject]@{
        DecisionFiles = $decisionFiles.Count
        CandidateFiles = $candidateFiles.Count
        Valid = (
            $decisionFiles.Count -eq 5 -and
            $candidateFiles.Count -eq 5
        )
    }
}

$BaseConfigContent =
    [System.IO.File]::ReadAllText($BaseConfigPath)

$StageCRoot =
    Join-Path $ProjectRoot "sim_results\stage_c"

$GeneratedConfigRoot =
    Join-Path $ProjectRoot "scripts\stage_c"

New-Item -ItemType Directory `
    -Path $StageCRoot `
    -Force | Out-Null

New-Item -ItemType Directory `
    -Path $GeneratedConfigRoot `
    -Force | Out-Null

$ManifestRows = New-Object System.Collections.Generic.List[object]

Push-Location $ProjectRoot

try {
    foreach ($condition in $Conditions) {

        $configFileName =
            "default_config_stage_c_" +
            $condition.FolderName +
            ".properties"

        $conditionConfigPath =
            Join-Path $GeneratedConfigRoot $configFileName

        New-StageCConfig `
            -BaseContent $BaseConfigContent `
            -Condition $condition `
            -DestinationPath $conditionConfigPath

        Write-Host ""
        Write-Host "============================================================"
        Write-Host "STAGE C CONDITION: $($condition.Name)"
        Write-Host "WLAN=$($condition.WlanBandwidth) Mbps; WAN=$($condition.WanBandwidth) Mbps; delay=$($condition.PropagationDelay) s"
        Write-Host "Configuration: $conditionConfigPath"
        Write-Host "============================================================"

        for (
            $iteration = $StartIteration;
            $iteration -le $EndIteration;
            $iteration++
        ) {
            $iterationOutput =
                Join-Path `
                    (Join-Path $StageCRoot $condition.FolderName) `
                    "ite$iteration"

            New-Item -ItemType Directory `
                -Path $iterationOutput `
                -Force | Out-Null

            Write-Host ""
            Write-Host "Running $($condition.Name), iteration $iteration ..."

            $startTime = Get-Date
            $status = "SUCCESS"
            $errorMessage = ""

            try {
                $javaArguments = @(
                    "-cp",
                    $ClassPath,
                    $MainClass,
                    $conditionConfigPath,
                    $iterationOutput,
                    $iteration.ToString()
                )

                & java @javaArguments

                if ($LASTEXITCODE -ne 0) {
                    throw "Java process exited with code $LASTEXITCODE."
                }
            }
            catch {
                $status = "FAILED"
                $errorMessage = $_.Exception.Message
            }

            $endTime = Get-Date
            $durationSeconds =
                [math]::Round(
                    ($endTime - $startTime).TotalSeconds,
                    3
                )

            $decisionLogDirectory =
                Join-Path $iterationOutput "decision_logs"

            $pairCheck =
                Test-ProvenancePairCount `
                    -DecisionLogDirectory $decisionLogDirectory `
                    -Iteration $iteration

            if (
                $status -eq "SUCCESS" -and
                -not $pairCheck.Valid
            ) {
                $status = "INCOMPLETE"
                $errorMessage =
                    "Expected 5 decision files and 5 candidate files, " +
                    "but found $($pairCheck.DecisionFiles) and " +
                    "$($pairCheck.CandidateFiles)."
            }

            $ManifestRows.Add(
                [pscustomobject]@{
                    stage = "STAGE_C"
                    condition = $condition.Name
                    iteration = $iteration
                    device_count = $DeviceCount
                    wlan_bandwidth_mbps =
                        [double]$condition.WlanBandwidth
                    wan_bandwidth_mbps =
                        [double]$condition.WanBandwidth
                    wan_propagation_delay_s =
                        [double]$condition.PropagationDelay
                    scheduler_count = 5
                    decision_file_count =
                        $pairCheck.DecisionFiles
                    candidate_file_count =
                        $pairCheck.CandidateFiles
                    output_directory = $iterationOutput
                    config_file = $conditionConfigPath
                    started_at =
                        $startTime.ToString("yyyy-MM-dd HH:mm:ss")
                    finished_at =
                        $endTime.ToString("yyyy-MM-dd HH:mm:ss")
                    duration_seconds = $durationSeconds
                    status = $status
                    error_message = $errorMessage
                }
            )

            if ($status -ne "SUCCESS") {
                Write-Error (
                    "Stage C run failed validation: " +
                    "$($condition.Name), iteration $iteration. " +
                    $errorMessage
                )
            }

            Write-Host (
                "Completed $($condition.Name), iteration $($iteration): " +
                "$($pairCheck.DecisionFiles) decision files and " +
                "$($pairCheck.CandidateFiles) candidate files."
            )
        }
    }
}
finally {
    Pop-Location

    $manifestPath =
        Join-Path $StageCRoot "stage_c_run_manifest.csv"

    $ManifestRows |
        Export-Csv `
            -LiteralPath $manifestPath `
            -NoTypeInformation `
            -Encoding UTF8

    Write-Host ""
    Write-Host "Stage C manifest written to:"
    Write-Host $manifestPath
}

Write-Host ""
Write-Host "Stage C simulation generation completed."
Write-Host "NORMAL is supplied by Stage A."
Write-Host "CONSTRAINED and SEVERE are under:"
Write-Host $StageCRoot
