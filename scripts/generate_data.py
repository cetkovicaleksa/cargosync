import argparse
import csv
import json
import math
import random
import re
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path


# Password: 1234
BCRYPT_HASH = "$2a$10$soSOb8dof8PlD3BDnVh.Ou74X0DDs160Z1PlmZu5ioI0PvWgagxkG"

# Specification minimums
MIN_CUSTOMERS = 2_000_000
MIN_COMPANIES = 2_000_000
MIN_MANAGERS = 1_000_000
MIN_FACTORIES = 20_000
MIN_WAREHOUSES = 40_000
MIN_VANS = 100_000

FACTORIES_FOR_PRODUCTION_TS = 1_000
WAREHOUSES_FOR_TEMP_TS = 100
VANS_FOR_DISTANCE_TS = 100
PRODUCTS_PER_FACTORY_FOR_TS = 10
SECTORS_PER_WAREHOUSE = 3

NUM_PRODUCTS = 200

CITIES = [
    ("RS", "BEG", "Beograd"),
    ("RS", "NVS", "Novi Sad"),
    ("RS", "SUB", "Subotica"),
    ("RS", "SRM", "Sremska Mitrovica"),
    ("BA", "SJJ", "Sarajevo"),
    ("BA", "BNX", "Banja Luka"),
    ("BA", "BJA", "Bijeljina"),
    ("BA", "TBJ", "Trebinje"),
]

CATEGORIES = ["Carbonated", "Juice", "Energy", "Syrup", "Water", "Tea", "Coffee", "Sport"]
DRINK_ADJECTIVES = [
    "Classic", "Zero", "Max", "Blast", "Fresh", "Sparkling", "Natural", "Pure", "Cool", "Refreshing", "Delicious"
]
DRINK_BASES = ["Cola", "Orange", "Lemon", "Apple", "Peach", "Berry", "Water", "Cherry", "Grape", "Strawberry", "Raspberry"]

SECTOR_DEFS = [
    ("Ambient", "AMBIENT", 20.0),
    ("Chilled", "REFRIGERATED", 5.0),
    ("Frozen", "FROZEN", -19.0),
]

UUID_NAMESPACE = uuid.UUID("f7f69ff4-9ead-4e22-b4d9-4f97e6f79928")


@dataclass(frozen=True)
class Config:
    output_dir: Path
    seed: int
    customers: int
    companies: int
    managers: int
    factories: int
    warehouses: int
    vans: int


def deterministic_uuid(entity: str, idx: int) -> str:
    return str(uuid.uuid5(UUID_NAMESPACE, f"{entity}:{idx}"))


def open_csv(path: Path, headers: list[str]) -> tuple[csv.writer, object]:
    fh = path.open("w", newline="", encoding="utf-8")
    writer = csv.writer(fh)
    writer.writerow(headers)
    return writer, fh


def bool_str(value: bool) -> str:
    return "true" if value else "false"


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def load_van_model_refs(repo_root: Path) -> list[tuple[str, str]]:
    yaml_path = repo_root / "core" / "src" / "main" / "resources" / "van_makes.yaml"
    if not yaml_path.exists():
        return [("MAN", "TGX440"), ("BNZ", "SPR906"), ("FRD", "TRANS7")]

    models: list[tuple[str, str]] = []
    current_wmi: str | None = None
    in_models = False

    with yaml_path.open("r", encoding="utf-8") as fh:
        for raw in fh:
            line = raw.rstrip("\n")
            stripped = line.strip()

            if stripped.startswith("---"):
                current_wmi = None
                in_models = False
                continue

            wmi_match = re.match(r"^wmi:\s*['\"]?([0-9A-Z]{3})['\"]?\s*$", stripped)
            if wmi_match:
                current_wmi = wmi_match.group(1)
                in_models = False
                continue

            if stripped == "models:":
                in_models = True
                continue

            if not in_models or current_wmi is None:
                continue

            if line.startswith("  ") and not line.startswith("    ") and stripped.endswith(":"):
                key = stripped[:-1]
                if key != "name" and re.match(r"^[0-9A-Z]{6}$", key):
                    models.append((current_wmi, key))

    return models or [("MAN", "TGX440"), ("BNZ", "SPR906"), ("FRD", "TRANS7")]


