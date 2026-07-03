package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing Patients")
public class PatientController {

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @GetMapping
  @Operation(summary = "Get Patients")
  public ResponseEntity<List<PatientResponseDTO>> getPatients() {
    List<PatientResponseDTO> patients = patientService.getPatients();
    return ResponseEntity.ok().body(patients);
  }

  @GetMapping("/search")
  @Operation(summary = "Search Patients by name or email")
  public ResponseEntity<List<PatientResponseDTO>> searchPatients(
      @RequestParam String q) {
    List<PatientResponseDTO> patients = patientService.searchPatients(q);
    return ResponseEntity.ok().body(patients);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a Patient by ID")
  public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable UUID id) {
    PatientResponseDTO patient = patientService.getPatient(id);
    return ResponseEntity.ok().body(patient);
  }

  @PostMapping
  @Operation(summary = "Create a new Patient")
  public ResponseEntity<PatientResponseDTO> createPatient(
      @Validated({Default.class, CreatePatientValidationGroup.class})
      @RequestBody PatientRequestDTO patientRequestDTO) {

    PatientResponseDTO patientResponseDTO = patientService.createPatient(
        patientRequestDTO);

    return ResponseEntity.ok().body(patientResponseDTO);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a new Patient")
  public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
      @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO) {

    PatientResponseDTO patientResponseDTO = patientService.updatePatient(id,
        patientRequestDTO);

    return ResponseEntity.ok().body(patientResponseDTO);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a Patient")
  public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
    patientService.deletePatient(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/admit")
  @Operation(summary = "Admit a patient — assign room and bed")
  public ResponseEntity<PatientResponseDTO> admitPatient(
      @PathVariable UUID id,
      @RequestParam String roomNumber,
      @RequestParam String bedNumber) {
    return ResponseEntity.ok(patientService.admitPatient(id, roomNumber, bedNumber));
  }

  @PutMapping("/{id}/discharge")
  @Operation(summary = "Discharge a patient — clear room assignment")
  public ResponseEntity<PatientResponseDTO> dischargePatient(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.dischargePatient(id));
  }

  @GetMapping("/status/{status}")
  @Operation(summary = "Get patients by status (ACTIVE, ADMITTED, DISCHARGED)")
  public ResponseEntity<List<PatientResponseDTO>> getPatientsByStatus(
      @PathVariable String status) {
    return ResponseEntity.ok(patientService.getPatientsByStatus(status));
  }

  @GetMapping("/count")
  @Operation(summary = "Get count of patients by status")
  public ResponseEntity<java.util.Map<String, Long>> getPatientCounts() {
    java.util.Map<String, Long> counts = new java.util.HashMap<>();
    counts.put("total", (long) patientService.getPatients().size());
    counts.put("admitted", patientService.countByStatus("ADMITTED"));
    counts.put("active", patientService.countByStatus("ACTIVE"));
    counts.put("discharged", patientService.countByStatus("DISCHARGED"));
    return ResponseEntity.ok(counts);
  }

  @GetMapping("/{id}/billing")
  @Operation(summary = "Get full billing details for a patient")
  public ResponseEntity<java.util.Map<String, Object>> getBillingDetails(@PathVariable UUID id) {
    return ResponseEntity.ok(patientService.getBillingDetails(id));
  }

  @PostMapping("/{id}/invoices")
  @Operation(summary = "Add a charge/invoice to a patient's billing account")
  public ResponseEntity<java.util.Map<String, Object>> addInvoice(
      @PathVariable UUID id,
      @RequestParam String description,
      @RequestParam double amount) {
    return ResponseEntity.ok(patientService.addInvoice(id, description, amount));
  }

  @PutMapping("/invoices/{invoiceId}/pay")
  @Operation(summary = "Record a payment for an invoice")
  public ResponseEntity<java.util.Map<String, Object>> recordPayment(@PathVariable String invoiceId) {
    return ResponseEntity.ok(patientService.recordPayment(invoiceId));
  }

  @GetMapping("/billing/summary")
  @Operation(summary = "Get outstanding billing summary across all accounts")
  public ResponseEntity<java.util.Map<String, Double>> getOutstandingSummary() {
    return ResponseEntity.ok(java.util.Map.of("totalOutstanding", patientService.getOutstandingSummary()));
  }
}
