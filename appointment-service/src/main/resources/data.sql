CREATE TABLE IF NOT EXISTS appointment
(
    id               UUID PRIMARY KEY,
    patient_id       UUID         NOT NULL,
    doctor_name      VARCHAR(255) NOT NULL,
    department       VARCHAR(100) NOT NULL,
    appointment_date DATE         NOT NULL,
    time_slot        VARCHAR(50)  NOT NULL,
    status           VARCHAR(20) DEFAULT 'SCHEDULED',
    notes            TEXT
);

-- Appointment 1: Rahul Sharma — post-surgery follow-up
INSERT INTO appointment (id, patient_id, doctor_name, department, appointment_date, time_slot, status, notes)
SELECT '333e4567-e89b-12d3-a456-426614174001', '123e4567-e89b-12d3-a456-426614174000',
       'Dr. Anil Kapoor', 'General Surgery', '2026-07-05', '10:00 AM - 10:30 AM', 'SCHEDULED', 'Post-surgery follow-up'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = '333e4567-e89b-12d3-a456-426614174001');

-- Appointment 2: Ananya Patel — annual checkup
INSERT INTO appointment (id, patient_id, doctor_name, department, appointment_date, time_slot, status, notes)
SELECT '333e4567-e89b-12d3-a456-426614174002', '123e4567-e89b-12d3-a456-426614174001',
       'Dr. Meena Shah', 'General Medicine', '2026-07-04', '11:00 AM - 11:30 AM', 'SCHEDULED', 'Annual health checkup'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = '333e4567-e89b-12d3-a456-426614174002');

-- Appointment 3: Suresh Pillai — cardiology
INSERT INTO appointment (id, patient_id, doctor_name, department, appointment_date, time_slot, status, notes)
SELECT '333e4567-e89b-12d3-a456-426614174003', '223e4567-e89b-12d3-a456-426614174010',
       'Dr. Rajiv Mehta', 'Cardiology', '2026-07-06', '09:00 AM - 09:30 AM', 'SCHEDULED', 'Echocardiogram review'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = '333e4567-e89b-12d3-a456-426614174003');

-- Appointment 4: Meera Iyer — completed
INSERT INTO appointment (id, patient_id, doctor_name, department, appointment_date, time_slot, status, notes)
SELECT '333e4567-e89b-12d3-a456-426614174004', '123e4567-e89b-12d3-a456-426614174003',
       'Dr. Anil Kapoor', 'General Surgery', '2026-06-20', '02:00 PM - 02:30 PM', 'COMPLETED', 'Pre-discharge assessment completed'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = '333e4567-e89b-12d3-a456-426614174004');

-- Appointment 5: Deepak Verma — orthopedics
INSERT INTO appointment (id, patient_id, doctor_name, department, appointment_date, time_slot, status, notes)
SELECT '333e4567-e89b-12d3-a456-426614174005', '123e4567-e89b-12d3-a456-426614174004',
       'Dr. Priya Nair', 'Orthopedics', '2026-07-08', '03:00 PM - 03:30 PM', 'SCHEDULED', 'Knee pain consultation'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = '333e4567-e89b-12d3-a456-426614174005');

-- Appointment 6: Arjun Mehta — dermatology
INSERT INTO appointment (id, patient_id, doctor_name, department, appointment_date, time_slot, status, notes)
SELECT '333e4567-e89b-12d3-a456-426614174006', '223e4567-e89b-12d3-a456-426614174008',
       'Dr. Suman Roy', 'Dermatology', '2026-07-04', '04:00 PM - 04:30 PM', 'SCHEDULED', 'Skin allergy follow-up'
WHERE NOT EXISTS (SELECT 1 FROM appointment WHERE id = '333e4567-e89b-12d3-a456-426614174006');
