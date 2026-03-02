import json
import os
import random
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any

from locust import HttpUser, between, constant, events, tag, task
from locust.exception import StopUser


API_PREFIX = os.getenv("CARGOSYNC_API_PREFIX", "/api").rstrip("/")

MANAGER_USERNAME = os.getenv("CARGOSYNC_MANAGER_USERNAME", "manager0")
MANAGER_PASSWORD = os.getenv("CARGOSYNC_MANAGER_PASSWORD", "1234")

SUPERMANAGER_USERNAME = os.getenv("CARGOSYNC_SUPERMANAGER_USERNAME", "admin")
SUPERMANAGER_PASSWORD = os.getenv("CARGOSYNC_SUPERMANAGER_PASSWORD", "")


def api_path(path: str) -> str:
    normalized = path if path.startswith("/") else f"/{path}"
    return f"{API_PREFIX}{normalized}"


def iso_utc(value: datetime) -> str:
    return value.astimezone(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def multipart_json(payload: dict) -> dict:
    return {
        "data": (None, json.dumps(payload), "application/json"),
    }


def page_content(payload: dict) -> list:
    content = payload.get("content")
    return content if isinstance(content, list) else []


@dataclass
class SharedIds:
    categories: list[str]
    product_ids: list[str]
    factory_ids: list[str]
    factory_product_pairs: list[tuple[str, str]]


class SharedState:
    ids: SharedIds | None = None
    optimistic_target: dict | None = None


class AuthMixin:
    client: Any
    token: str | None = None

    def login(self, username: str, password: str) -> str | None:
        response = self.client.post(
            api_path("/auth/login"),
            json={"username": username, "password": password},
            name="POST /auth/login",
        )
        if response.status_code != 200:
            return None

        data = response.json() if response.content else {}
        token = data.get("accessToken")
        return token if isinstance(token, str) and token else None

    def auth_headers(self) -> dict:
        return {"Authorization": f"Bearer {self.token}"}


class LoginStormUser(HttpUser):
    wait_time = constant(0)

    @tag("login")
    @task
    def login_storm(self):
        response = self.client.post(
            api_path("/auth/login"),
            json={"username": MANAGER_USERNAME, "password": MANAGER_PASSWORD},
            name="POST /auth/login",
        )
        if response.status_code != 200:
            response.failure(f"Login failed with status={response.status_code}")


class ManagerApiUser(HttpUser, AuthMixin):
    wait_time = between(0.05, 0.5)

    def on_start(self):
        self.token = self.login(MANAGER_USERNAME, MANAGER_PASSWORD)
        if not self.token:
            raise StopUser("Manager login failed; verify credentials and running backend.")

    @tag("categories")
    @task(8)
    def get_categories(self):
        response = self.client.get(
            api_path("/categories"),
            headers=self.auth_headers(),
            name="GET /api/categories",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")

    @tag("products", "products_unfiltered")
    @task(8)
    def search_products_unfiltered(self):
        response = self.client.get(
            api_path("/products?page=0&size=20"),
            headers=self.auth_headers(),
            name="GET /api/products",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")

    @tag("products", "products_filtered")
    @task(8)
    def search_products_filtered(self):
        response = self.client.get(
            api_path("/products?name=Cola&minPrice=50&page=0&size=20"),
            headers=self.auth_headers(),
            name="GET /api/products?name=Cola&minPrice=50",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")

    @tag("products", "products_create")
    @task(2)
    def create_product(self):
        if not SharedState.ids or not SharedState.ids.categories:
            return

        category_name = random.choice(SharedState.ids.categories)
        factory_ids = [random.choice(SharedState.ids.factory_ids)] if SharedState.ids.factory_ids else []

        payload = {
            "name": f"Load Product {random.randint(100000, 999999)}",
            "imageUrl": None,
            "price": round(random.uniform(55.0, 180.0), 2),
            "weight": round(random.uniform(0.5, 2.5), 2),
            "isForSale": True,
            "categoryName": category_name,
            "factoryIds": factory_ids,
            "version": 0,
        }

        response = self.client.post(
            api_path("/products"),
            headers=self.auth_headers(),
            files=multipart_json(payload),
            name="POST /api/products",
        )
        if response.status_code not in (201, 409):
            response.failure(f"Unexpected status={response.status_code}")

    @tag("products", "products_update", "optimistic_lock")
    @task(6)
    def update_same_product_concurrently(self):
        target = SharedState.optimistic_target
        if not target:
            return

        payload = {
            "name": f"{target['name']} Load {random.randint(1, 999)}",
            "imageUrl": target.get("imageUrl"),
            "price": target["price"],
            "weight": target["weight"],
            "isForSale": target["isForSale"],
            "categoryName": target["categoryName"],
            "factoryIds": target.get("factoryIds", []),
            "version": target["version"],
        }

        with self.client.put(
            api_path(f"/products/{target['id']}"),
            headers=self.auth_headers(),
            files=multipart_json(payload),
            name="PUT /api/products/{id}",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 409:
                response.success()
            else:
                response.failure(f"Expected 200 or 409, got {response.status_code}")

    @tag("factories", "factories_search")
    @task(7)
    def search_factories_by_city(self):
        response = self.client.get(
            api_path("/factories?cityCode=BEG&page=0&size=20"),
            headers=self.auth_headers(),
            name="GET /api/factories?cityCode=BEG",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")

    @tag("factories", "factories_get_one", "cache")
    @task(7)
    def get_factory_by_id(self):
        if not SharedState.ids or not SharedState.ids.factory_ids:
            return

        factory_id = random.choice(SharedState.ids.factory_ids)
        response = self.client.get(
            api_path(f"/factories/{factory_id}"),
            headers=self.auth_headers(),
            name="GET /api/factories/{id}",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")

    @tag("factory_analytics", "influx_heavy")
    @task(5)
    def get_factory_product_production(self):
        if not SharedState.ids or not SharedState.ids.factory_product_pairs:
            return

        factory_id, product_id = random.choice(SharedState.ids.factory_product_pairs)
        to_dt = datetime.now(timezone.utc)
        from_dt = to_dt - timedelta(days=180)

        response = self.client.get(
            api_path(
                f"/factories/{factory_id}/products/{product_id}/production"
                f"?from={iso_utc(from_dt)}&to={iso_utc(to_dt)}"
            ),
            headers=self.auth_headers(),
            name="GET /api/factories/{id}/products/{id}/production?6m",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")


class SuperManagerApiUser(HttpUser, AuthMixin):
    wait_time = between(0.05, 0.4)

    def on_start(self):
        if not SUPERMANAGER_PASSWORD:
            raise StopUser("Set CARGOSYNC_SUPERMANAGER_PASSWORD to run manager search scenario.")

        self.token = self.login(SUPERMANAGER_USERNAME, SUPERMANAGER_PASSWORD)
        if not self.token:
            raise StopUser("Supermanager login failed; verify credentials for /api/managers scenario.")

    @tag("managers_search")
    @task
    def search_managers(self):
        response = self.client.get(
            api_path("/managers?query=manager555&page=0&size=20"),
            headers=self.auth_headers(),
            name="GET /api/managers?query=manager555",
        )
        if response.status_code != 200:
            response.failure(f"Unexpected status={response.status_code}")


@events.test_start.add_listener
def preload_ids(environment, **kwargs):
    host = environment.host
    if not host:
        return

    class BootstrapUser(HttpUser, AuthMixin):
        abstract = True

    bootstrap = BootstrapUser(environment)
    bootstrap.host = host

    token = bootstrap.login(MANAGER_USERNAME, MANAGER_PASSWORD)
    if not token:
        return

    headers = {"Authorization": f"Bearer {token}"}

    categories = []
    category_res = bootstrap.client.get(api_path("/categories"), headers=headers, name="bootstrap categories")
    if category_res.status_code == 200 and category_res.content:
        payload = category_res.json()
        if isinstance(payload, list):
            categories = [
                name
                for item in payload
                if isinstance(item, dict)
                for name in [item.get("name")]
                if isinstance(name, str) and name
            ]

    product_ids = []
    products_res = bootstrap.client.get(api_path("/products?page=0&size=100"), headers=headers, name="bootstrap products")
    optimistic_target = None
    if products_res.status_code == 200 and products_res.content:
        payload = products_res.json()
        for item in page_content(payload):
            if not isinstance(item, dict) or not item.get("id"):
                continue
            product_ids.append(item["id"])
            if optimistic_target is None and item.get("categoryName") and item.get("version") is not None:
                optimistic_target = {
                    "id": item["id"],
                    "name": item.get("name") or "Load Product",
                    "imageUrl": item.get("imageUrl"),
                    "price": item.get("price", 99.99),
                    "weight": item.get("weight", 1.0),
                    "isForSale": item.get("isForSale", True),
                    "categoryName": item.get("categoryName"),
                    "factoryIds": item.get("factoryIds") or [],
                    "version": item.get("version"),
                }

    factory_ids = []
    factories_res = bootstrap.client.get(api_path("/factories?page=0&size=100"), headers=headers, name="bootstrap factories")
    if factories_res.status_code == 200 and factories_res.content:
        payload = factories_res.json()
        for item in page_content(payload):
            if isinstance(item, dict) and item.get("id"):
                factory_ids.append(item["id"])

    pairs: list[tuple[str, str]] = []
    for factory_id in factory_ids[:15]:
        products_in_factory_res = bootstrap.client.get(
            api_path(f"/factories/{factory_id}/products"), headers=headers, name="bootstrap factory products"
        )
        if products_in_factory_res.status_code != 200 or not products_in_factory_res.content:
            continue
        payload = products_in_factory_res.json()
        if not isinstance(payload, list):
            continue
        for item in payload[:5]:
            if isinstance(item, dict) and item.get("id"):
                pairs.append((factory_id, item["id"]))

    SharedState.ids = SharedIds(
        categories=categories,
        product_ids=product_ids,
        factory_ids=factory_ids,
        factory_product_pairs=pairs,
    )
    SharedState.optimistic_target = optimistic_target
