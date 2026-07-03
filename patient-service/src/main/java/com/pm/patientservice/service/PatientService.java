package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.model.Patient.PatientStatus;
import com.pm.patientservice.repository.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

  private final PatientRepository patientRepository;
  private final BillingServiceGrpcClient billingServiceGrpcClient;
  private final KafkaProducer kafkaProducer;

  public PatientService(PatientRepository patientRepository,
      BillingServiceGrpcClient billingServiceGrpcClient,
      KafkaProducer kafkaProducer) {
    this.patientRepository = patientRepository;
    this.billingServiceGrpcClient = billingServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  public List<PatientResponseDTO> getPatients() {
    List<Patient> patients = patientRepository.findAll();
    return patients.stream().map(PatientMapper::toDTO).toList();
  }

  public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
    if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
      throw new EmailAlreadyExistsException(
          "A patient with this email already exists: " + patientRequestDTO.getEmail());
    }

    Patient newPatient = patientRepository.save(
        PatientMapper.toModel(patientRequestDTO));

    billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(),
        newPatient.getName(), newPatient.getEmail());

    kafkaProducer.sendEvent(newPatient);

    return PatientMapper.toDTO(newPatient);
  }

  public PatientResponseDTO updatePatient(UUID id,
      PatientRequestDTO patientRequestDTO) {

    Patient patient = patientRepository.findById(id).orElseThrow(
        () -> new PatientNotFoundException("Patient not found with ID: " + id));

    if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
      throw new EmailAlreadyExistsException(
          "A patient with this email already exists: " + patientRequestDTO.getEmail());
    }

    patient.setName(patientRequestDTO.getName());
    patient.setAddress(patientRequestDTO.getAddress());
    patient.setEmail(patientRequestDTO.getEmail());
    patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

    // Update new fields if provided
    if (patientRequestDTO.getPhone() != null) patient.setPhone(patientRequestDTO.getPhone());
    if (patientRequestDTO.getGender() != null && !patientRequestDTO.getGender().isBlank()) {
      patient.setGender(Patient.Gender.valueOf(patientRequestDTO.getGender().toUpperCase()));
    }
    if (patientRequestDTO.getBloodGroup() != null) patient.setBloodGroup(patientRequestDTO.getBloodGroup());
    if (patientRequestDTO.getEmergencyContactName() != null) patient.setEmergencyContactName(patientRequestDTO.getEmergencyContactName());
    if (patientRequestDTO.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(patientRequestDTO.getEmergencyContactPhone());
    if (patientRequestDTO.getNotes() != null) patient.setNotes(patientRequestDTO.getNotes());

    Patient updatedPatient = patientRepository.save(patient);
    return PatientMapper.toDTO(updatedPatient);
  }

  public void deletePatient(UUID id) {
    patientRepository.deleteById(id);
  }

  public PatientResponseDTO getPatient(UUID id) {
    Patient patient = patientRepository.findById(id).orElseThrow(
        () -> new PatientNotFoundException("Patient not found with ID: " + id));
    return PatientMapper.toDTO(patient);
  }

  public List<PatientResponseDTO> searchPatients(String query) {
    List<Patient> patients = patientRepository
        .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    return patients.stream().map(PatientMapper::toDTO).toList();
  }

  public PatientResponseDTO admitPatient(UUID id, String roomNumber, String bedNumber) {
    Patient patient = patientRepository.findById(id).orElseThrow(
        () -> new PatientNotFoundException("Patient not found with ID: " + id));

    patient.setStatus(PatientStatus.ADMITTED);
    patient.setRoomNumber(roomNumber);
    patient.setBedNumber(bedNumber);
    patient.setAdmissionDate(LocalDate.now());
    patient.setDischargeDate(null);

    Patient saved = patientRepository.save(patient);
    return PatientMapper.toDTO(saved);
  }

  public PatientResponseDTO dischargePatient(UUID id) {
    Patient patient = patientRepository.findById(id).orElseThrow(
        () -> new PatientNotFoundException("Patient not found with ID: " + id));

    patient.setStatus(PatientStatus.DISCHARGED);
    patient.setRoomNumber(null);
    patient.setBedNumber(null);
    patient.setDischargeDate(LocalDate.now());

    Patient saved = patientRepository.save(patient);
    return PatientMapper.toDTO(saved);
  }

  public List<PatientResponseDTO> getPatientsByStatus(String status) {
    PatientStatus ps = PatientStatus.valueOf(status.toUpperCase());
    List<Patient> patients = patientRepository.findByStatus(ps);
    return patients.stream().map(PatientMapper::toDTO).toList();
  }

  public long countByStatus(String status) {
    PatientStatus ps = PatientStatus.valueOf(status.toUpperCase());
    return patientRepository.countByStatus(ps);
  }

  // --- Billing Service Proxy Methods (via gRPC) ---

  public java.util.Map<String, Object> getBillingDetails(UUID patientId) {
    billing.BillingDetailsResponse response = billingServiceGrpcClient.getBillingDetails(patientId.toString());
    
    java.util.List<java.util.Map<String, Object>> invoices = new java.util.ArrayList<>();
    for (billing.InvoiceResponse inv : response.getInvoicesList()) {
      java.util.Map<String, Object> invMap = new java.util.HashMap<>();
      invMap.put("invoiceId", inv.getInvoiceId());
      invMap.put("description", inv.getDescription());
      invMap.put("amount", inv.getAmount());
      invMap.put("status", inv.getStatus());
      invMap.put("invoiceDate", inv.getInvoiceDate());
      invoices.add(invMap);
    }

    java.util.Map<String, Object> details = new java.util.HashMap<>();
    details.put("accountId", response.getAccountId());
    details.put("patientId", response.getPatientId());
    details.put("status", response.getStatus());
    details.put("insuranceProvider", response.getInsuranceProvider());
    details.put("insurancePolicyNumber", response.getInsurancePolicyNumber());
    details.put("totalBilled", response.getTotalBilled());
    details.put("totalPaid", response.getTotalPaid());
    details.put("outstandingBalance", response.getOutstandingBalance());
    details.put("invoices", invoices);

    return details;
  }

  public java.util.Map<String, Object> addInvoice(UUID patientId, String description, double amount) {
    billing.InvoiceResponse response = billingServiceGrpcClient.addInvoice(patientId.toString(), description, amount);
    
    java.util.Map<String, Object> result = new java.util.HashMap<>();
    result.put("invoiceId", response.getInvoiceId());
    result.put("description", response.getDescription());
    result.put("amount", response.getAmount());
    result.put("status", response.getStatus());
    result.put("invoiceDate", response.getInvoiceDate());
    return result;
  }

  public java.util.Map<String, Object> recordPayment(String invoiceId) {
    billing.InvoiceResponse response = billingServiceGrpcClient.recordPayment(invoiceId);
    
    java.util.Map<String, Object> result = new java.util.HashMap<>();
    result.put("invoiceId", response.getInvoiceId());
    result.put("description", response.getDescription());
    result.put("amount", response.getAmount());
    result.put("status", response.getStatus());
    result.put("invoiceDate", response.getInvoiceDate());
    return result;
  }

  public double getOutstandingSummary() {
    return billingServiceGrpcClient.getOutstandingSummary();
  }
}
