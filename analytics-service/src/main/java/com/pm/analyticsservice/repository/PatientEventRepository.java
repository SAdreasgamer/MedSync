package com.pm.analyticsservice.repository;

import com.pm.analyticsservice.model.PatientEventRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientEventRepository
    extends JpaRepository<PatientEventRecord, UUID> {

  long countByEventType(String eventType);

  long countByTimestampAfter(LocalDateTime after);

  List<PatientEventRecord> findTop20ByOrderByTimestampDesc();
}
