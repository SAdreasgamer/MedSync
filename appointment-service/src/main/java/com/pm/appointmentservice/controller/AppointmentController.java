package com.pm.appointmentservice.controller;

import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.service.AppointmentService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;

  public AppointmentController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  @GetMapping
  public ResponseEntity<List<Appointment>> getAllAppointments() {
    return ResponseEntity.ok(appointmentService.getAllAppointments());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Appointment> getAppointment(@PathVariable UUID id) {
    return ResponseEntity.ok(appointmentService.getAppointment(id));
  }

  @GetMapping("/patient/{patientId}")
  public ResponseEntity<List<Appointment>> getByPatient(@PathVariable UUID patientId) {
    return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
  }

  @GetMapping("/date")
  public ResponseEntity<List<Appointment>> getByDate(@RequestParam String date) {
    return ResponseEntity.ok(appointmentService.getAppointmentsByDate(LocalDate.parse(date)));
  }

  @GetMapping("/today")
  public ResponseEntity<List<Appointment>> getTodayAppointments() {
    return ResponseEntity.ok(appointmentService.getAppointmentsByDate(LocalDate.now()));
  }

  @GetMapping("/count/today")
  public ResponseEntity<Map<String, Long>> countToday() {
    return ResponseEntity.ok(Map.of("count", appointmentService.countTodayAppointments()));
  }

  @PostMapping
  public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment) {
    return ResponseEntity.ok(appointmentService.createAppointment(appointment));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Appointment> updateAppointment(
      @PathVariable UUID id, @RequestBody Appointment appointment) {
    return ResponseEntity.ok(appointmentService.updateAppointment(id, appointment));
  }

  @PutMapping("/{id}/status")
  public ResponseEntity<Appointment> updateStatus(
      @PathVariable UUID id, @RequestParam String status) {
    return ResponseEntity.ok(appointmentService.updateStatus(id, status));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAppointment(@PathVariable UUID id) {
    appointmentService.deleteAppointment(id);
    return ResponseEntity.noContent().build();
  }
}
