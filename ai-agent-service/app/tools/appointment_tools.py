"""Tools for interacting with the appointment-service microservice."""

import httpx
from langchain_core.tools import tool

from app.config import get_settings


def _base() -> str:
    return f"{get_settings().appointment_service_url}/appointments"


@tool
def schedule_appointment(
    patient_id: str,
    doctor_name: str,
    department: str,
    appointment_date: str,
    time_slot: str,
    notes: str = "",
) -> dict:
    """Schedule a new appointment for a patient.

    Args:
        patient_id: UUID of the patient
        doctor_name: Name of the doctor (e.g. 'Dr. Mehta')
        department: Department name (e.g. 'Cardiology', 'General Medicine', 'Orthopedics')
        appointment_date: Date in YYYY-MM-DD format
        time_slot: Time slot string (e.g. '10:00 AM - 10:30 AM')
        notes: Optional notes for the appointment
    """
    payload = {
        "patientId": patient_id,
        "doctorName": doctor_name,
        "department": department,
        "appointmentDate": appointment_date,
        "timeSlot": time_slot,
        "notes": notes,
    }
    resp = httpx.post(_base(), json=payload, timeout=10)
    resp.raise_for_status()
    return resp.json()


@tool
def get_patient_appointments(patient_id: str) -> list:
    """Get all appointments for a specific patient.

    Args:
        patient_id: UUID of the patient
    """
    resp = httpx.get(f"{_base()}/patient/{patient_id}", timeout=10)
    resp.raise_for_status()
    return resp.json()


@tool
def get_today_appointments() -> list:
    """Get all appointments scheduled for today."""
    resp = httpx.get(f"{_base()}/today", timeout=10)
    resp.raise_for_status()
    return resp.json()


@tool
def get_appointments_by_date(date: str) -> list:
    """Get all appointments for a specific date.

    Args:
        date: Date in YYYY-MM-DD format
    """
    resp = httpx.get(f"{_base()}/date", params={"date": date}, timeout=10)
    resp.raise_for_status()
    return resp.json()


@tool
def update_appointment_status(appointment_id: str, status: str) -> dict:
    """Update the status of an appointment.

    Args:
        appointment_id: UUID of the appointment
        status: New status — one of SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    """
    resp = httpx.put(
        f"{_base()}/{appointment_id}/status",
        params={"status": status},
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()
