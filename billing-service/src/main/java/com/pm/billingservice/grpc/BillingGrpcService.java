package com.pm.billingservice.grpc;

import billing.AddInvoiceRequest;
import billing.BillingDetailsResponse;
import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import billing.GetBillingAccountRequest;
import billing.InvoiceResponse;
import billing.RecordPaymentRequest;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.Invoice;
import com.pm.billingservice.model.Invoice.InvoiceStatus;
import com.pm.billingservice.repository.BillingAccountRepository;
import com.pm.billingservice.repository.InvoiceRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(
      BillingGrpcService.class);

  private final BillingAccountRepository repository;
  private final InvoiceRepository invoiceRepository;

  public BillingGrpcService(BillingAccountRepository repository,
      InvoiceRepository invoiceRepository) {
    this.repository = repository;
    this.invoiceRepository = invoiceRepository;
  }

  @Override
  public void createBillingAccount(BillingRequest billingRequest,
      StreamObserver<BillingResponse> responseObserver) {

    log.info("createBillingAccount request received {}",
        billingRequest.toString());

    // Check if billing account already exists for this patient
    if (repository.existsByPatientId(billingRequest.getPatientId())) {
      BillingAccount existing = repository
          .findByPatientId(billingRequest.getPatientId()).get();
      responseObserver.onNext(BillingResponse.newBuilder()
          .setAccountId(existing.getId().toString())
          .setStatus(existing.getStatus())
          .build());
      responseObserver.onCompleted();
      return;
    }

    BillingAccount account = new BillingAccount();
    account.setPatientId(billingRequest.getPatientId());
    account.setName(billingRequest.getName());
    account.setEmail(billingRequest.getEmail());
    account.setStatus("ACTIVE");
    account.setCreatedDate(LocalDate.now());
    BillingAccount saved = repository.save(account);

    log.info("Billing account created with ID: {}", saved.getId());

    BillingResponse response = BillingResponse.newBuilder()
        .setAccountId(saved.getId().toString())
        .setStatus(saved.getStatus())
        .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getBillingAccount(GetBillingAccountRequest request,
      StreamObserver<BillingResponse> responseObserver) {

    log.info("getBillingAccount request received for patient {}",
        request.getPatientId());

    repository.findByPatientId(request.getPatientId()).ifPresentOrElse(
        account -> {
          responseObserver.onNext(BillingResponse.newBuilder()
              .setAccountId(account.getId().toString())
              .setStatus(account.getStatus())
              .build());
          responseObserver.onCompleted();
        },
        () -> {
          responseObserver.onError(Status.NOT_FOUND
              .withDescription(
                  "No billing account found for patient "
                      + request.getPatientId())
              .asRuntimeException());
        }
    );
  }

  @Override
  public void addInvoice(AddInvoiceRequest request,
      StreamObserver<InvoiceResponse> responseObserver) {

    log.info("addInvoice request: patient={}, desc={}, amount={}",
        request.getPatientId(), request.getDescription(), request.getAmount());

    repository.findByPatientId(request.getPatientId()).ifPresentOrElse(
        account -> {
          Invoice invoice = new Invoice();
          invoice.setBillingAccountId(account.getId());
          invoice.setDescription(request.getDescription());
          invoice.setAmount(BigDecimal.valueOf(request.getAmount()));
          invoice.setInvoiceDate(LocalDate.now());
          invoice.setStatus(InvoiceStatus.PENDING);
          Invoice saved = invoiceRepository.save(invoice);

          // Update account totals
          account.setTotalBilled(account.getTotalBilled().add(saved.getAmount()));
          account.setOutstandingBalance(account.getTotalBilled().subtract(account.getTotalPaid()));
          repository.save(account);

          responseObserver.onNext(InvoiceResponse.newBuilder()
              .setInvoiceId(saved.getId().toString())
              .setDescription(saved.getDescription())
              .setAmount(saved.getAmount().doubleValue())
              .setStatus(saved.getStatus().name())
              .setInvoiceDate(saved.getInvoiceDate().toString())
              .build());
          responseObserver.onCompleted();
        },
        () -> {
          responseObserver.onError(Status.NOT_FOUND
              .withDescription("No billing account for patient " + request.getPatientId())
              .asRuntimeException());
        }
    );
  }

  @Override
  public void recordPayment(RecordPaymentRequest request,
      StreamObserver<InvoiceResponse> responseObserver) {

    log.info("recordPayment request: invoiceId={}", request.getInvoiceId());

    invoiceRepository.findById(java.util.UUID.fromString(request.getInvoiceId()))
        .ifPresentOrElse(
            invoice -> {
              invoice.setStatus(InvoiceStatus.PAID);
              invoiceRepository.save(invoice);

              // Update account totals
              repository.findById(invoice.getBillingAccountId()).ifPresent(account -> {
                account.setTotalPaid(account.getTotalPaid().add(invoice.getAmount()));
                account.setOutstandingBalance(account.getTotalBilled().subtract(account.getTotalPaid()));
                repository.save(account);
              });

              responseObserver.onNext(InvoiceResponse.newBuilder()
                  .setInvoiceId(invoice.getId().toString())
                  .setDescription(invoice.getDescription())
                  .setAmount(invoice.getAmount().doubleValue())
                  .setStatus(invoice.getStatus().name())
                  .setInvoiceDate(invoice.getInvoiceDate().toString())
                  .build());
              responseObserver.onCompleted();
            },
            () -> {
              responseObserver.onError(Status.NOT_FOUND
                  .withDescription("Invoice not found: " + request.getInvoiceId())
                  .asRuntimeException());
            }
        );
  }

  @Override
  public void getBillingDetails(GetBillingAccountRequest request,
      StreamObserver<BillingDetailsResponse> responseObserver) {

    log.info("getBillingDetails request for patient {}", request.getPatientId());

    repository.findByPatientId(request.getPatientId()).ifPresentOrElse(
        account -> {
          List<Invoice> invoices = invoiceRepository.findByBillingAccountId(account.getId());

          BillingDetailsResponse.Builder builder = BillingDetailsResponse.newBuilder()
              .setAccountId(account.getId().toString())
              .setPatientId(account.getPatientId())
              .setStatus(account.getStatus())
              .setTotalBilled(account.getTotalBilled().doubleValue())
              .setTotalPaid(account.getTotalPaid().doubleValue())
              .setOutstandingBalance(account.getOutstandingBalance().doubleValue());

          if (account.getInsuranceProvider() != null) {
            builder.setInsuranceProvider(account.getInsuranceProvider());
          }
          if (account.getInsurancePolicyNumber() != null) {
            builder.setInsurancePolicyNumber(account.getInsurancePolicyNumber());
          }

          for (Invoice inv : invoices) {
            builder.addInvoices(InvoiceResponse.newBuilder()
                .setInvoiceId(inv.getId().toString())
                .setDescription(inv.getDescription())
                .setAmount(inv.getAmount().doubleValue())
                .setStatus(inv.getStatus().name())
                .setInvoiceDate(inv.getInvoiceDate().toString())
                .build());
          }

          responseObserver.onNext(builder.build());
          responseObserver.onCompleted();
        },
        () -> {
          responseObserver.onError(Status.NOT_FOUND
              .withDescription("No billing account for patient " + request.getPatientId())
              .asRuntimeException());
        }
    );
  }

  @Override
  public void getOutstandingSummary(billing.EmptyRequest request,
      StreamObserver<billing.OutstandingSummaryResponse> responseObserver) {

    log.info("getOutstandingSummary request received");

    java.math.BigDecimal total = repository.findAll().stream()
        .map(BillingAccount::getOutstandingBalance)
        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

    responseObserver.onNext(billing.OutstandingSummaryResponse.newBuilder()
        .setTotalOutstanding(total.doubleValue())
        .build());
    responseObserver.onCompleted();
  }
}
