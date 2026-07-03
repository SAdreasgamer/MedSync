package com.pm.analyticsservice.controller;

import com.pm.analyticsservice.model.PatientEventRecord;
import com.pm.analyticsservice.repository.PatientEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

  private final PatientEventRepository repository;

  public AnalyticsController(PatientEventRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/patient-count")
  public ResponseEntity<Map<String, Long>> getPatientCount() {
    long totalCreated = repository.countByEventType("PATIENT_CREATED");
    return ResponseEntity.ok(Map.of("totalPatients", totalCreated));
  }

  @GetMapping("/registrations")
  public ResponseEntity<Map<String, Long>> getRegistrations(
      @RequestParam(defaultValue = "month") String period) {

    LocalDateTime since;
    switch (period) {
      case "week" -> since = LocalDateTime.now().minusWeeks(1);
      case "year" -> since = LocalDateTime.now().minusYears(1);
      default -> since = LocalDateTime.now().minusMonths(1);
    }

    long count = repository.countByTimestampAfter(since);
    return ResponseEntity.ok(Map.of("registrations", count, "periodDays",
        (long) java.time.Duration.between(since, LocalDateTime.now())
            .toDays()));
  }

  @GetMapping("/recent-events")
  public ResponseEntity<List<PatientEventRecord>> getRecentEvents() {
    return ResponseEntity.ok(
        repository.findTop20ByOrderByTimestampDesc());
  }
}
