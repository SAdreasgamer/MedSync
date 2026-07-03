package com.pm.patientservice.dto;

public class PatientResponseDTO {
  private String id;
  private String name;
  private String email;
  private String address;
  private String dateOfBirth;
  private String registeredDate;
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

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }

  public String getDateOfBirth() { return dateOfBirth; }
  public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

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