def select_product_ids(factory_index: int, product_ids: list[str], count: int, rng_seed: int) -> list[str]:
    pick_rng = random.Random(rng_seed + 100_000 + factory_index)
    indices = pick_rng.sample(range(len(product_ids)), k=count)
    return [product_ids[i] for i in indices]


def generate_relational_data(cfg: Config, repo_root: Path) -> dict:
    rng = random.Random(cfg.seed)
    out = cfg.output_dir
    out.mkdir(parents=True, exist_ok=True)

    summary: dict[str, int] = {}

    print("[1/4] Generating products and categories...")
    product_categories_path = out / "product_categories.csv"
    products_path = out / "products.csv"

    category_ids = {name: deterministic_uuid("product-category", i) for i, name in enumerate(CATEGORIES)}
    product_ids: list[str] = []

    writer, fh = open_csv(product_categories_path, ["id", "name"])
    try:
        for name in CATEGORIES:
            writer.writerow([category_ids[name], name])
    finally:
        fh.close()

    writer, fh = open_csv(
        products_path,
        ["id", "name", "image_url", "price", "weight", "is_for_sale", "version", "category_id"],
    )
    try:
        for i in range(NUM_PRODUCTS):
            product_id = deterministic_uuid("product", i)
            product_ids.append(product_id)
            writer.writerow([
                product_id,
                f"{rng.choice(DRINK_BASES)} {rng.choice(DRINK_ADJECTIVES)} {rng.randint(1, 5)}L",
                f"https://picsum.photos/seed/product-{i}/600/400",
                f"{rng.uniform(50.0, 200.0):.2f}",
                f"{rng.uniform(0.50, 5.00):.2f}",
                bool_str(True),
                0,
                category_ids[rng.choice(CATEGORIES)],
            ])
    finally:
        fh.close()

    summary["product_categories"] = len(CATEGORIES)
    summary["products"] = NUM_PRODUCTS

    print("[2/4] Generating users/accounts/customers/managers/companies...")
    customers_users_path = out / "users_customers.csv"
    managers_users_path = out / "users_managers.csv"
    accounts_customers_path = out / "accounts_customers.csv"
    accounts_managers_path = out / "accounts_managers.csv"
    companies_path = out / "companies.csv"
    company_requests_path = out / "company_requests_approved.csv"

    first_customer_id = deterministic_uuid("customer", 0)

    writer_u_c, fh_u_c = open_csv(customers_users_path, ["id", "email", "avatar", "dtype", "first_name", "last_name"])
    writer_a_c, fh_a_c = open_csv(
        accounts_customers_path,
        ["id", "username", "password", "role", "owner_id", "enabled", "credentials_non_expired", "account_non_locked"],
    )
    try:
        for i in range(cfg.customers):
            customer_id = deterministic_uuid("customer", i)
            email = f"customer{i}@cargosync.rs"
            username = f"customer{i}"
            writer_u_c.writerow([
                customer_id,
                email,
                f"https://ui-avatars.com/api/?name=Customer+{i}",
                "Customer",
                f"Customer{i}",
                f"Load{i}",
            ])
            writer_a_c.writerow([
                deterministic_uuid("account-customer", i),
                username,
                BCRYPT_HASH,
                "CUSTOMER",
                customer_id,
                bool_str(True),
                bool_str(True),
                bool_str(True),
            ])
            if i > 0 and i % 200_000 == 0:
                print(f"  customers/accounts generated: {i}")
    finally:
        fh_u_c.close()
        fh_a_c.close()

    writer_u_m, fh_u_m = open_csv(managers_users_path, ["id", "email", "avatar", "dtype", "first_name", "last_name"])
    writer_a_m, fh_a_m = open_csv(
        accounts_managers_path,
        ["id", "username", "password", "role", "owner_id", "enabled", "credentials_non_expired", "account_non_locked"],
    )
    try:
        for i in range(cfg.managers):
            manager_id = deterministic_uuid("manager", i)
            email = f"manager{i}@cargosync.rs"
            username = f"manager{i}"
            writer_u_m.writerow([
                manager_id,
                email,
                f"https://ui-avatars.com/api/?name=Manager+{i}",
                "Manager",
                "",
                "",
            ])
            writer_a_m.writerow([
                deterministic_uuid("account-manager", i),
                username,
                BCRYPT_HASH,
                "MANAGER",
                manager_id,
                bool_str(True),
                bool_str(True),
                bool_str(True),
            ])
            if i > 0 and i % 200_000 == 0:
                print(f"  managers/accounts generated: {i}")
    finally:
        fh_u_m.close()
        fh_a_m.close()

    writer_comp, fh_comp = open_csv(
        companies_path,
        ["id", "owner_id", "name", "city_country_code", "city_code", "address", "latitude", "longitude"],
    )
    writer_req, fh_req = open_csv(
        company_requests_path,
        [
            "id", "created_by", "created_date", "last_modified_by", "last_modified_date",
            "name", "city_country_code", "city_code", "address", "latitude", "longitude", "status", "revision",
        ],
    )
    try:
        now = now_iso()
        for i in range(cfg.companies):
            city_country, city_code, city_name = CITIES[i % len(CITIES)]
            company_id = deterministic_uuid("company", i)

            # Ensure at least one customer owns >= 100 companies
            if i < 120:
                owner_id = first_customer_id
                owner_username = "customer0"
            else:
                owner_idx = i % cfg.customers
                owner_id = deterministic_uuid("customer", owner_idx)
                owner_username = f"customer{owner_idx}"

            lat = 43.0 + (i % 2500) * 0.001
            lon = 19.0 + (i % 2500) * 0.001

            writer_comp.writerow([
                company_id,
                owner_id,
                f"Company {i}",
                city_country,
                city_code,
                f"{city_name} Logistics Street {i % 500 + 1}",
                f"{lat:.2f}",
                f"{lon:.2f}",
            ])

            writer_req.writerow([
                deterministic_uuid("company-request", i),
                owner_username,
                now,
                owner_username,
                now,
                f"Company {i}",
                city_country,
                city_code,
                f"{city_name} Logistics Street {i % 500 + 1}",
                f"{lat:.2f}",
                f"{lon:.2f}",
                "APPROVED",
                0,
            ])

            if i > 0 and i % 200_000 == 0:
                print(f"  companies generated: {i}")
    finally:
        fh_comp.close()
        fh_req.close()

    summary["customers"] = cfg.customers
    summary["managers"] = cfg.managers
    summary["accounts_customers"] = cfg.customers
    summary["accounts_managers"] = cfg.managers
    summary["companies"] = cfg.companies
    summary["company_requests_approved"] = cfg.companies

    print("[3/4] Generating factories/warehouses/vans and relational links...")
    factories_path = out / "factories.csv"
    factory_statuses_path = out / "factory_statuses.csv"
    product_factories_path = out / "product_factories.csv"
    warehouses_path = out / "warehouses.csv"
    warehouse_status_path = out / "warehouse_status.csv"
    warehouse_sectors_path = out / "warehouse_sectors.csv"
    vans_path = out / "vans.csv"

    w_fact, f_fact = open_csv(
        factories_path,
        ["id", "name", "latitude", "longitude", "street", "country_code", "city_code", "version"],
    )
    w_fact_status, f_fact_status = open_csv(
        factory_statuses_path,
        ["factory_id", "is_online", "last_heartbeat"],
    )
    w_pf, f_pf = open_csv(product_factories_path, ["product_id", "factory_id"])

    try:
        now = now_iso()
        for i in range(cfg.factories):
            country, city_code, city_name = CITIES[i % len(CITIES)]
            factory_id = deterministic_uuid("factory", i)
            lat = 42.0 + (i % 3000) * 0.001
            lon = 18.0 + (i % 3000) * 0.001
            w_fact.writerow([
                factory_id,
                f"{city_name} Bottling Plant {i}",
                f"{lat:.2f}",
                f"{lon:.2f}",
                f"Industrial Zone {i % 300 + 1}",
                country,
                city_code,
                0,
            ])
            w_fact_status.writerow([factory_id, bool_str(i % 5 != 0), now])

            for pid in select_product_ids(i, product_ids, PRODUCTS_PER_FACTORY_FOR_TS, cfg.seed):
                w_pf.writerow([pid, factory_id])

            if i > 0 and i % 5_000 == 0:
                print(f"  factories generated: {i}")
    finally:
        f_fact.close()
        f_fact_status.close()
        f_pf.close()

    w_wh, f_wh = open_csv(
        warehouses_path,
        [
            "id", "created_by", "created_date", "last_modified_by", "last_modified_date",
            "name", "address", "city_country_code", "city_code", "latitude", "longitude", "version",
        ],
    )
    w_wh_status, f_wh_status = open_csv(
        warehouse_status_path,
        ["id", "warehouse_id", "online", "last_heartbeat", "version"],
    )
    w_wh_sector, f_wh_sector = open_csv(
        warehouse_sectors_path,
        ["id", "warehouse_id", "name", "climate_zone", "last_temperature", "last_temperature_at"],
    )
    try:
        now = now_iso()
        for i in range(cfg.warehouses):
            country, city_code, city_name = CITIES[i % len(CITIES)]
            warehouse_id = deterministic_uuid("warehouse", i)
            created_by = f"manager{i % cfg.managers}"
            lat = 42.5 + (i % 3000) * 0.001
            lon = 18.5 + (i % 3000) * 0.001

            w_wh.writerow([
                warehouse_id,
                created_by,
                now,
                created_by,
                now,
                f"{city_name} Warehouse {i}",
                f"Warehouse Street {i % 400 + 1}",
                country,
                city_code,
                f"{lat:.2f}",
                f"{lon:.2f}",
                0,
            ])
            w_wh_status.writerow([
                deterministic_uuid("warehouse-status", i),
                warehouse_id,
                bool_str(i % 6 != 0),
                now,
                0,
            ])

            for si, (sector_name, climate_zone, base_temp) in enumerate(SECTOR_DEFS, start=1):
                w_wh_sector.writerow([
                    deterministic_uuid("warehouse-sector", i * 10 + si),
                    warehouse_id,
                    sector_name,
                    climate_zone,
                    f"{base_temp:.2f}",
                    now,
                ])

            if i > 0 and i % 10_000 == 0:
                print(f"  warehouses generated: {i}")
    finally:
        f_wh.close()
        f_wh_status.close()
        f_wh_sector.close()

    van_model_refs = load_van_model_refs(repo_root)
    w_van, f_van = open_csv(
        vans_path,
        ["vi", "wmi", "vd", "registration_number", "max_payload_kg", "revision"],
    )
    try:
        for i in range(cfg.vans):
            wmi, vd = van_model_refs[i % len(van_model_refs)]
            vi = deterministic_uuid("van-vi", i).split("-")[0].upper()
            reg_num = f"BG-{1000 + (i % 9000)}-{chr(65 + (i % 26))}{chr(65 + ((i // 26) % 26))}"
            payload = 750 + (i % 6751)
            w_van.writerow([vi, wmi, vd, reg_num, payload, 0])
            if i > 0 and i % 20_000 == 0:
                print(f"  vans generated: {i}")
    finally:
        f_van.close()

    summary["factories"] = cfg.factories
    summary["factory_statuses"] = cfg.factories
    summary["product_factories"] = cfg.factories * PRODUCTS_PER_FACTORY_FOR_TS
    summary["warehouses"] = cfg.warehouses
    summary["warehouse_status"] = cfg.warehouses
    summary["warehouse_sectors"] = cfg.warehouses * SECTORS_PER_WAREHOUSE
    summary["vans"] = cfg.vans

    print("[4/4] Relational CSV generation finished.")
    return {"summary": summary, "product_ids": product_ids}


