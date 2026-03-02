# CargoSync

CargoSync is a wholesale logistics platform with factory/warehouse monitoring, ordering and transport support.

## Technologies Used

- **Backend:** Java 21, Spring Boot, Spring Data JPA, Spring Security, RabbitMQ integration
- **Frontend:** Angular (TypeScript, SCSS)
- **Databases:** PostgreSQL (relational), InfluxDB 2.x (time-series)
- **Messaging/Infra:** RabbitMQ, Redis, Docker Compose, Nginx
- **Load testing/data prep:** Python (bulk test data generation), PowerShell and Bash import scripts

## Project Startup

### Prerequisites

- Docker + Docker Compose
- Python 3.12+ (for data generation script)
- For Windows loader: PowerShell 5.1+
- For Linux loader: Bash

### 1) Start infrastructure containers

From project root:

#### Dev stack (compose.yaml + compose.override.yaml)

```bash
docker compose -f dock/compose.yaml -f dock/compose.override.yaml up -d
```

#### Test stack (compose.yaml + compose.test.yaml)

```bash
docker compose -f dock/compose.yaml -f dock/compose.test.yaml up -d
```

### 2) (Optional) Start API + frontend for full app run

For the test stack, API and Nginx are included in `compose.test.yaml`.

If needed, stop stack with:

```bash
docker compose -f dock/compose.yaml -f dock/compose.test.yaml down
```

## Test Data Generation

Bulk test data is generated with:

```bash
python scripts/generate_data.py
```

Default output directory:

- `scripts/results/generated_data`

Generated artifacts include relational CSV files and time-series LP files (`*.lp`) used for load testing.

## Database Seeding Scripts (PostgreSQL + InfluxDB)

The repository includes scripts for importing generated data directly into Docker containers:

- **Windows/PowerShell:** `scripts/load_generated_data.ps1`
- **Linux/Bash:** `scripts/load_generated_data.sh`

### Windows (PowerShell)

#### Test stack import

```powershell
powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -TruncateFirst
```

#### Dry run (print commands only)

```powershell
powershell -ExecutionPolicy Bypass -File scripts/load_generated_data.ps1 -UseTestStack -DryRun
```

### Linux (Bash)

#### Test stack import

```bash
bash scripts/load_generated_data.sh --use-test-stack --truncate-first
```

#### Dry run (print commands only)

```bash
bash scripts/load_generated_data.sh --use-test-stack --dry-run
```

### Notes

- Import scripts assume generated files are in `scripts/results/generated_data` (override via script args).
- Postgres import uses ordered `COPY` for speed and FK-safe order.
- Influx import uses `influx write` with line protocol files.
- For large datasets, keep sufficient free disk and memory for Docker volumes.

## Contributors

1. Филип Тот <tot.sv14.2022@uns.ac.rs> • [@filip.tot983](https://gitlab.com/filip.tot983)  
2. Роман Аванесов <avanesov.sv88.2024@uns.ac.rs> • [@TortAlpha](https://gitlab.com/TortAlpha)  
3. Алекса Ћетковић <cetkovic.sv77.2022@uns.ac.rs> • [@cetkovicaleksa](https://gitlab.com/cetkovicaleksa)  
