package com.pm.billingservice.repository;

import com.pm.billingservice.model.Invoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
  List<Invoice> findByBillingAccountId(UUID billingAccountId);
}
