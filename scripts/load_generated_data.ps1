[CmdletBinding()]
param(
    [string]$DataDir = "",
    [string]$ComposeDir = "",
    [string[]]$ComposeFiles = @(),
    [string]$DbService = "postgres",
    [string]$InfluxService = "influxdb2",
    [string]$EnvFile = "",
    [switch]$UseTestStack,
    [switch]$SkipCopyIfExists,
    [switch]$InfluxParallel,
    [string[]]$InfluxFiles = @(),
    [switch]$TruncateFirst,
    [switch]$DryRun,
    [switch]$SkipPostgres,
    [switch]$SkipInflux
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[loader] $Message" -ForegroundColor Cyan
}

function Resolve-DefaultPaths {
    $scriptDir = Split-Path -Parent $MyInvocation.ScriptName
    $repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path

    if ([string]::IsNullOrWhiteSpace($ComposeDir)) {
        $script:ComposeDir = Join-Path $repoRoot "dock"
    } else {
        $script:ComposeDir = (Resolve-Path $ComposeDir).Path
    }

    if ([string]::IsNullOrWhiteSpace($DataDir)) {
        $script:DataDir = Join-Path $repoRoot "scripts/results/generated_data"
    } else {
        $script:DataDir = (Resolve-Path $DataDir).Path
    }

    if ([string]::IsNullOrWhiteSpace($EnvFile)) {
        if ($UseTestStack) {
            $script:EnvFile = Join-Path $script:ComposeDir ".env.test"
        } else {
            $script:EnvFile = Join-Path $script:ComposeDir ".env.dev"
        }
    } elseif ([System.IO.Path]::IsPathRooted($EnvFile)) {
        $script:EnvFile = (Resolve-Path $EnvFile).Path
    } else {
        $script:EnvFile = (Resolve-Path (Join-Path $script:ComposeDir $EnvFile)).Path
    }
}

function Read-EnvFile {
    param([string]$Path)

    $values = @{}
    Get-Content -Path $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            return
        }

        $idx = $line.IndexOf("=")
        if ($idx -lt 1) {
            return
        }

        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        $values[$key] = $value
    }
    return $values
}

function Get-ComposeArgs {
    param(
        [string]$ComposeBaseDir,
        [string[]]$ExplicitComposeFiles,
        [bool]$UseTestingStack
    )

    $selectedFiles = @()

    if ($ExplicitComposeFiles.Count -gt 0) {
        foreach ($file in $ExplicitComposeFiles) {
            if ([System.IO.Path]::IsPathRooted($file)) {
                $resolvedFile = (Resolve-Path $file).Path
            } else {
                $resolvedFile = (Resolve-Path (Join-Path $ComposeBaseDir $file)).Path
            }
            $selectedFiles += $resolvedFile
        }
    } else {
        $compose = Join-Path $ComposeBaseDir "compose.yaml"
        if (-not (Test-Path $compose)) {
            throw "compose.yaml not found in $ComposeBaseDir"
        }
        $selectedFiles += $compose

        if ($UseTestingStack) {
            $testCompose = Join-Path $ComposeBaseDir "compose.test.yaml"
            if (-not (Test-Path $testCompose)) {
                throw "compose.test.yaml not found in $ComposeBaseDir"
            }
            $selectedFiles += $testCompose
        } else {
            $override = Join-Path $ComposeBaseDir "compose.override.yaml"
            if (Test-Path $override) {
                $selectedFiles += $override
            }
        }
    }

    $args = @("compose")
    foreach ($file in $selectedFiles) {
        $args += @("-f", $file)
    }

    return $args
}

function Invoke-Compose {
    param(
        [string[]]$ComposeArgs,
        [string[]]$Rest
    )

    $all = @($ComposeArgs + $Rest)
    $printable = "docker " + ($all -join " ")

    if ($DryRun) {
        Write-Host "[dry-run] $printable" -ForegroundColor Yellow
        return ""
    }

    $prevErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & docker @all
    } finally {
        $ErrorActionPreference = $prevErrorActionPreference
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $printable"
    }
}

