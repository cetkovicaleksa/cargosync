"""Locust load scenarios for company requests and delivery vans.

Run one scenario at a time so the results describe one workflow:

  locust -f company_requests_and_vans_load_test.py VanManagementLoadUser
  locust -f company_requests_and_vans_load_test.py CompanyRequestLoadUser

Each workflow is sequential for one user. The scenarios are separate Locust
user classes and should not be selected together for an isolated measurement.
"""

import os
import uuid
from datetime import datetime, timedelta, timezone

from locust import HttpUser, SequentialTaskSet, between, task


API_PREFIX = os.getenv("CARGOSYNC_API_PREFIX", "/api").rstrip("/")
MANAGER_USERNAME = os.getenv("CARGOSYNC_MANAGER_USERNAME", "manager")
MANAGER_PASSWORD = os.getenv("CARGOSYNC_MANAGER_PASSWORD", "manager")
CUSTOMER_USERNAME = os.getenv("CARGOSYNC_CUSTOMER_USERNAME", "customer")
CUSTOMER_PASSWORD = os.getenv("CARGOSYNC_CUSTOMER_PASSWORD", "1234")


def api(path: str) -> str:
    return f"{API_PREFIX}/{path.lstrip('/')}"


class VanManagementWorkflow(SequentialTaskSet):
    def on_start(self):
        response = self.client.post(
            api("auth/login"),
            json={"username": MANAGER_USERNAME, "password": MANAGER_PASSWORD},
            name="POST /auth/login (manager)",
        )
        response.raise_for_status()
        self.headers = {"Authorization": f"Bearer {response.json()['accessToken']}"}
        self.vin = os.getenv("CARGOSYNC_LOAD_VIN", "BNZ-SPR906-1")

    @task
    def list_vans(self):
        response = self.client.get(
            api("vans?page=0&size=20&q=BG"),
            headers=self.headers,
            name="GET /vans (search)",
        )
        response.raise_for_status()

    @task
    def get_van(self):
        response = self.client.get(
            api(f"vans/{self.vin}"),
            headers=self.headers,
            name="GET /vans/{vin}",
        )
        if response.status_code not in (200, 404):
            response.raise_for_status()

    @task
    def get_status(self):
        response = self.client.get(
            api(f"vans/{self.vin}/status"),
            headers=self.headers,
            name="GET /vans/{vin}/status",
        )
        if response.status_code not in (200, 404):
            response.raise_for_status()

    @task
    def get_distance(self):
        now = datetime.now(timezone.utc)
        params = {
            "from": (now - timedelta(days=7)).isoformat().replace("+00:00", "Z"),
            "to": now.isoformat().replace("+00:00", "Z"),
        }
        response = self.client.get(
            api(f"vans/{self.vin}/distance"),
            params=params,
            headers=self.headers,
            name="GET /vans/{vin}/distance",
        )
        if response.status_code not in (200, 404):
            response.raise_for_status()


class CompanyRequestWorkflow(SequentialTaskSet):
    def on_start(self):
        response = self.client.post(
            api("auth/login"),
            json={"username": CUSTOMER_USERNAME, "password": CUSTOMER_PASSWORD},
            name="POST /auth/login (customer)",
        )
        response.raise_for_status()
        self.headers = {"Authorization": f"Bearer {response.json()['accessToken']}"}

    @task
    def create_request(self):
        request_id = uuid.uuid4().hex[:10]
        response = self.client.post(
            api("companies"),
            data={
                "name": f"Load Company {request_id}",
                "address": "Bulevar Evrope 12",
                "city.countryCode": "RS",
                "city.code": "NVS",
                "location.latitude": "45.2671",
                "location.longitude": "19.8335",
            },
            files={
                "images": ("company.jpg", b"load-test-image", "image/jpeg"),
                "proofs": ("ownership.pdf", b"load-test-proof", "application/pdf"),
            },
            headers=self.headers,
            name="POST /companies (multipart request)",
        )
        if response.status_code not in (201, 202):
            response.raise_for_status()


class VanManagementLoadUser(HttpUser):
    wait_time = between(0.2, 0.5)
    tasks = [VanManagementWorkflow]


class CompanyRequestLoadUser(HttpUser):
    wait_time = between(0.2, 0.5)
    tasks = [CompanyRequestWorkflow]