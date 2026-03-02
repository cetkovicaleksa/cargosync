#!/usr/bin/env bash
set -euo pipefail

DATA_DIR=""
COMPOSE_DIR=""
DB_SERVICE="postgres"
INFLUX_SERVICE="influxdb2"
ENV_FILE=""
USE_TEST_STACK=0
SKIP_COPY_IF_EXISTS=0
INFLUX_PARALLEL=0
TRUNCATE_FIRST=0
DRY_RUN=0
SKIP_POSTGRES=0
SKIP_INFLUX=0
COMPOSE_FILES=()
INFLUX_FILES=()

log_step() {
  echo "[loader] $*"
}

die() {
  echo "[loader][error] $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/load_generated_data.sh [options]

Options:
  --data-dir <path>         Path to generated_data directory
  --compose-dir <path>      Path to dock directory
  --compose-file <path>     Compose file (repeatable). If set, overrides stack auto-selection.
  --env-file <path>         Env file path (default: .env.dev or .env.test with --use-test-stack)
  --db-service <name>       Postgres service name (default: postgres)
  --influx-service <name>   Influx service name (default: influxdb2)
  --use-test-stack          Use compose.yaml + compose.test.yaml
  --skip-copy-if-exists     Skip docker cp when target file already exists in container
  --influx-parallel         Import selected LP files in parallel (one docker job per file)
  --influx-file <name>      Import only specific LP file (repeatable)
  --truncate-first          Truncate target tables before COPY
  --skip-postgres           Skip Postgres import
  --skip-influx             Skip Influx import
  --dry-run                 Print commands without executing
  --help                    Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --data-dir)
      DATA_DIR="$2"
      shift 2
      ;;
    --compose-dir)
      COMPOSE_DIR="$2"
      shift 2
      ;;
    --compose-file)
      COMPOSE_FILES+=("$2")
      shift 2
      ;;
    --env-file)
      ENV_FILE="$2"
      shift 2
      ;;
    --db-service)
      DB_SERVICE="$2"
      shift 2
      ;;
    --influx-service)
      INFLUX_SERVICE="$2"
      shift 2
      ;;
    --use-test-stack)
      USE_TEST_STACK=1
      shift
      ;;
    --skip-copy-if-exists)
      SKIP_COPY_IF_EXISTS=1
      shift
      ;;
    --influx-parallel)
      INFLUX_PARALLEL=1
      shift
      ;;
    --influx-file)
      INFLUX_FILES+=("$2")
      shift 2
      ;;
    --truncate-first)
      TRUNCATE_FIRST=1
      shift
      ;;
    --skip-postgres)
      SKIP_POSTGRES=1
      shift
      ;;
    --skip-influx)
      SKIP_INFLUX=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -z "$COMPOSE_DIR" ]]; then
  COMPOSE_DIR="$REPO_ROOT/dock"
fi
if [[ -z "$DATA_DIR" ]]; then
  DATA_DIR="$REPO_ROOT/scripts/results/generated_data"
fi
if [[ -z "$ENV_FILE" ]]; then
  if [[ $USE_TEST_STACK -eq 1 ]]; then
    ENV_FILE="$COMPOSE_DIR/.env.test"
  else
    ENV_FILE="$COMPOSE_DIR/.env.dev"
  fi
fi

[[ -d "$COMPOSE_DIR" ]] || die "Compose dir not found: $COMPOSE_DIR"
[[ -d "$DATA_DIR" ]] || die "Data dir not found: $DATA_DIR"
[[ -f "$ENV_FILE" ]] || die "Env file not found: $ENV_FILE"

read_env_var() {
  local key="$1"
  local value
  value="$(grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d= -f2- || true)"
  echo "$value"
}

DB_USER="$(read_env_var POSTGRES_USER)"
DB_NAME="$(read_env_var POSTGRES_DB)"
INFLUX_TOKEN="$(read_env_var DOCKER_INFLUXDB_INIT_ADMIN_TOKEN)"
INFLUX_ORG="$(read_env_var DOCKER_INFLUXDB_INIT_ORG)"
INFLUX_BUCKET="$(read_env_var DOCKER_INFLUXDB_INIT_BUCKET)"

if [[ $SKIP_POSTGRES -eq 0 ]]; then
  [[ -n "$DB_USER" && -n "$DB_NAME" ]] || die "POSTGRES_USER/POSTGRES_DB missing in $ENV_FILE"
fi
if [[ $SKIP_INFLUX -eq 0 ]]; then
  [[ -n "$INFLUX_TOKEN" && -n "$INFLUX_ORG" && -n "$INFLUX_BUCKET" ]] || die "Influx token/org/bucket missing in $ENV_FILE"
fi

