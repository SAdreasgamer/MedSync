package com.pm.billingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class BillingAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(unique = true, nullable = false)
  private String patientId;

  private String name;

  private String email;

  private String status;

  private LocalDate createdDate;

  // New fields
  private String insuranceProvider;

  private String insurancePolicyNumber;

  @Column(precision = 12, scale = 2)
  private BigDecimal totalBilled = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2)
  private BigDecimal totalPaid = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2)
  private BigDecimal outstandingBalance = BigDecimal.ZERO;

  // --- Getters & Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public LocalDate getCreatedDate() { return createdDate; }
  public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

  public String getInsuranceProvider() { return insuranceProvider; }
  public void setInsuranceProvider(String insuranceProvider) { this.insuranceProvider = insuranceProvider; }

  public String getInsurancePolicyNumber() { return insurancePolicyNumber; }
  public void setInsurancePolicyNumber(String insurancePolicyNumber) { this.insurancePolicyNumber = insurancePolicyNumber; }

  public BigDecimal getTotalBilled() { return totalBilled; }
  public void setTotalBilled(BigDecimal totalBilled) { this.totalBilled = totalBilled; }

  public BigDecimal getTotalPaid() { return totalPaid; }
  public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }

  public BigDecimal getOutstandingBalance() { return outstandingBalance; }
  public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
}
