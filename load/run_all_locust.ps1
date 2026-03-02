param(
    [string]$HostUrl = "http://localhost:8080",
    [switch]$IncludeManagers,
    [string]$LoginUsers = "120",
    [string]$ProductsUsers = "100",
    [string]$FactoriesUsers = "100",
    [string]$AnalyticsUsers = "80",
    [string]$ManagersUsers = "40",
    [string]$LoginRate = "12",
    [string]$ProductsRate = "10",
    [string]$FactoriesRate = "10",
    [string]$AnalyticsRate = "8",
    [string]$ManagersRate = "4",
    [string]$LoginTime = "5m",
    [string]$ProductsTime = "6m",
    [string]$FactoriesTime = "6m",
    [string]$AnalyticsTime = "8m",
    [string]$ManagersTime = "4m"
)

$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$locustPath = Join-Path $PSScriptRoot ".venv\Scripts\locust.exe"
$testFile = Join-Path $PSScriptRoot "tests\cargosync_locust.py"
$resultsDir = Join-Path $PSScriptRoot "results"

if (-not (Test-Path $locustPath)) {
    throw "Locust executable not found at: $locustPath"
}

if (-not (Test-Path $testFile)) {
    throw "Locust test file not found at: $testFile"
}

New-Item -Path $resultsDir -ItemType Directory -Force | Out-Null

$scenarios = @(
    @{ Name = "login"; Tag = "login"; Users = $LoginUsers; Rate = $LoginRate; Time = $LoginTime },
    @{ Name = "products"; Tag = "products"; Users = $ProductsUsers; Rate = $ProductsRate; Time = $ProductsTime },
    @{ Name = "factories"; Tag = "factories"; Users = $FactoriesUsers; Rate = $FactoriesRate; Time = $FactoriesTime },
    @{ Name = "factory_analytics"; Tag = "factory_analytics"; Users = $AnalyticsUsers; Rate = $AnalyticsRate; Time = $AnalyticsTime }
)

if ($IncludeManagers) {
    $scenarios += @{ Name = "managers"; Tag = "managers_search"; Users = $ManagersUsers; Rate = $ManagersRate; Time = $ManagersTime }
}

$summary = New-Object System.Collections.Generic.List[object]

foreach ($scenario in $scenarios) {
    $prefix = Join-Path $resultsDir $scenario.Name

    Write-Host "Running scenario '$($scenario.Name)' (tag=$($scenario.Tag), users=$($scenario.Users), rate=$($scenario.Rate), time=$($scenario.Time))"

    & $locustPath `
        -f $testFile `
        --host $HostUrl `
        --headless `
        -u $scenario.Users `
        -r $scenario.Rate `
        -t $scenario.Time `
        --tags $scenario.Tag `
        --csv $prefix

    if ($LASTEXITCODE -ne 0) {
        throw "Locust run failed for scenario '$($scenario.Name)' with exit code $LASTEXITCODE"
    }

    $statsFile = "${prefix}_stats.csv"
    if (-not (Test-Path $statsFile)) {
        throw "Expected stats file not found: $statsFile"
    }

    $agg = Import-Csv $statsFile | Where-Object { $_.Type -eq "Aggregated" } | Select-Object -First 1
    if (-not $agg) {
        throw "Aggregated row not found in: $statsFile"
    }

    $summary.Add([pscustomobject]@{
        Scenario  = $scenario.Name
        Requests  = [int]$agg."Request Count"
        Failures  = [int]$agg."Failure Count"
        FailPct   = [math]::Round([double]$agg."Failure Percentage", 2)
        Rps       = [math]::Round([double]$agg."Requests/s", 2)
        P95Ms     = [math]::Round([double]$agg."95%", 2)
        AvgMs     = [math]::Round([double]$agg."Average Response Time", 2)
    })
}

$summaryCsv = Join-Path $resultsDir "summary.csv"
$summaryMd = Join-Path $resultsDir "summary.md"

$summary | Export-Csv -Path $summaryCsv -NoTypeInformation -Encoding UTF8

$md = @(
    "| Scenario | Requests | Failures | Fail % | RPS | P95 (ms) | Avg (ms) |",
    "|---|---:|---:|---:|---:|---:|---:|"
)

foreach ($row in $summary) {
    $md += "| $($row.Scenario) | $($row.Requests) | $($row.Failures) | $($row.FailPct) | $($row.Rps) | $($row.P95Ms) | $($row.AvgMs) |"
}

$md | Set-Content -Path $summaryMd -Encoding UTF8

Write-Host ""
Write-Host "Done. Summary table files:"
Write-Host " - $summaryCsv"
Write-Host " - $summaryMd"
Write-Host ""
$summary | Format-Table -AutoSize
