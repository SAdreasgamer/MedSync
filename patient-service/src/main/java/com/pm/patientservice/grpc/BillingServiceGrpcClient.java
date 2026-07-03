package com.pm.patientservice.grpc;

import billing.AddInvoiceRequest;
import billing.BillingDetailsResponse;
import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import billing.GetBillingAccountRequest;
import billing.InvoiceResponse;
import billing.RecordPaymentRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {

  private static final Logger log = LoggerFactory.getLogger(
      BillingServiceGrpcClient.class);
  private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

  public BillingServiceGrpcClient(
      @Value("${billing.service.address:localhost}") String serverAddress,
      @Value("${billing.service.grpc.port:9001}") int serverPort) {

    log.info("Connecting to Billing Service GRPC service at {}:{}",
        serverAddress, serverPort);

    ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,
        serverPort).usePlaintext().build();

    blockingStub = BillingServiceGrpc.newBlockingStub(channel);
  }

  public BillingResponse createBillingAccount(String patientId, String name,
      String email) {

    BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId)
        .setName(name).setEmail(email).build();

    BillingResponse response = blockingStub.createBillingAccount(request);
    log.info("Received response from billing service via GRPC: {}", response);
    return response;
  }

  public BillingDetailsResponse getBillingDetails(String patientId) {
    GetBillingAccountRequest request = GetBillingAccountRequest.newBuilder()
        .setPatientId(patientId).build();
    BillingDetailsResponse response = blockingStub.getBillingDetails(request);
    log.info("Received billing details from billing service via GRPC: {}", response.getAccountId());
    return response;
  }

  public InvoiceResponse addInvoice(String patientId, String description, double amount) {
    AddInvoiceRequest request = AddInvoiceRequest.newBuilder()
        .setPatientId(patientId)
        .setDescription(description)
        .setAmount(amount)
        .build();
    InvoiceResponse response = blockingStub.addInvoice(request);
    log.info("Invoice added via GRPC: {}", response.getInvoiceId());
    return response;
  }

  public InvoiceResponse recordPayment(String invoiceId) {
    RecordPaymentRequest request = RecordPaymentRequest.newBuilder()
        .setInvoiceId(invoiceId)
        .build();
    InvoiceResponse response = blockingStub.recordPayment(request);
    log.info("Payment recorded via GRPC for invoice: {}", response.getInvoiceId());
    return response;
  }

  public double getOutstandingSummary() {
    billing.EmptyRequest request = billing.EmptyRequest.newBuilder().build();
    billing.OutstandingSummaryResponse response = blockingStub.getOutstandingSummary(request);
    log.info("Outstanding summary retrieved via GRPC: {}", response.getTotalOutstanding());
    return response.getTotalOutstanding();
  }
}
