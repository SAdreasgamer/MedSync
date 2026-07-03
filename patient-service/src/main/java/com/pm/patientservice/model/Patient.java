package com.pm.patientservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Patient {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @NotNull
  private String name;

  @NotNull
  @Email
  @Column(unique = true)
  private String email;

  @NotNull
  private String address;

  @NotNull
  private LocalDate dateOfBirth;

  @NotNull
  private LocalDate registeredDate;

  // --- New fields ---

  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Gender gender;

  @Column(length = 5)
  private String bloodGroup;

  private String emergencyContactName;

  private String emergencyContactPhone;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private PatientStatus status;

  private String roomNumber;

  private String bedNumber;

  private LocalDate admissionDate;

  private LocalDate dischargeDate;

  @Column(columnDefinition = "TEXT")
  private String notes;

  // --- Enums ---

  public enum Gender {
    MALE, FEMALE, OTHER
  }

  public enum PatientStatus {
    ACTIVE, ADMITTED, DISCHARGED
  }

  // --- Getters & Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public @NotNull String getName() { return name; }
  public void setName(@NotNull String name) { this.name = name; }

  public @NotNull @Email String getEmail() { return email; }
  public void setEmail(@NotNull @Email String email) { this.email = email; }

  public @NotNull String getAddress() { return address; }
  public void setAddress(@NotNull String address) { this.address = address; }

  public @NotNull LocalDate getDateOfBirth() { return dateOfBirth; }
  public void setDateOfBirth(@NotNull LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

  public @NotNull LocalDate getRegisteredDate() { return registeredDate; }
  public void setRegisteredDate(@NotNull LocalDate registeredDate) { this.registeredDate = registeredDate; }

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }

  public Gender getGender() { return gender; }
  public void setGender(Gender gender) { this.gender = gender; }

  public String getBloodGroup() { return bloodGroup; }
  public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

  public String getEmergencyContactName() { return emergencyContactName; }
  public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

  public String getEmergencyContactPhone() { return emergencyContactPhone; }
  public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

  public PatientStatus getStatus() { return status; }
  public void setStatus(PatientStatus status) { this.status = status; }

  public String getRoomNumber() { return roomNumber; }
  public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

  public String getBedNumber() { return bedNumber; }
  public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

  public LocalDate getAdmissionDate() { return admissionDate; }
  public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

  public LocalDate getDischargeDate() { return dischargeDate; }
  public void setDischargeDate(LocalDate dischargeDate) { this.dischargeDate = dischargeDate; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
