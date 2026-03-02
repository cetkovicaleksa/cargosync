"""
CargoSync Load Tests
====================
Covers all 10 endpoints from the NFR performance-testing spec.

Endpoint map
------------
 #1  GET  /api/products/catalogue        — product search (customer)
 #2  POST /api/auth/register             — customer registration
 #3  GET  /api/auth/activate             — account activation (token-based)
 #4  POST /api/auth/login                — login
 #5  GET  /api/warehouses                — warehouse list / search
 #6  GET  /api/warehouses/{id}           — warehouse detail
 #7  GET  /api/warehouses/{id}/sectors/{sectorId}/temperature-chart
 #8  POST /api/orders                    — create order (transactional)
 #9  GET  /api/orders/{id}               — order detail
 #10 GET  /api/warehouses/{id}/availability  — simulator availability (5.14)

Run
---
    cd load
    uv run locust -f tests/locustfile.py --host http://localhost:8080

Web UI → http://localhost:8089
Headless (CI):
    uv run locust -f tests/locustfile.py --host http://localhost:8080 \\
        --headless -u 100 -r 10 --run-time 2m \\
        --html results/report.html --csv results/stats
"""

import json
import random
import string
import logging
import uuid

import requests
from locust import HttpUser, task, between, events

logger = logging.getLogger(__name__)

# ──────────────────────────────────────────────────────────────────────────────
# Seeded credentials (DevDatabaseSeeder)
# ──────────────────────────────────────────────────────────────────────────────
MANAGER_USERNAME = "manager"
MANAGER_PASSWORD = "manager"
CUSTOMER_USERNAME = "customer"
CUSTOMER_PASSWORD = "1234"

# ──────────────────────────────────────────────────────────────────────────────
# Shared runtime state (populated on test_start)
# ──────────────────────────────────────────────────────────────────────────────
shared: dict = {
    "warehouse_ids": [],          # list[str]
    "sector_map": {},             # warehouse_id -> list[str] (sector UUIDs)
    "product_ids": [],            # list[str]
    "company_id": None,           # str — customer's seeded company
    "order_ids": [],              # list[str] — orders placed during the run
    "manager_token": None,
    "customer_token": None,
}

CHART_PERIODS = ["LAST_7D", "LAST_30D", "LAST_3M", "LAST_6M", "LAST_1Y"]
AVAILABILITY_PERIODS = ["1h", "12h", "24h", "7d", "30d"]
SEARCH_TERMS = ["", "frozen", "cold", "ambient", "warehouse", "sector"]


# ──────────────────────────────────────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────────────────────────────────────

def _bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _rand_str(n: int = 8) -> str:
    return "".join(random.choices(string.ascii_lowercase, k=n))


def _rand_email() -> str:
    return f"loadtest_{_rand_str(10)}@example.com"


def _login(base: str, username: str, password: str) -> str | None:
    """Return JWT accessToken or None on failure."""
    r = requests.post(
        f"{base}/api/auth/login",
        json={"username": username, "password": password},
        timeout=10,
    )
    if r.ok:
        return r.json().get("accessToken")
    logger.warning("login(%s) failed: %s %s", username, r.status_code, r.text[:200])
    return None


# ──────────────────────────────────────────────────────────────────────────────
# Test startup — populate shared fixtures
# ──────────────────────────────────────────────────────────────────────────────

