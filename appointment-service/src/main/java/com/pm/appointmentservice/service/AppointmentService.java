package com.pm.appointmentservice.service;

import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.Appointment.AppointmentStatus;
import com.pm.appointmentservice.repository.AppointmentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;

  public AppointmentService(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  public List<Appointment> getAllAppointments() {
    return appointmentRepository.findAll();
  }

  public Appointment getAppointment(UUID id) {
    return appointmentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
  }

  public List<Appointment> getAppointmentsByPatient(UUID patientId) {
    return appointmentRepository.findByPatientId(patientId);
  }

  public List<Appointment> getAppointmentsByDate(LocalDate date) {
    return appointmentRepository.findByAppointmentDate(date);
  }

  public Appointment createAppointment(Appointment appointment) {
    if (appointment.getStatus() == null) {
      appointment.setStatus(AppointmentStatus.SCHEDULED);
    }
    return appointmentRepository.save(appointment);
  }

  public Appointment updateStatus(UUID id, String status) {
    Appointment appointment = getAppointment(id);
    appointment.setStatus(AppointmentStatus.valueOf(status.toUpperCase()));
    return appointmentRepository.save(appointment);
  }

  public Appointment updateAppointment(UUID id, Appointment updated) {
    Appointment appointment = getAppointment(id);
    if (updated.getDoctorName() != null) appointment.setDoctorName(updated.getDoctorName());
    if (updated.getDepartment() != null) appointment.setDepartment(updated.getDepartment());
    if (updated.getAppointmentDate() != null) appointment.setAppointmentDate(updated.getAppointmentDate());
    if (updated.getTimeSlot() != null) appointment.setTimeSlot(updated.getTimeSlot());
    if (updated.getStatus() != null) appointment.setStatus(updated.getStatus());
    if (updated.getNotes() != null) appointment.setNotes(updated.getNotes());
    return appointmentRepository.save(appointment);
  }

  public void deleteAppointment(UUID id) {
    appointmentRepository.deleteById(id);
  }

  public long countTodayAppointments() {
    return appointmentRepository.countByAppointmentDate(LocalDate.now());
  }
}