function Get-ContainerId {
    param(
        [string[]]$ComposeArgs,
        [string]$Service
    )

    if ($DryRun) {
        return ""
    }

    $prevErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $id = (& docker @($ComposeArgs + @("ps", "-q", $Service)) 2>$null) | Select-Object -First 1
    } finally {
        $ErrorActionPreference = $prevErrorActionPreference
    }

    if ([string]::IsNullOrWhiteSpace($id)) {
        throw "No running container for service '$Service'. Start docker compose stack first."
    }
    return $id.Trim()
}

function Ensure-FileExists {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        throw "Required file missing: $Path"
    }
}

function Copy-ToContainer {
    param(
        [string]$ContainerId,
        [string]$HostPath,
        [string]$ContainerPath
    )

    if ($DryRun) {
        Write-Host ("[dry-run] docker cp {0} {1}:{2}" -f $HostPath, $ContainerId, $ContainerPath) -ForegroundColor Yellow
        return
    }

    $prevErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & docker cp $HostPath "$ContainerId`:$ContainerPath"
    } finally {
        $ErrorActionPreference = $prevErrorActionPreference
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to copy to container: $HostPath -> $ContainerPath"
    }
}

function Test-ContainerFileExists {
    param(
        [string[]]$ComposeArgs,
        [string]$Service,
        [string]$ContainerPath
    )

    if ($DryRun) {
        return $false
    }

    $prevErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & docker @($ComposeArgs + @("exec", "-T", $Service, "sh", "-lc", "test -f '$ContainerPath'")) *> $null
    } finally {
        $ErrorActionPreference = $prevErrorActionPreference
    }

    return $LASTEXITCODE -eq 0
}

function Normalize-LpFileInContainer {
    param(
        [string[]]$ComposeArgs,
        [string]$Service,
        [string]$ContainerPath
    )

    Invoke-Compose -ComposeArgs $ComposeArgs -Rest @(
        "exec", "-T", $Service,
        "sh", "-lc",
        "sed -i 's/\r`$//' '$ContainerPath'"
    )
}

function Escape-ShArg {
    param([string]$Value)
    if ($null -eq $Value) {
        return '""'
    }
    return '"' + ($Value -replace '"', '\\"') + '"'
}