def generate_production_lp(cfg: Config, product_ids: list[str]) -> int:
    out_path = cfg.output_dir / "factory_production.lp"
    rng = random.Random(cfg.seed + 1)

    years = 4
    days = int(years * 365.25)  # 1461
    reports_per_day = 2
    interval_ns = 12 * 60 * 60 * 1_000_000_000

    start = datetime.now(timezone.utc) - timedelta(days=days)
    base_ns = int(start.timestamp() * 1_000_000_000)

    total_rows = 0
    with out_path.open("w", encoding="utf-8", newline="\n") as fh:
        for fi in range(FACTORIES_FOR_PRODUCTION_TS):
            factory_id = deterministic_uuid("factory", fi)
            pids = select_product_ids(fi, product_ids, PRODUCTS_PER_FACTORY_FOR_TS, cfg.seed)

            for t in range(days * reports_per_day):
                timestamp_ns = base_ns + t * interval_ns
                daily_phase = math.sin((t % reports_per_day) / reports_per_day * 2.0 * math.pi)

                for pid in pids:
                    produced = int(1200 + 900 * daily_phase + rng.randint(0, 2600))
                    produced = max(100, produced)
                    fh.write(
                        f"production,factory_id={factory_id},product_id={pid} amount={produced}i {timestamp_ns}\n"
                    )
                    total_rows += 1

            if fi > 0 and fi % 100 == 0:
                print(f"  production LP factories done: {fi}")

    return total_rows