COMPOSE_ARGS=(compose)
if [[ ${#COMPOSE_FILES[@]} -gt 0 ]]; then
  for f in "${COMPOSE_FILES[@]}"; do
    if [[ "$f" = /* ]]; then
      COMPOSE_ARGS+=(-f "$f")
    else
      COMPOSE_ARGS+=(-f "$COMPOSE_DIR/$f")
    fi
  done
else
  [[ -f "$COMPOSE_DIR/compose.yaml" ]] || die "compose.yaml not found in $COMPOSE_DIR"
  COMPOSE_ARGS+=(-f "$COMPOSE_DIR/compose.yaml")

  if [[ $USE_TEST_STACK -eq 1 ]]; then
    [[ -f "$COMPOSE_DIR/compose.test.yaml" ]] || die "compose.test.yaml not found in $COMPOSE_DIR"
    COMPOSE_ARGS+=(-f "$COMPOSE_DIR/compose.test.yaml")
  else
    if [[ -f "$COMPOSE_DIR/compose.override.yaml" ]]; then
      COMPOSE_ARGS+=(-f "$COMPOSE_DIR/compose.override.yaml")
    fi
  fi
fi

run_compose() {
  if [[ $DRY_RUN -eq 1 ]]; then
    echo "[dry-run] docker ${COMPOSE_ARGS[*]} $*"
    return
  fi
  docker "${COMPOSE_ARGS[@]}" "$@"
}

container_id() {
  local service="$1"
  if [[ $DRY_RUN -eq 1 ]]; then
    echo ""
    return
  fi
  local id
  id="$(docker "${COMPOSE_ARGS[@]}" ps -q "$service" | head -n 1)"
  [[ -n "$id" ]] || die "No running container for service '$service'. Start docker compose stack first."
  echo "$id"
}

copy_to_container() {
  local cid="$1"
  local src="$2"
  local dst="$3"

  if [[ $DRY_RUN -eq 1 ]]; then
    echo "[dry-run] docker cp $src ${cid}:$dst"
    return
  fi

  docker cp "$src" "${cid}:$dst"
}

container_file_exists() {
  local service="$1"
  local path="$2"

  if [[ $DRY_RUN -eq 1 ]]; then
    return 1
  fi

  docker "${COMPOSE_ARGS[@]}" exec -T "$service" sh -lc "test -f '$path'" >/dev/null 2>&1
}

normalize_lp_in_container() {
  local service="$1"
  local path="$2"

  run_compose exec -T "$service" sh -lc "sed -i 's/\r$//' '$path'"
}

csv_loads=(
  "product_categories|id,name|product_categories.csv"
  "products|id,name,image_url,price,weight,is_for_sale,version,category_id|products.csv"
  "users|id,email,avatar,dtype,first_name,last_name|users_customers.csv"
  "users|id,email,avatar,dtype,first_name,last_name|users_managers.csv"
  "accounts|id,username,password,role,owner_id,enabled,credentials_non_expired,account_non_locked|accounts_customers.csv"
  "accounts|id,username,password,role,owner_id,enabled,credentials_non_expired,account_non_locked|accounts_managers.csv"
  "companies|id,owner_id,name,city_country_code,city_code,address,latitude,longitude|companies.csv"
  "company_requests|id,created_by,created_date,last_modified_by,last_modified_date,name,city_country_code,city_code,address,latitude,longitude,status,revision|company_requests_approved.csv"
  "factories|id,name,latitude,longitude,street,country_code,city_code,version|factories.csv"
  "factory_statuses|factory_id,is_online,last_heartbeat|factory_statuses.csv"
  "product_factories|product_id,factory_id|product_factories.csv"
  "warehouses|id,created_by,created_date,last_modified_by,last_modified_date,name,address,city_country_code,city_code,latitude,longitude,version|warehouses.csv"
  "warehouse_status|id,warehouse_id,online,last_heartbeat,version|warehouse_status.csv"
  "warehouse_sectors|id,warehouse_id,name,climate_zone,last_temperature,last_temperature_at|warehouse_sectors.csv"
  "vans|vi,wmi,vd,registration_number,max_payload_kg,revision|vans.csv"
)

lp_loads=(
  "factory_production.lp"
  "warehouse_temperature.lp"
  "van_distance.lp"
)

if [[ ${#INFLUX_FILES[@]} -gt 0 ]]; then
  selected=()
  for wanted in "${INFLUX_FILES[@]}"; do
    found=0
    for file in "${lp_loads[@]}"; do
      if [[ "$wanted" == "$file" ]]; then
        selected+=("$file")
        found=1
        break
      fi
    done
    if [[ $found -eq 0 ]]; then
      die "Unknown --influx-file: $wanted (allowed: factory_production.lp, warehouse_temperature.lp, van_distance.lp)"
    fi
  done
  lp_loads=("${selected[@]}")
fi

for item in "${csv_loads[@]}"; do
  IFS='|' read -r _ _ file <<< "$item"
  [[ -f "$DATA_DIR/$file" ]] || die "Required file missing: $DATA_DIR/$file"
done
for item in "${lp_loads[@]}"; do
  file="$item"
  [[ -f "$DATA_DIR/$file" ]] || die "Required file missing: $DATA_DIR/$file"
done

log_step "Using data dir: $DATA_DIR"
log_step "Using compose dir: $COMPOSE_DIR"
log_step "Using env file: $ENV_FILE"
log_step "Using compose files: ${COMPOSE_ARGS[*]}"

if [[ $SKIP_POSTGRES -eq 0 ]]; then
  log_step "Preparing Postgres import into service '$DB_SERVICE'..."
  PG_CONTAINER_ID="$(container_id "$DB_SERVICE")"
  PG_TMP_DIR="/tmp/cargosync-load"

  run_compose exec -T "$DB_SERVICE" sh -lc "mkdir -p $PG_TMP_DIR"

  for item in "${csv_loads[@]}"; do
    IFS='|' read -r _ _ file <<< "$item"
    if [[ $SKIP_COPY_IF_EXISTS -eq 1 ]] && container_file_exists "$DB_SERVICE" "$PG_TMP_DIR/$file"; then
      log_step "Skipping copy (already exists): $file"
      continue
    fi
    log_step "Copying CSV -> container: $file"
    copy_to_container "$PG_CONTAINER_ID" "$DATA_DIR/$file" "$PG_TMP_DIR/$file"
  done

  if [[ $TRUNCATE_FIRST -eq 1 ]]; then
    log_step "Truncating target Postgres tables..."
    truncate_sql=$(cat <<'SQL'
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
SQL
)
    run_compose exec -T "$DB_SERVICE" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$truncate_sql"
  fi

  session_sql="SET synchronous_commit=off; SET maintenance_work_mem='1GB';"
  run_compose exec -T "$DB_SERVICE" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$session_sql"

  for item in "${csv_loads[@]}"; do
    IFS='|' read -r table columns file <<< "$item"
    log_step "COPY $file -> $table"
    copy_sql="COPY ${table} (${columns}) FROM '${PG_TMP_DIR}/${file}' WITH (FORMAT csv, HEADER true);"
    run_compose exec -T "$DB_SERVICE" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$copy_sql"
  done

  log_step "Postgres import completed."
fi

if [[ $SKIP_INFLUX -eq 0 ]]; then
  log_step "Preparing Influx import into service '$INFLUX_SERVICE'..."
  INFLUX_CONTAINER_ID="$(container_id "$INFLUX_SERVICE")"
  INFLUX_TMP_DIR="/tmp/cargosync-load"

  run_compose exec -T "$INFLUX_SERVICE" sh -lc "mkdir -p $INFLUX_TMP_DIR"

  for item in "${lp_loads[@]}"; do
    file="$item"
    if [[ $SKIP_COPY_IF_EXISTS -eq 1 ]] && container_file_exists "$INFLUX_SERVICE" "$INFLUX_TMP_DIR/$file"; then
      log_step "Skipping copy (already exists): $file"
      continue
    fi
    log_step "Copying LP -> container: $file"
    copy_to_container "$INFLUX_CONTAINER_ID" "$DATA_DIR/$file" "$INFLUX_TMP_DIR/$file"
  done

  for item in "${lp_loads[@]}"; do
    file="$item"
    log_step "Normalizing LP line endings: $file"
    normalize_lp_in_container "$INFLUX_SERVICE" "$INFLUX_TMP_DIR/$file"
  done

  if [[ $INFLUX_PARALLEL -eq 1 && ${#lp_loads[@]} -gt 1 && $DRY_RUN -eq 0 ]]; then
    log_step "Influx parallel import enabled (per-file jobs)."
    pids=()

    for item in "${lp_loads[@]}"; do
      file="$item"
      log_step "Influx write (parallel): $file"
      run_compose exec -T "$INFLUX_SERVICE" influx write \
        --host http://localhost:8086 \
        --token "$INFLUX_TOKEN" \
        --org "$INFLUX_ORG" \
        --bucket "$INFLUX_BUCKET" \
        --precision ns \
        --format lp \
        --errors-file "$INFLUX_TMP_DIR/$file.errors" \
        --file "$INFLUX_TMP_DIR/$file" &
      pids+=("$!")
    done

    failed=0
    for pid in "${pids[@]}"; do
      if ! wait "$pid"; then
        failed=1
      fi
    done
    [[ $failed -eq 0 ]] || die "One or more parallel Influx imports failed."
  else
    for item in "${lp_loads[@]}"; do
      file="$item"
      log_step "Influx write: $file"
      run_compose exec -T "$INFLUX_SERVICE" influx write \
        --host http://localhost:8086 \
        --token "$INFLUX_TOKEN" \
        --org "$INFLUX_ORG" \
        --bucket "$INFLUX_BUCKET" \
        --precision ns \
        --format lp \
        --errors-file "$INFLUX_TMP_DIR/$file.errors" \
        --file "$INFLUX_TMP_DIR/$file"
    done
  fi

  log_step "Influx import completed."
fi

log_step "All done."

echo
echo "Examples:"
echo "  bash scripts/load_generated_data.sh --use-test-stack --truncate-first"
echo "  bash scripts/load_generated_data.sh --use-test-stack --skip-copy-if-exists --skip-postgres"
echo "  bash scripts/load_generated_data.sh --use-test-stack --skip-copy-if-exists --skip-postgres --influx-parallel"
echo "  bash scripts/load_generated_data.sh --use-test-stack --skip-postgres --influx-file factory_production.lp"
echo "  bash scripts/load_generated_data.sh --use-test-stack --dry-run"
echo "  bash scripts/load_generated_data.sh --data-dir /path/to/generated_data"