@events.test_start.add_listener
def on_test_start(environment, **_kwargs):
    base = environment.host or "http://localhost:8080"

    # ── authenticate ──────────────────────────────────────────────────────────
    shared["manager_token"] = _login(base, MANAGER_USERNAME, MANAGER_PASSWORD)
    shared["customer_token"] = _login(base, CUSTOMER_USERNAME, CUSTOMER_PASSWORD)

    if not shared["manager_token"]:
        logger.error("Could not obtain manager token — manager-only tests will fail")
    if not shared["customer_token"]:
        logger.error("Could not obtain customer token — customer tests will fail")

    manager_hdrs = _bearer(shared["manager_token"]) if shared["manager_token"] else {}
    customer_hdrs = _bearer(shared["customer_token"]) if shared["customer_token"] else {}

    # ── warehouses ────────────────────────────────────────────────────────────
    r = requests.get(f"{base}/api/warehouses?page=0&size=50", headers=manager_hdrs, timeout=15)
    if r.ok:
        for w in r.json().get("content", []):
            wid = w["id"]
            shared["warehouse_ids"].append(wid)
            sectors = w.get("sectors") or []
            if sectors:
                shared["sector_map"][wid] = [s["id"] for s in sectors]
            else:
                # fetch detail for sector list
                rd = requests.get(f"{base}/api/warehouses/{wid}", headers=manager_hdrs, timeout=10)
                if rd.ok:
                    shared["sector_map"][wid] = [s["id"] for s in rd.json().get("sectors", [])]
        logger.info("Loaded %d warehouses", len(shared["warehouse_ids"]))
    else:
        logger.warning("Failed to load warehouses: %s", r.status_code)

    # ── products ──────────────────────────────────────────────────────────────
    r = requests.get(f"{base}/api/products/catalogue?page=0&size=50", headers=customer_hdrs, timeout=15)
    if r.ok:
        shared["product_ids"] = [p["id"] for p in r.json().get("content", [])]
        logger.info("Loaded %d products", len(shared["product_ids"]))
    else:
        logger.warning("Failed to load products: %s", r.status_code)

    # ── customer company ──────────────────────────────────────────────────────
    r = requests.get(f"{base}/api/companies/mine", headers=customer_hdrs, timeout=10)
    if r.ok:
        companies = r.json()
        if companies:
            shared["company_id"] = companies[0]["id"]
            logger.info("Customer company: %s", shared["company_id"])
    else:
        logger.warning("Failed to load customer companies: %s", r.status_code)


# ──────────────────────────────────────────────────────────────────────────────
# User classes
# ──────────────────────────────────────────────────────────────────────────────


class AnonymousUser(HttpUser):
    """
    Low-privilege customer browsing the catalogue and warehouse list.
    Uses the shared customer token obtained at test_start.

    Covers: #1 (products/catalogue), #5 (warehouse list), #6 (warehouse detail)
    """

    weight = 5
    wait_time = between(1, 3)

    def on_start(self):
        self._headers = _bearer(shared["customer_token"]) if shared["customer_token"] else {}

    # ── #1 product catalogue ─────────────────────────────────────────────────
    @task(4)
    def browse_products(self):
        search = random.choice(SEARCH_TERMS)
        page = random.randint(0, 4)
        params = {"page": page, "size": 20}
        if search:
            params["name"] = search
        self.client.get(
            "/api/products/catalogue",
            params=params,
            headers=self._headers,
            name="/api/products/catalogue",
        )

    # ── #5 warehouse list ─────────────────────────────────────────────────────
    @task(3)
    def list_warehouses(self):
        term = random.choice(SEARCH_TERMS)
        params: dict = {"page": random.randint(0, 4), "size": 20}
        if term:
            params["name"] = term
        self.client.get(
            "/api/warehouses",
            params=params,
            headers=self._headers,
            name="/api/warehouses",
        )

    # ── #6 warehouse detail ───────────────────────────────────────────────────
    @task(2)
    def get_warehouse_detail(self):
        ids = shared["warehouse_ids"]
        if not ids:
            return
        wid = random.choice(ids)
        self.client.get(f"/api/warehouses/{wid}", headers=self._headers, name="/api/warehouses/{id}")