def generate_warehouse_temperature_lp(cfg: Config) -> int:
    out_path = cfg.output_dir / "warehouse_temperature.lp"
    rng = random.Random(cfg.seed + 2)

    years = 2
    full_days = int(years * 365.25)  # 730
    extra_reports = 72  # half day, for exact 365.25 handling with 10-min cadence
    reports_per_day = 144
    report_count_per_sector = full_days * reports_per_day + extra_reports  # 105,192

    interval_ns = 10 * 60 * 1_000_000_000
    start = datetime.now(timezone.utc) - timedelta(days=full_days, hours=12)
    base_ns = int(start.timestamp() * 1_000_000_000)

    total_rows = 0
    with out_path.open("w", encoding="utf-8", newline="\n") as fh:
        for wi in range(WAREHOUSES_FOR_TEMP_TS):
            warehouse_id = deterministic_uuid("warehouse", wi)
            for sector_name, _, base in SECTOR_DEFS:
                for t in range(report_count_per_sector):
                    ts = base_ns + t * interval_ns
                    day_idx = t / reports_per_day
                    seasonal = math.sin(2.0 * math.pi * (day_idx / 365.25))
                    daily = math.sin(2.0 * math.pi * ((t % reports_per_day) / reports_per_day))
                    noise = rng.uniform(-0.3, 0.3)

                    if sector_name == "Frozen":
                        temp = base + seasonal * 0.7 + daily * 0.4 + noise
                    elif sector_name == "Chilled":
                        temp = base + seasonal * 1.3 + daily * 0.6 + noise
                    else:
                        temp = base + seasonal * 4.0 + daily * 1.6 + noise

                    fh.write(
                        f"temperature,warehouse_id={warehouse_id},sector={sector_name} value={temp:.2f} {ts}\n"
                    )
                    total_rows += 1

            if wi > 0 and wi % 20 == 0:
                print(f"  warehouse temperature LP done: {wi}")

    return total_rows


