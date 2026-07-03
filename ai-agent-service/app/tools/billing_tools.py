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


@tool
def add_invoice(patient_id: str, description: str, amount: float) -> str:
    """Add a billing charge/invoice to a patient.

    Args:
        patient_id: UUID of the patient.
        description: Description of the charge (e.g. 'Lab Work - CBC', 'Room Charge').
        amount: The monetary amount of the charge.
    """
    channel, stub = _get_billing_stub()
    try:
        response = stub.AddInvoice(
            billing_pb2.AddInvoiceRequest(
                patientId=patient_id,
                description=description,
                amount=amount,
            ),
            timeout=10.0,
        )
        return json.dumps({
            "invoiceId": response.invoiceId,
            "description": response.description,
            "amount": response.amount,
            "status": response.status,
            "invoiceDate": response.invoiceDate,
        })
    except grpc.RpcError as e:
        return f"Error adding invoice: {e.details()}"
    finally:
        channel.close()


@tool
def record_payment(invoice_id: str) -> str:
    """Record a payment for a specific invoice, marking it as PAID.

    Args:
        invoice_id: UUID of the invoice being paid.
    """
    channel, stub = _get_billing_stub()
    try:
        response = stub.RecordPayment(
            billing_pb2.RecordPaymentRequest(invoiceId=invoice_id),
            timeout=10.0,
        )
        return json.dumps({
            "invoiceId": response.invoiceId,
            "description": response.description,
            "amount": response.amount,
            "status": response.status,
            "invoiceDate": response.invoiceDate,
        })
    except grpc.RpcError as e:
        return f"Error recording payment: {e.details()}"
    finally:
        channel.close()


@tool
def get_billing_details(patient_id: str) -> str:
    """Get the full billing details for a patient, including their invoices and totals.

    Args:
        patient_id: UUID of the patient.
    """
    channel, stub = _get_billing_stub()
    try:
        response = stub.GetBillingDetails(
            billing_pb2.GetBillingAccountRequest(patientId=patient_id),
            timeout=10.0,
        )
        invoices = []
        for inv in response.invoices:
            invoices.append({
                "invoiceId": inv.invoiceId,
                "description": inv.description,
                "amount": inv.amount,
                "status": inv.status,
                "invoiceDate": inv.invoiceDate,
            })
        return json.dumps({
            "accountId": response.accountId,
            "patientId": response.patientId,
            "status": response.status,
            "insuranceProvider": response.insuranceProvider,
            "insurancePolicyNumber": response.insurancePolicyNumber,
            "totalBilled": response.totalBilled,
            "totalPaid": response.totalPaid,
            "outstandingBalance": response.outstandingBalance,
            "invoices": invoices,
        })
    except grpc.RpcError as e:
        if e.code() == grpc.StatusCode.NOT_FOUND:
            return f"No billing details found for patient '{patient_id}'."
        return f"Error fetching billing details: {e.details()}"
    finally:
        channel.close()