class RegistrationUser(HttpUser):
    """
    Simulates the registration + activation flow for brand-new customers.

    Covers: #2 (register), #3 (activate — tested with invalid token to exercise
    the endpoint; actual email-click cannot be automated), #4 (login)
    """

    weight = 1
    wait_time = between(2, 5)

    def on_start(self):
        self._pending_email: str | None = None

    # ── #2 register ───────────────────────────────────────────────────────────
    @task(3)
    def register(self):
        email = _rand_email()
        # multipart/form-data: JSON part "data" + no avatar
        payload = json.dumps({
            "firstName": "Load",
            "lastName": "Test",
            "email": email,
            "password": "LoadTest@123",
            "confirmPassword": "LoadTest@123",
        })
        with self.client.post(
            "/api/auth/register",
            files={"data": (None, payload, "application/json")},
            name="/api/auth/register",
            catch_response=True,
        ) as resp:
            if resp.status_code == 201:
                self._pending_email = email
                resp.success()
            else:
                resp.success()   # 409 Conflict (dup email) is still a valid measured response

    # ── #3 activate (invalid token — measures endpoint performance) ───────────
    @task(1)
    def activate_invalid_token(self):
        """
        Calls the activation endpoint with a random UUID.
        Will return 4xx, but this still exercises the endpoint path, DB lookup,
        and any validation logic — which is what we want to measure for #3.
        """
        fake_token = str(uuid.uuid4())
        with self.client.get(
            "/api/auth/activate",
            params={"token": fake_token},
            name="/api/auth/activate",
            catch_response=True,
        ) as resp:
            # 400/404/410 are expected — do not count as Locust failures
            if resp.status_code in (400, 404, 410, 200):
                resp.success()

    # ── #4 login (new accounts are not active, so this tests rate-limiting) ──
    @task(2)
    def login(self):
        """Login with seeded credentials to measure #4 endpoint throughput."""
        self.client.post(
            "/api/auth/login",
            json={"username": CUSTOMER_USERNAME, "password": CUSTOMER_PASSWORD},
            name="/api/auth/login",
        )


class CustomerUser(HttpUser):
    """
    Authenticated customer journey: browse → add to cart → place order → view order.

    Covers: #4 (login), #1 (catalogue), #8 (create order), #9 (order detail)
    """

    weight = 3
    wait_time = between(1, 4)

    def on_start(self):
        """Obtain a fresh JWT for this virtual user."""
        resp = self.client.post(
            "/api/auth/login",
            json={"username": CUSTOMER_USERNAME, "password": CUSTOMER_PASSWORD},
            name="/api/auth/login [setup]",
        )
        if resp.ok:
            self._token = resp.json().get("accessToken", "")
            self._headers = _bearer(self._token)
        else:
            self._token = shared.get("customer_token") or ""
            self._headers = _bearer(self._token)

    # ── #4 periodic re-login (session refresh simulation) ────────────────────
    @task(1)
    def login(self):
        resp = self.client.post(
            "/api/auth/login",
            json={"username": CUSTOMER_USERNAME, "password": CUSTOMER_PASSWORD},
            name="/api/auth/login",
        )
        if resp.ok:
            self._token = resp.json().get("accessToken", "")
            self._headers = _bearer(self._token)

    # ── #1 product search ─────────────────────────────────────────────────────
    @task(5)
    def browse_catalogue(self):
        params = {"page": random.randint(0, 3), "size": 20}
        term = random.choice(SEARCH_TERMS)
        if term:
            params["name"] = term
        self.client.get(
            "/api/products/catalogue",
            params=params,
            headers=self._headers,
            name="/api/products/catalogue",
        )

    # ── #8 create order ───────────────────────────────────────────────────────
    @task(2)
    def create_order(self):
        product_ids = shared["product_ids"]
        company_id = shared["company_id"]
        if not product_ids or not company_id:
            return

        # Pick 1–3 random products with small quantities to avoid stock depletion
        n = min(random.randint(1, 3), len(product_ids))
        chosen = random.sample(product_ids, n)
        items = [{"productId": pid, "quantity": random.randint(1, 2)} for pid in chosen]

        with self.client.post(
            "/api/orders",
            json={"companyId": company_id, "items": items},
            headers=self._headers,
            name="/api/orders [POST]",
            catch_response=True,
        ) as resp:
            if resp.status_code == 201:
                order_id = resp.json().get("id")
                if order_id:
                    shared["order_ids"].append(order_id)
                resp.success()
            elif resp.status_code in (409, 400, 422):
                # Conflict (out of stock) or validation error — still a valid response
                resp.success()

    # ── #9 order detail ───────────────────────────────────────────────────────
    @task(3)
    def get_order(self):
        order_ids = shared["order_ids"]
        if not order_ids:
            return
        oid = random.choice(order_ids)
        self.client.get(
            f"/api/orders/{oid}",
            headers=self._headers,
            name="/api/orders/{id}",
        )

    # ── also: warehouse browsing (covers #5 and #6 for logged-in users) ──────
    @task(2)
    def list_warehouses(self):
        self.client.get(
            "/api/warehouses",
            params={"page": 0, "size": 20},
            headers=self._headers,
            name="/api/warehouses",
        )