def generate_van_distance_lp(cfg: Config) -> int:
    out_path = cfg.output_dir / "van_distance.lp"
    rng = random.Random(cfg.seed + 3)

    years = 5
    full_days = int(years * 365.25)  # 1826
    extra_reports = 36  # quarter day for exact 365.25 handling with 10-min cadence
    reports_per_day = 144
    report_count_per_van = full_days * reports_per_day + extra_reports  # 262,980

    interval_ns = 10 * 60 * 1_000_000_000
    start = datetime.now(timezone.utc) - timedelta(days=full_days, hours=6)
    base_ns = int(start.timestamp() * 1_000_000_000)

    total_rows = 0
    with out_path.open("w", encoding="utf-8", newline="\n") as fh:
        for vi in range(VANS_FOR_DISTANCE_TS):
            van_id = deterministic_uuid("van-ts", vi)
            for t in range(report_count_per_van):
                ts = base_ns + t * interval_ns
                hour = (t % reports_per_day) / 6.0

                # Realistic cadence: mostly active between 06:00 and 22:00.
                if 6.0 <= hour <= 22.0:
                    base_distance = 1.2 + 1.8 * math.sin((hour - 6.0) / 16.0 * math.pi)
                    distance = max(0.0, base_distance + rng.uniform(-0.35, 0.65))
                else:
                    distance = max(0.0, rng.uniform(0.0, 0.2))

                fh.write(f"van_distance,van_id={van_id} distance_km={distance:.3f} {ts}\n")
                total_rows += 1

            if vi > 0 and vi % 20 == 0:
                print(f"  van distance LP done: {vi}")

    return total_rows


