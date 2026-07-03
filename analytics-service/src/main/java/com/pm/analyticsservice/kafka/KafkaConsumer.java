package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.analyticsservice.model.PatientEventRecord;
import com.pm.analyticsservice.repository.PatientEventRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(
      KafkaConsumer.class);

  private final PatientEventRepository repository;

  public KafkaConsumer(PatientEventRepository repository) {
    this.repository = repository;
  }

  @KafkaListener(topics = "patient", groupId = "analytics-service")
  public void consumeEvent(byte[] event) {
    try {
      PatientEvent patientEvent = PatientEvent.parseFrom(event);
      // ... perform any business related to analytics here

      log.info(
          "Received Patient Event: [PatientId={},PatientName={},PatientEmail={}]",
          patientEvent.getPatientId(),
          patientEvent.getName(),
          patientEvent.getEmail());

      // Persist event to database for analytics queries
      PatientEventRecord record = new PatientEventRecord();
      record.setPatientId(patientEvent.getPatientId());
      record.setPatientName(patientEvent.getName());
      record.setPatientEmail(patientEvent.getEmail());
      record.setEventType(patientEvent.getEventType());
      record.setTimestamp(LocalDateTime.now());
      repository.save(record);

      log.info("Patient event persisted to analytics database");

    } catch (InvalidProtocolBufferException e) {
      log.error("Error deserializing event {}", e.getMessage());
    }
  }
}
