-- Ensure the 'patient' table has the new columns
CREATE TABLE IF NOT EXISTS patient
(
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(255)        NOT NULL,
    email                   VARCHAR(255) UNIQUE NOT NULL,
    address                 VARCHAR(255)        NOT NULL,
    date_of_birth           DATE                NOT NULL,
    registered_date         DATE                NOT NULL,
    phone                   VARCHAR(20),
    gender                  VARCHAR(10),
    blood_group             VARCHAR(5),
    emergency_contact_name  VARCHAR(255),
    emergency_contact_phone VARCHAR(20),
    status                  VARCHAR(20) DEFAULT 'ACTIVE',
    room_number             VARCHAR(20),
    bed_number              VARCHAR(10),
    admission_date          DATE,
    discharge_date          DATE,
    notes                   TEXT
);

-- ========== SEED PATIENTS ==========

-- Patient 1: ADMITTED in ICU
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, room_number, bed_number, admission_date, notes)
SELECT '123e4567-e89b-12d3-a456-426614174000', 'Rahul Sharma', 'rahul.sharma@example.com', '45 MG Road, Bangalore', '1985-06-15', '2024-01-10',
       '+91-9876543210', 'MALE', 'O+', 'Priya Sharma', '+91-9123456789', 'ADMITTED', 'ICU-201', 'A', '2026-06-28', 'Post-surgery observation. Vitals stable.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '123e4567-e89b-12d3-a456-426614174000');

-- Patient 2: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, notes)
SELECT '123e4567-e89b-12d3-a456-426614174001', 'Ananya Patel', 'ananya.patel@example.com', '12 Jubilee Hills, Hyderabad', '1990-09-23', '2023-12-01',
       '+91-9988776655', 'FEMALE', 'A+', 'Vikram Patel', '+91-9001234567', 'ACTIVE', 'Regular checkup patient. No known allergies.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '123e4567-e89b-12d3-a456-426614174001');

-- Patient 3: ADMITTED in General Ward
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, room_number, bed_number, admission_date, notes)
SELECT '123e4567-e89b-12d3-a456-426614174002', 'Amit Kumar', 'amit.kumar@example.com', '78 Connaught Place, Delhi', '1978-03-12', '2022-06-20',
       '+91-9112233445', 'MALE', 'B+', 'Sunita Kumar', '+91-9556677889', 'ADMITTED', 'GEN-305', 'B', '2026-07-01', 'Admitted for dengue treatment. Platelet count being monitored.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '123e4567-e89b-12d3-a456-426614174002');

-- Patient 4: DISCHARGED
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, discharge_date, notes)
SELECT '123e4567-e89b-12d3-a456-426614174003', 'Meera Iyer', 'meera.iyer@example.com', '23 Anna Nagar, Chennai', '1982-11-30', '2023-05-14',
       '+91-9334455667', 'FEMALE', 'AB+', 'Rajan Iyer', '+91-9778899001', 'DISCHARGED', '2026-06-25', 'Discharged after appendectomy. Follow-up in 2 weeks.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '123e4567-e89b-12d3-a456-426614174003');

-- Patient 5: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '123e4567-e89b-12d3-a456-426614174004', 'Deepak Verma', 'deepak.verma@example.com', '56 Banjara Hills, Hyderabad', '1995-02-05', '2024-03-01',
       '+91-9445566778', 'MALE', 'O-', 'Kavita Verma', '+91-9223344556', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '123e4567-e89b-12d3-a456-426614174004');

-- Patient 6: ADMITTED in Pediatrics
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, room_number, bed_number, admission_date, notes)
SELECT '223e4567-e89b-12d3-a456-426614174005', 'Sneha Reddy', 'sneha.reddy@example.com', '90 Koramangala, Bangalore', '2015-07-25', '2024-02-15',
       '+91-9667788990', 'FEMALE', 'A-', 'Ramesh Reddy', '+91-9001122334', 'ADMITTED', 'PED-101', 'A', '2026-07-02', 'Pediatric patient. Admitted for high fever and dehydration.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174005');