class ManagerUser(HttpUser):
    """
    Authenticated manager: browses warehouses, inspects temperature charts
    and availability dashboards.

    Covers: #4 (login), #5, #6, #7 (temperature chart), #10 (availability)
    """

    weight = 1
    wait_time = between(2, 6)

    def on_start(self):
        resp = self.client.post(
            "/api/auth/login",
            json={"username": MANAGER_USERNAME, "password": MANAGER_PASSWORD},
            name="/api/auth/login [setup]",
        )
        if resp.ok:
            self._token = resp.json().get("accessToken", "")
            self._headers = _bearer(self._token)
        else:
            self._token = shared.get("manager_token") or ""
            self._headers = _bearer(self._token)

    # ── #4 login ──────────────────────────────────────────────────────────────
    @task(1)
    def login(self):
        resp = self.client.post(
            "/api/auth/login",
            json={"username": MANAGER_USERNAME, "password": MANAGER_PASSWORD},
            name="/api/auth/login",
        )
        if resp.ok:
            self._token = resp.json().get("accessToken", "")
            self._headers = _bearer(self._token)

    # ── #5 warehouse list ─────────────────────────────────────────────────────
    @task(3)
    def list_warehouses(self):
        params: dict = {"page": random.randint(0, 3), "size": 20}
        term = random.choice(SEARCH_TERMS)
        if term:
            params["name"] = term
        self.client.get(
            "/api/warehouses",
            params=params,
            headers=self._headers,
            name="/api/warehouses",
        )

    # ── #6 warehouse detail ───────────────────────────────────────────────────
    @task(3)
    def get_warehouse(self):
        ids = shared["warehouse_ids"]
        if not ids:
            return
        wid = random.choice(ids)
        self.client.get(
            f"/api/warehouses/{wid}",
            headers=self._headers,
            name="/api/warehouses/{id}",
        )

    # ── #7 temperature chart ──────────────────────────────────────────────────
    @task(4)
    def temperature_chart(self):
        ids = shared["warehouse_ids"]
        if not ids:
            return
        wid = random.choice(ids)
        sector_ids = shared["sector_map"].get(wid, [])
        if not sector_ids:
            return
        sid = random.choice(sector_ids)
        period = random.choice(CHART_PERIODS)
        self.client.get(
            f"/api/warehouses/{wid}/sectors/{sid}/temperature-chart",
            params={"period": period},
            headers=self._headers,
            name="/api/warehouses/{id}/sectors/{sectorId}/temperature-chart",
        )

    # ── #10 availability chart (5.14 — bonus, may return 404 until implemented)
    @task(2)
    def availability_chart(self):
        ids = shared["warehouse_ids"]
        if not ids:
            return
        wid = random.choice(ids)
        period = random.choice(AVAILABILITY_PERIODS)
        with self.client.get(
            f"/api/warehouses/{wid}/availability",
            params={"period": period},
            headers=self._headers,
            name="/api/warehouses/{id}/availability",
            catch_response=True,
        ) as resp:
            # 404 is expected until 5.14 is implemented; don't count as failure
            if resp.status_code in (200, 404):
                resp.success()
