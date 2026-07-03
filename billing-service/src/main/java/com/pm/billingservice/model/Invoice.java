package com.pm.billingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false)
  private UUID billingAccountId;

  private String description;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal amount;

  private LocalDate invoiceDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private InvoiceStatus status = InvoiceStatus.PENDING;

  public enum InvoiceStatus {
    PENDING, PAID, OVERDUE
  }

  // --- Getters & Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getBillingAccountId() { return billingAccountId; }
  public void setBillingAccountId(UUID billingAccountId) { this.billingAccountId = billingAccountId; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  public LocalDate getInvoiceDate() { return invoiceDate; }
  public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

  public InvoiceStatus getStatus() { return status; }
  public void setStatus(InvoiceStatus status) { this.status = status; }
}