-- Patient 7: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '223e4567-e89b-12d3-a456-426614174006', 'Vikrant Singh', 'vikrant.singh@example.com', '34 Sector 15, Gurgaon', '1992-04-18', '2023-08-25',
       '+91-9556677001', 'MALE', 'B-', 'Harpreet Singh', '+91-9887766554', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174006');

-- Patient 8: DISCHARGED
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, discharge_date, notes)
SELECT '223e4567-e89b-12d3-a456-426614174007', 'Lakshmi Nair', 'lakshmi.nair@example.com', '67 Marine Drive, Mumbai', '1975-01-11', '2022-10-10',
       '+91-9001234568', 'FEMALE', 'AB-', 'Gopal Nair', '+91-9112345678', 'DISCHARGED', '2026-06-20', 'Knee replacement surgery completed. Physiotherapy recommended.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174007');

-- Patient 9: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '223e4567-e89b-12d3-a456-426614174008', 'Arjun Mehta', 'arjun.mehta@example.com', '12 Park Street, Kolkata', '1989-09-02', '2024-04-20',
       '+91-9778899002', 'MALE', 'A+', 'Neeta Mehta', '+91-9334455001', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174008');

-- Patient 10: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '223e4567-e89b-12d3-a456-426614174009', 'Pooja Gupta', 'pooja.gupta@example.com', '89 Civil Lines, Jaipur', '1993-11-15', '2023-06-30',
       '+91-9445566001', 'FEMALE', 'O+', 'Rajesh Gupta', '+91-9667788001', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174009');

-- Patient 11: ADMITTED in Cardiology
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, room_number, bed_number, admission_date, notes)
SELECT '223e4567-e89b-12d3-a456-426614174010', 'Suresh Pillai', 'suresh.pillai@example.com', '45 Ernakulam, Kochi', '1960-08-09', '2023-01-22',
       '+91-9223344001', 'MALE', 'B+', 'Geetha Pillai', '+91-9556677002', 'ADMITTED', 'CAR-102', 'A', '2026-06-30', 'Cardiac monitoring post-angioplasty. Stable condition.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174010');

-- Patient 12: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '223e4567-e89b-12d3-a456-426614174011', 'Divya Joshi', 'divya.joshi@example.com', '23 FC Road, Pune', '1984-05-03', '2024-05-12',
       '+91-9112233001', 'FEMALE', 'A-', 'Manish Joshi', '+91-9445566002', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174011');

-- Patient 13: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '223e4567-e89b-12d3-a456-426614174012', 'Karan Malhotra', 'karan.malhotra@example.com', '78 GS Road, Guwahati', '1991-12-25', '2022-11-11',
       '+91-9667788002', 'MALE', 'O+', 'Rekha Malhotra', '+91-9778899003', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174012');

-- Patient 14: DISCHARGED
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status, discharge_date, notes)
SELECT '223e4567-e89b-12d3-a456-426614174013', 'Nisha Agarwal', 'nisha.agarwal@example.com', '56 Hazratganj, Lucknow', '1976-06-08', '2023-09-19',
       '+91-9334455002', 'FEMALE', 'AB+', 'Sanjay Agarwal', '+91-9001234569', 'DISCHARGED', '2026-06-15', 'Discharged after cataract surgery. Vision improving.'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174013');

-- Patient 15: ACTIVE
INSERT INTO patient (id, name, email, address, date_of_birth, registered_date, phone, gender, blood_group, emergency_contact_name, emergency_contact_phone, status)
SELECT '223e4567-e89b-12d3-a456-426614174014', 'Rohan Das', 'rohan.das@example.com', '34 Salt Lake, Kolkata', '1987-10-17', '2024-03-29',
       '+91-9889900112', 'MALE', 'B-', 'Anita Das', '+91-9223344002', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE id = '223e4567-e89b-12d3-a456-426614174014');
