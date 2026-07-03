"""Patient management tools — REST calls to patient-service."""

import json
from datetime import date

import httpx
from langchain_core.tools import tool

from app.config import get_settings

settings = get_settings()
BASE_URL = settings.patient_service_url


@tool
def get_all_patients() -> str:
    """Get a list of all patients currently registered in the system."""
    response = httpx.get(f"{BASE_URL}/patients", timeout=10.0)
    if response.status_code == 200:
        patients = response.json()
        if not patients:
            return "No patients are currently registered in the system."
        return json.dumps(patients, indent=2)
    return f"Error fetching patients: HTTP {response.status_code}"


@tool
def get_patient_by_id(patient_id: str) -> str:
    """Get details of a specific patient by their UUID."""
    response = httpx.get(f"{BASE_URL}/patients/{patient_id}", timeout=10.0)
    if response.status_code == 200:
        return json.dumps(response.json(), indent=2)
    if response.status_code == 404:
        return f"Patient with ID '{patient_id}' was not found."
    return f"Error fetching patient: HTTP {response.status_code}"


@tool
def search_patients(query: str) -> str:
    """Search patients by name or email. Returns matching patients."""
    response = httpx.get(
        f"{BASE_URL}/patients/search", params={"q": query}, timeout=10.0
    )
    if response.status_code == 200:
        patients = response.json()
        if not patients:
            return f"No patients found matching '{query}'."
        return json.dumps(patients, indent=2)
    return f"Error searching patients: HTTP {response.status_code}"


@tool
def create_patient(
    name: str, email: str, date_of_birth: str, address: str,
    phone: str = "", gender: str = "", blood_group: str = "",
    emergency_contact_name: str = "", emergency_contact_phone: str = "",
) -> str:
    """Register a new patient in the system.

    Args:
        name: Full name of the patient.
        email: Email address (must be unique).
        date_of_birth: Date of birth in YYYY-MM-DD format.
        address: Home address of the patient.
        phone: Phone number (optional).
        gender: Gender — MALE, FEMALE, or OTHER (optional).
        blood_group: Blood group e.g. O+, AB- (optional).
        emergency_contact_name: Emergency contact name (optional).
        emergency_contact_phone: Emergency contact phone (optional).
    """
    payload = {
        "name": name,
        "email": email,
        "dateOfBirth": date_of_birth,
        "address": address,
        "registeredDate": str(date.today()),
    }
    if phone: payload["phone"] = phone
    if gender: payload["gender"] = gender
    if blood_group: payload["bloodGroup"] = blood_group
    if emergency_contact_name: payload["emergencyContactName"] = emergency_contact_name
    if emergency_contact_phone: payload["emergencyContactPhone"] = emergency_contact_phone

    response = httpx.post(
        f"{BASE_URL}/patients", json=payload, timeout=10.0
    )
    if response.status_code == 200:
        patient = response.json()
        return json.dumps(patient, indent=2)
    return f"Error creating patient: {response.text}"


@tool
def update_patient(
    patient_id: str, name: str, email: str, date_of_birth: str, address: str
) -> str:
    """Update an existing patient's information.

    Args:
        patient_id: UUID of the patient to update.
        name: Updated full name.
        email: Updated email address.
        date_of_birth: Updated date of birth (YYYY-MM-DD).
        address: Updated home address.
    """
    payload = {
        "name": name,
        "email": email,
        "dateOfBirth": date_of_birth,
        "address": address,
    }
    response = httpx.put(
        f"{BASE_URL}/patients/{patient_id}", json=payload, timeout=10.0
    )
    if response.status_code == 200:
        return json.dumps(response.json(), indent=2)
    return f"Error updating patient: {response.text}"


@tool
def delete_patient(patient_id: str) -> str:
    """Delete a patient from the system. This action cannot be undone.

    Args:
        patient_id: UUID of the patient to delete.
    """
    response = httpx.delete(
        f"{BASE_URL}/patients/{patient_id}", timeout=10.0
    )
    if response.status_code == 204:
        return f"Patient '{patient_id}' has been successfully deleted."
    if response.status_code == 404:
        return f"Patient '{patient_id}' was not found."
    return f"Error deleting patient: HTTP {response.status_code}"


@tool
def admit_patient(patient_id: str, room_number: str, bed_number: str) -> str:
    """Admit a patient to the hospital — assigns room and bed.

    Args:
        patient_id: UUID of the patient.
        room_number: Room number (e.g. ICU-201, GEN-305, PED-101).
        bed_number: Bed identifier (e.g. A, B, C).
    """
    response = httpx.put(
        f"{BASE_URL}/patients/{patient_id}/admit",
        params={"roomNumber": room_number, "bedNumber": bed_number},
        timeout=10.0,
    )
    if response.status_code == 200:
        return json.dumps(response.json(), indent=2)
    return f"Error admitting patient: {response.text}"


@tool
def discharge_patient(patient_id: str) -> str:
    """Discharge a patient from the hospital — clears room assignment.

    Args:
        patient_id: UUID of the patient.
    """
    response = httpx.put(
        f"{BASE_URL}/patients/{patient_id}/discharge", timeout=10.0
    )
    if response.status_code == 200:
        return json.dumps(response.json(), indent=2)
    return f"Error discharging patient: {response.text}"
