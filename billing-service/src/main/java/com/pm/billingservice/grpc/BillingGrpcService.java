package com.pm.billingservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import billing.GetBillingAccountRequest;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.repository.BillingAccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(
      BillingGrpcService.class);

  private final BillingAccountRepository repository;

  public BillingGrpcService(BillingAccountRepository repository) {
    this.repository = repository;
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
}
