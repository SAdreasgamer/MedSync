package com.pm.patientservice.dto;

import com.pm.patientservice.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PatientRequestDTO {

  @NotBlank(message = "Name is required")
  @Size(max = 100, message = "Name cannot exceed 100 characters")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "Address is required")
  private String address;

  @NotBlank(message = "Date of birth is required")
  private String dateOfBirth;

  @NotBlank(groups = CreatePatientValidationGroup.class, message =
      "Registered date is required")
  private String registeredDate;

  // New optional fields
  private String phone;
  private String gender;
  private String bloodGroup;
  private String emergencyContactName;
  private String emergencyContactPhone;
  private String status;
  private String roomNumber;
  private String bedNumber;
  private String admissionDate;
  private String dischargeDate;
  private String notes;

  // --- Getters & Setters ---

  public @NotBlank(message = "Name is required") @Size(max = 100, message = "Name cannot exceed 100 characters") String getName() {
    return name;
  }

  public void setName(
      @NotBlank(message = "Name is required") @Size(max = 100, message = "Name cannot exceed 100 characters") String name) {
    this.name = name;
  }

  public @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String getEmail() {
    return email;
  }

  public void setEmail(
      @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
    this.email = email;
  }

  public @NotBlank(message = "Address is required") String getAddress() {
    return address;
  }

  public void setAddress(
      @NotBlank(message = "Address is required") String address) {
    this.address = address;
  }

  public @NotBlank(message = "Date of birth is required") String getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(
      @NotBlank(message = "Date of birth is required") String dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getRegisteredDate() { return registeredDate; }
  public void setRegisteredDate(String registeredDate) { this.registeredDate = registeredDate; }

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }

  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }

  public String getBloodGroup() { return bloodGroup; }
  public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

  public String getEmergencyContactName() { return emergencyContactName; }
  public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

  public String getEmergencyContactPhone() { return emergencyContactPhone; }
  public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getRoomNumber() { return roomNumber; }
  public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

  public String getBedNumber() { return bedNumber; }
  public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

  public String getAdmissionDate() { return admissionDate; }
  public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }

  public String getDischargeDate() { return dischargeDate; }
  public void setDischargeDate(String dischargeDate) { this.dischargeDate = dischargeDate; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