def write_manifest(cfg: Config, relational_summary: dict[str, int], ts_summary: dict[str, int]) -> None:
    manifest_path = cfg.output_dir / "generation_manifest.json"
    payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "config": {
            "customers": cfg.customers,
            "companies": cfg.companies,
            "managers": cfg.managers,
            "factories": cfg.factories,
            "warehouses": cfg.warehouses,
            "vans": cfg.vans,
            "seed": cfg.seed,
            "output_dir": str(cfg.output_dir),
        },
        "relational_rows": relational_summary,
        "time_series_rows": ts_summary,
        "notes": {
            "production_formula": "1000 factories * 10 products * 4*365.25 days * 2/day = 29,220,000",
            "warehouse_temp_formula": "100 warehouses * 3 sectors * 2*365.25 days * 144/day = 31,557,600",
            "van_distance_formula": "100 vans * 5*365.25 days * 144/day = 26,298,000",
            "approved_company_requests": "Generated in company_requests_approved.csv with status=APPROVED",
            "special_customer": "customer0 owns first 120 companies (>=100 requirement)",
        },
    }
    with manifest_path.open("w", encoding="utf-8") as fh:
        json.dump(payload, fh, indent=2)


def parse_args() -> Config:
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent
    default_output = repo_root / "scripts" / "results" / "generated_data"

    parser = argparse.ArgumentParser(description="Generate load-test data for CargoSync")
    parser.add_argument("--output-dir", type=Path, default=default_output)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--customers", type=int, default=MIN_CUSTOMERS)
    parser.add_argument("--companies", type=int, default=MIN_COMPANIES)
    parser.add_argument("--managers", type=int, default=MIN_MANAGERS)
    parser.add_argument("--factories", type=int, default=MIN_FACTORIES)
    parser.add_argument("--warehouses", type=int, default=MIN_WAREHOUSES)
    parser.add_argument("--vans", type=int, default=MIN_VANS)
    args = parser.parse_args()

    return Config(
        output_dir=args.output_dir,
        seed=args.seed,
        customers=max(args.customers, MIN_CUSTOMERS),
        companies=max(args.companies, MIN_COMPANIES),
        managers=max(args.managers, MIN_MANAGERS),
        factories=max(args.factories, MIN_FACTORIES),
        warehouses=max(args.warehouses, MIN_WAREHOUSES),
        vans=max(args.vans, MIN_VANS),
    )


def main() -> None:
    cfg = parse_args()
    repo_root = Path(__file__).resolve().parent.parent

    print("=== CargoSync bulk data generation started ===")
    print(f"Output directory: {cfg.output_dir}")
    print(
        f"Relational targets -> customers={cfg.customers:,}, companies={cfg.companies:,}, "
        f"managers={cfg.managers:,}, factories={cfg.factories:,}, warehouses={cfg.warehouses:,}, vans={cfg.vans:,}"
    )

    relational = generate_relational_data(cfg, repo_root)

    print("Generating LP: factory production (target 29,220,000 rows)...")
    production_rows = generate_production_lp(cfg, relational["product_ids"])

    print("Generating LP: warehouse temperatures (target 31,557,600 rows)...")
    temp_rows = generate_warehouse_temperature_lp(cfg)

    print("Generating LP: van distance (target 26,298,000 rows)...")
    van_rows = generate_van_distance_lp(cfg)

    ts_summary = {
        "factory_production.lp": production_rows,
        "warehouse_temperature.lp": temp_rows,
        "van_distance.lp": van_rows,
    }

    write_manifest(cfg, relational["summary"], ts_summary)

    print("=== Data generation completed ===")
    print(f"Production rows: {production_rows:,}")
    print(f"Warehouse temperature rows: {temp_rows:,}")
    print(f"Van distance rows: {van_rows:,}")
    print(f"Manifest: {cfg.output_dir / 'generation_manifest.json'}")


if __name__ == "__main__":
    main()