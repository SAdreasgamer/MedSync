"""Billing tools — gRPC calls to billing-service."""

import json
import sys
import os

import grpc
from langchain_core.tools import tool

from app.config import get_settings

# Add the generated stubs to the path
sys.path.insert(
    0,
    os.path.join(os.path.dirname(__file__), "..", "grpc_client", "generated"),
)

from app.grpc_client.generated import billing_service_pb2 as billing_pb2
from app.grpc_client.generated import (
    billing_service_pb2_grpc as billing_pb2_grpc,
)

settings = get_settings()


def _get_billing_stub():
    """Create a gRPC channel and billing stub."""
    channel = grpc.insecure_channel(
        f"{settings.billing_service_host}:{settings.billing_service_grpc_port}"
    )
    return channel, billing_pb2_grpc.BillingServiceStub(channel)


@tool
def create_billing_account(
    patient_id: str, name: str, email: str
) -> str:
    """Create a billing account for a patient.

    Args:
        patient_id: UUID of the patient.
        name: Patient's full name.
        email: Patient's email address.
    """
    channel, stub = _get_billing_stub()
    try:
        response = stub.CreateBillingAccount(
            billing_pb2.BillingRequest(
                patientId=patient_id, name=name, email=email
            ),
            timeout=10.0,
        )
        return json.dumps(
            {"accountId": response.accountId, "status": response.status}
        )
    except grpc.RpcError as e:
        return f"Error creating billing account: {e.details()}"
    finally:
        channel.close()


@tool
def get_billing_status(patient_id: str) -> str:
    """Check a patient's billing account status by their patient ID.

    Args:
        patient_id: UUID of the patient to check billing for.
    """
    channel, stub = _get_billing_stub()
    try:
        response = stub.GetBillingAccount(
            billing_pb2.GetBillingAccountRequest(patientId=patient_id),
            timeout=10.0,
        )
        return json.dumps(
            {"accountId": response.accountId, "status": response.status}
        )
    except grpc.RpcError as e:
        if e.code() == grpc.StatusCode.NOT_FOUND:
            return f"No billing account found for patient '{patient_id}'."
        return f"Error checking billing status: {e.details()}"
    finally:
        channel.close()
