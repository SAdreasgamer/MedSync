package com.pm.billingservice.repository;

import com.pm.billingservice.model.BillingAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingAccountRepository extends JpaRepository<BillingAccount, UUID> {
  Optional<BillingAccount> findByPatientId(String patientId);
  boolean existsByPatientId(String patientId);
}