function Invoke-InfluxWrite {
    param(
        [string[]]$ComposeArgs,
        [string]$Service,
        [string]$InfluxHost,
        [string]$Token,
        [string]$Org,
        [string]$Bucket,
        [string]$ErrorsFile,
        [string]$File
    )

    $cmd = "influx write --host {0} --token {1} --org {2} --bucket {3} --precision ns --format lp --errors-file {4} --file {5} < /dev/null" -f `
        (Escape-ShArg $InfluxHost),
        (Escape-ShArg $Token),
        (Escape-ShArg $Org),
        (Escape-ShArg $Bucket),
        (Escape-ShArg $ErrorsFile),
        (Escape-ShArg $File)

    Invoke-Compose -ComposeArgs $ComposeArgs -Rest @(
        "exec", "-T", $Service,
        "sh", "-lc", $cmd
    )
}

Resolve-DefaultPaths

Write-Step "Using data dir: $DataDir"
Write-Step "Using compose dir: $ComposeDir"
Write-Step "Using env file: $EnvFile"

Ensure-FileExists $EnvFile

$envVars = Read-EnvFile -Path $EnvFile
$dbUser = $envVars["POSTGRES_USER"]
$dbName = $envVars["POSTGRES_DB"]
$influxToken = $envVars["DOCKER_INFLUXDB_INIT_ADMIN_TOKEN"]
$influxOrg = $envVars["DOCKER_INFLUXDB_INIT_ORG"]
$influxBucket = $envVars["DOCKER_INFLUXDB_INIT_BUCKET"]

if ([string]::IsNullOrWhiteSpace($env:CARGOSYNC_RABBITMQ_USERNAME)) {
    $env:CARGOSYNC_RABBITMQ_USERNAME = $envVars["RABBITMQ_DEFAULT_USER"]
}
if ([string]::IsNullOrWhiteSpace($env:CARGOSYNC_RABBITMQ_PASSWORD)) {
    $env:CARGOSYNC_RABBITMQ_PASSWORD = $envVars["RABBITMQ_DEFAULT_PASS"]
}

if (-not $SkipPostgres -and ([string]::IsNullOrWhiteSpace($dbUser) -or [string]::IsNullOrWhiteSpace($dbName))) {
    throw "POSTGRES_USER/POSTGRES_DB missing in env file: $EnvFile"
}
if (-not $SkipInflux -and (
    [string]::IsNullOrWhiteSpace($influxToken) -or
    [string]::IsNullOrWhiteSpace($influxOrg) -or
    [string]::IsNullOrWhiteSpace($influxBucket)
)) {
    throw "Influx token/org/bucket missing in env file: $EnvFile"
}

$composeArgs = Get-ComposeArgs -ComposeBaseDir $ComposeDir -ExplicitComposeFiles $ComposeFiles -UseTestingStack $UseTestStack
Write-Step ("Using compose files: {0}" -f (($composeArgs | Select-Object -Skip 1) -join " "))

$csvLoads = @(
    @{ table = "product_categories"; columns = "id,name"; file = "product_categories.csv" },
    @{ table = "products"; columns = "id,name,image_url,price,weight,is_for_sale,version,category_id"; file = "products.csv" },
    @{ table = "users"; columns = "id,email,avatar,dtype,first_name,last_name"; file = "users_customers.csv" },
    @{ table = "users"; columns = "id,email,avatar,dtype,first_name,last_name"; file = "users_managers.csv" },
    @{ table = "accounts"; columns = "id,username,password,role,owner_id,enabled,credentials_non_expired,account_non_locked"; file = "accounts_customers.csv" },
    @{ table = "accounts"; columns = "id,username,password,role,owner_id,enabled,credentials_non_expired,account_non_locked"; file = "accounts_managers.csv" },
    @{ table = "companies"; columns = "id,owner_id,name,city_country_code,city_code,address,latitude,longitude"; file = "companies.csv" },
    @{ table = "company_requests"; columns = "id,created_by,created_date,last_modified_by,last_modified_date,name,city_country_code,city_code,address,latitude,longitude,status,revision"; file = "company_requests_approved.csv" },
    @{ table = "factories"; columns = "id,name,latitude,longitude,street,country_code,city_code,version"; file = "factories.csv" },
    @{ table = "factory_statuses"; columns = "factory_id,is_online,last_heartbeat"; file = "factory_statuses.csv" },
    @{ table = "product_factories"; columns = "product_id,factory_id"; file = "product_factories.csv" },
    @{ table = "warehouses"; columns = "id,created_by,created_date,last_modified_by,last_modified_date,name,address,city_country_code,city_code,latitude,longitude,version"; file = "warehouses.csv" },
    @{ table = "warehouse_status"; columns = "id,warehouse_id,online,last_heartbeat,version"; file = "warehouse_status.csv" },
    @{ table = "warehouse_sectors"; columns = "id,warehouse_id,name,climate_zone,last_temperature,last_temperature_at"; file = "warehouse_sectors.csv" },
    @{ table = "vans"; columns = "vi,wmi,vd,registration_number,max_payload_kg,revision"; file = "vans.csv" }
)

$lpLoads = @(
    @{ file = "factory_production.lp" },
    @{ file = "factory_availability.lp" },
    @{ file = "warehouse_temperature.lp" },
    @{ file = "van_distance.lp" }
)

if ($InfluxFiles.Count -gt 0) {
    $requested = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($name in $InfluxFiles) {
        [void]$requested.Add($name)
    }

    $lpLoads = @($lpLoads | Where-Object { $requested.Contains($_.file) })
    if ($lpLoads.Count -eq 0) {
        throw "No LP files selected. Available: factory_production.lp, factory_availability.lp, warehouse_temperature.lp, van_distance.lp"
    }
}

foreach ($load in $csvLoads) {
    Ensure-FileExists (Join-Path $DataDir $load.file)
}
foreach ($lp in $lpLoads) {
    Ensure-FileExists (Join-Path $DataDir $lp.file)
}

if (-not $SkipPostgres) {
    Write-Step "Preparing Postgres import into service '$DbService'..."

    $pgContainerId = Get-ContainerId -ComposeArgs $composeArgs -Service $DbService
    $pgTmpDir = "/tmp/cargosync-load"

    Invoke-Compose -ComposeArgs $composeArgs -Rest @("exec", "-T", $DbService, "sh", "-lc", "mkdir -p $pgTmpDir")

    foreach ($load in $csvLoads) {
        $hostFile = Join-Path $DataDir $load.file
        $containerFile = "$pgTmpDir/$($load.file)"
        if ($SkipCopyIfExists -and (Test-ContainerFileExists -ComposeArgs $composeArgs -Service $DbService -ContainerPath $containerFile)) {
            Write-Step "Skipping copy (already exists): $($load.file)"
            continue
        }
        Write-Step "Copying CSV -> container: $($load.file)"
        Copy-ToContainer -ContainerId $pgContainerId -HostPath $hostFile -ContainerPath $containerFile
    }

    if ($TruncateFirst) {
        Write-Step "Truncating target Postgres tables..."
        $truncateSql = @"
TRUNCATE TABLE
  product_factories,
  factory_statuses,
  warehouse_sectors,
  warehouse_status,
  companies,
  company_requests,
  factories,
  warehouses,
  vans,
  accounts,
  users,
  products,
  product_categories
RESTART IDENTITY CASCADE;
"@
        $truncateSqlEscaped = $truncateSql -replace '"', '\"'
        Invoke-Compose -ComposeArgs $composeArgs -Rest @(
            "exec", "-T", $DbService,
            "psql", "-U", $dbUser, "-d", $dbName,
            "-v", "ON_ERROR_STOP=1",
            "-c", $truncateSqlEscaped
        )
    }

    Write-Step "Running ordered Postgres COPY imports..."
    $sessionSql = @(
        "SET synchronous_commit=off;",
        "SET maintenance_work_mem='1GB';"
    ) -join " "

    Invoke-Compose -ComposeArgs $composeArgs -Rest @(
        "exec", "-T", $DbService,
        "psql", "-U", $dbUser, "-d", $dbName,
        "-v", "ON_ERROR_STOP=1",
        "-c", $sessionSql
    )

    foreach ($load in $csvLoads) {
        $copySql = "COPY $($load.table) ($($load.columns)) FROM '$pgTmpDir/$($load.file)' WITH (FORMAT csv, HEADER true);"
        Write-Step "COPY $($load.file) -> $($load.table)"

        Invoke-Compose -ComposeArgs $composeArgs -Rest @(
            "exec", "-T", $DbService,
            "psql", "-U", $dbUser, "-d", $dbName,
            "-v", "ON_ERROR_STOP=1",
            "-c", $copySql
        )
    }

    Write-Step "Postgres import completed."
}

if (-not $SkipInflux) {
    Write-Step "Preparing Influx import into service '$InfluxService'..."

    $influxContainerId = Get-ContainerId -ComposeArgs $composeArgs -Service $InfluxService
    $influxTmpDir = "/tmp/cargosync-load"

    Invoke-Compose -ComposeArgs $composeArgs -Rest @("exec", "-T", $InfluxService, "sh", "-lc", "mkdir -p $influxTmpDir")

    foreach ($lp in $lpLoads) {
        $hostFile = Join-Path $DataDir $lp.file
        $containerFile = "$influxTmpDir/$($lp.file)"
        if ($SkipCopyIfExists -and (Test-ContainerFileExists -ComposeArgs $composeArgs -Service $InfluxService -ContainerPath $containerFile)) {
            Write-Step "Skipping copy (already exists): $($lp.file)"
            continue
        }
        Write-Step "Copying LP -> container: $($lp.file)"
        Copy-ToContainer -ContainerId $influxContainerId -HostPath $hostFile -ContainerPath $containerFile
    }

    foreach ($lp in $lpLoads) {
        $containerFile = "$influxTmpDir/$($lp.file)"
        Write-Step "Normalizing LP line endings: $($lp.file)"
        Normalize-LpFileInContainer -ComposeArgs $composeArgs -Service $InfluxService -ContainerPath $containerFile
    }

    if ($InfluxParallel -and $lpLoads.Count -gt 1 -and -not $DryRun) {
        Write-Step "Influx parallel import enabled (per-file jobs)."
        $jobs = @()

        foreach ($lp in $lpLoads) {
            $containerFile = "$influxTmpDir/$($lp.file)"
            $errorsFile = "$influxTmpDir/$($lp.file).errors"
            Write-Step "Influx write (parallel): $($lp.file)"
            $cmd = "influx write --host {0} --token {1} --org {2} --bucket {3} --precision ns --format lp --errors-file {4} --file {5} < /dev/null" -f `
                (Escape-ShArg "http://localhost:8086"),
                (Escape-ShArg $influxToken),
                (Escape-ShArg $influxOrg),
                (Escape-ShArg $influxBucket),
                (Escape-ShArg $errorsFile),
                (Escape-ShArg $containerFile)

            $dockerArgs = @(
                $composeArgs + @(
                    "exec", "-T", $InfluxService,
                    "sh", "-lc", $cmd
                )
            )

            $jobs += Start-Job -ScriptBlock {
                param([string[]]$args)
                & docker @args
                if ($LASTEXITCODE -ne 0) {
                    throw "Command failed: docker $($args -join ' ')"
                }
            } -ArgumentList (,$dockerArgs)
        }

        Wait-Job -Job $jobs | Out-Null
        foreach ($job in $jobs) {
            Receive-Job -Job $job -ErrorAction Stop | Out-Null
            Remove-Job -Job $job
        }
    } else {
        foreach ($lp in $lpLoads) {
            $containerFile = "$influxTmpDir/$($lp.file)"
            $errorsFile = "$influxTmpDir/$($lp.file).errors"
            Write-Step "Influx write: $($lp.file)"
            try {
                Invoke-InfluxWrite -ComposeArgs $composeArgs -Service $InfluxService -InfluxHost "http://localhost:8086" -Token $influxToken -Org $influxOrg -Bucket $influxBucket -ErrorsFile $errorsFile -File $containerFile
            } catch {
                Write-Step "Influx write failed, printing error rows from $errorsFile"
                try {
                    Invoke-Compose -ComposeArgs $composeArgs -Rest @(
                        "exec", "-T", $InfluxService,
                        "sh", "-lc", "if [ -f '$errorsFile' ]; then tail -n 20 '$errorsFile'; fi"
                    )
                } catch {
                }
                throw
            }
        }
    }

    Write-Step "Influx import completed."
}

Write-Step "All done."
Write-Host ""
Write-Host "Examples:" -ForegroundColor Green
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -TruncateFirst"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -TruncateFirst"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -SkipCopyIfExists -SkipPostgres"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -SkipCopyIfExists -SkipPostgres -InfluxParallel"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -SkipPostgres -InfluxFiles factory_production.lp"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -SkipPostgres -InfluxFiles factory_availability.lp"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -DryRun"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -DataDir D:/path/to/generated_data"
