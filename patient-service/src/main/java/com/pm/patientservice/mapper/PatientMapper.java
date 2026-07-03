package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.model.Patient.Gender;
import com.pm.patientservice.model.Patient.PatientStatus;
import java.time.LocalDate;

public class PatientMapper {
  public static PatientResponseDTO toDTO(Patient patient) {
    PatientResponseDTO dto = new PatientResponseDTO();
    dto.setId(patient.getId().toString());
    dto.setName(patient.getName());
    dto.setAddress(patient.getAddress());
    dto.setEmail(patient.getEmail());
    dto.setDateOfBirth(patient.getDateOfBirth().toString());
    dto.setRegisteredDate(patient.getRegisteredDate() != null ? patient.getRegisteredDate().toString() : null);
    dto.setPhone(patient.getPhone());
    dto.setGender(patient.getGender() != null ? patient.getGender().name() : null);
    dto.setBloodGroup(patient.getBloodGroup());
    dto.setEmergencyContactName(patient.getEmergencyContactName());
    dto.setEmergencyContactPhone(patient.getEmergencyContactPhone());
    dto.setStatus(patient.getStatus() != null ? patient.getStatus().name() : "ACTIVE");
    dto.setRoomNumber(patient.getRoomNumber());
    dto.setBedNumber(patient.getBedNumber());
    dto.setAdmissionDate(patient.getAdmissionDate() != null ? patient.getAdmissionDate().toString() : null);
    dto.setDischargeDate(patient.getDischargeDate() != null ? patient.getDischargeDate().toString() : null);
    dto.setNotes(patient.getNotes());
    return dto;
  }

  public static Patient toModel(PatientRequestDTO dto) {
    Patient patient = new Patient();
    patient.setName(dto.getName());
    patient.setAddress(dto.getAddress());
    patient.setEmail(dto.getEmail());
    patient.setDateOfBirth(LocalDate.parse(dto.getDateOfBirth()));
    patient.setRegisteredDate(
        dto.getRegisteredDate() != null ? LocalDate.parse(dto.getRegisteredDate()) : LocalDate.now());
    patient.setPhone(dto.getPhone());
    if (dto.getGender() != null && !dto.getGender().isBlank()) {
      patient.setGender(Gender.valueOf(dto.getGender().toUpperCase()));
    }
    patient.setBloodGroup(dto.getBloodGroup());
    patient.setEmergencyContactName(dto.getEmergencyContactName());
    patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
    if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
      patient.setStatus(PatientStatus.valueOf(dto.getStatus().toUpperCase()));
    } else {
      patient.setStatus(PatientStatus.ACTIVE);
    }
    patient.setRoomNumber(dto.getRoomNumber());
    patient.setBedNumber(dto.getBedNumber());
    if (dto.getAdmissionDate() != null && !dto.getAdmissionDate().isBlank()) {
      patient.setAdmissionDate(LocalDate.parse(dto.getAdmissionDate()));
    }
    if (dto.getDischargeDate() != null && !dto.getDischargeDate().isBlank()) {
      patient.setDischargeDate(LocalDate.parse(dto.getDischargeDate()));
    }
    patient.setNotes(dto.getNotes());
    return patient;
  }
}
