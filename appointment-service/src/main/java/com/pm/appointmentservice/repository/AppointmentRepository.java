package com.pm.appointmentservice.repository;

import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.Appointment.AppointmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
  List<Appointment> findByPatientId(UUID patientId);
  List<Appointment> findByAppointmentDate(LocalDate date);
  List<Appointment> findByStatus(AppointmentStatus status);
  long countByAppointmentDate(LocalDate date);
}
