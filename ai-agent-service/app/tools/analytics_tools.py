"""Analytics tools — REST calls to analytics-service."""

import json

import httpx
from langchain_core.tools import tool

from app.config import get_settings

settings = get_settings()
BASE_URL = settings.analytics_service_url


@tool
def get_patient_count() -> str:
    """Get the total number of patients registered in the system."""
    response = httpx.get(f"{BASE_URL}/analytics/patient-count", timeout=10.0)
    if response.status_code == 200:
        return json.dumps(response.json())
    return f"Error fetching patient count: HTTP {response.status_code}"


@tool
def get_recent_events() -> str:
    """Get the most recent patient events (registrations, updates, deletions)."""
    response = httpx.get(f"{BASE_URL}/analytics/recent-events", timeout=10.0)
    if response.status_code == 200:
        events = response.json()
        if not events:
            return "No recent events found."
        return json.dumps(events, indent=2)
    return f"Error fetching recent events: HTTP {response.status_code}"
