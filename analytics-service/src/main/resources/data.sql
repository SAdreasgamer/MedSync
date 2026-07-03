INSERT INTO patient_event_record (id, patient_id, patient_name, patient_email, event_type, timestamp)
VALUES
  ('a23e4567-e89b-12d3-a456-426614177001', '123e4567-e89b-12d3-a456-426614174000', 'Rahul Sharma', 'rahul.sharma@example.com', 'PATIENT_CREATED', '2026-06-28 09:30:00'),
  ('a23e4567-e89b-12d3-a456-426614177002', '123e4567-e89b-12d3-a456-426614174001', 'Ananya Patel', 'ananya.patel@example.com', 'PATIENT_CREATED', '2026-06-29 10:15:00'),
  ('a23e4567-e89b-12d3-a456-426614177003', '123e4567-e89b-12d3-a456-426614174002', 'Amit Kumar', 'amit.kumar@example.com', 'PATIENT_CREATED', '2026-06-30 14:00:00'),
  ('a23e4567-e89b-12d3-a456-426614177004', '223e4567-e89b-12d3-a456-426614174005', 'Sneha Reddy', 'sneha.reddy@example.com', 'PATIENT_CREATED', '2026-07-01 11:45:00'),
  ('a23e4567-e89b-12d3-a456-426614177005', '223e4567-e89b-12d3-a456-426614174006', 'Vikrant Singh', 'vikrant.singh@example.com', 'PATIENT_CREATED', '2026-07-02 08:00:00'),
  ('a23e4567-e89b-12d3-a456-426614177006', '223e4567-e89b-12d3-a456-426614174010', 'Suresh Pillai', 'suresh.pillai@example.com', 'PATIENT_CREATED', '2026-07-03 16:20:00')
ON CONFLICT (id) DO NOTHING;
