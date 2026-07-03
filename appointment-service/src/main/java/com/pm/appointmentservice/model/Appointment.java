package com.pm.appointmentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @NotNull
  @Column(nullable = false)
  private UUID patientId;

  @NotNull
  private String doctorName;

  @NotNull
  private String department;

  @NotNull
  private LocalDate appointmentDate;

  @NotNull
  private String timeSlot;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private AppointmentStatus status = AppointmentStatus.SCHEDULED;

  @Column(columnDefinition = "TEXT")
  private String notes;

  public enum AppointmentStatus {
    SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
  }

  // --- Getters & Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getPatientId() { return patientId; }
  public void setPatientId(UUID patientId) { this.patientId = patientId; }

  public String getDoctorName() { return doctorName; }
  public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

  public String getDepartment() { return department; }
  public void setDepartment(String department) { this.department = department; }

  public LocalDate getAppointmentDate() { return appointmentDate; }
  public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

  public String getTimeSlot() { return timeSlot; }
  public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

  public AppointmentStatus getStatus() { return status; }
  public void setStatus(AppointmentStatus status) { this.status = status; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
