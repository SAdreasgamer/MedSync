-- Seed billing accounts
INSERT INTO billing_account (id, patient_id, name, email, status, created_date, insurance_provider, insurance_policy_number, total_billed, total_paid, outstanding_balance)
VALUES 
  ('323e4567-e89b-12d3-a456-426614175000', '123e4567-e89b-12d3-a456-426614174000', 'Rahul Sharma', 'rahul.sharma@example.com', 'ACTIVE', '2024-01-10', 'Star Health', 'POL-12345-2026', 25000.00, 15000.00, 10000.00),
  ('323e4567-e89b-12d3-a456-426614175002', '123e4567-e89b-12d3-a456-426614174002', 'Amit Kumar', 'amit.kumar@example.com', 'ACTIVE', '2022-06-20', 'ICICI Lombard', 'POL-98765-2026', 12000.00, 12000.00, 0.00),
  ('323e4567-e89b-12d3-a456-426614175005', '223e4567-e89b-12d3-a456-426614174005', 'Sneha Reddy', 'sneha.reddy@example.com', 'ACTIVE', '2024-02-15', 'Star Health', 'POL-22233-2026', 8000.00, 0.00, 8000.00),
  ('323e4567-e89b-12d3-a456-426614175010', '223e4567-e89b-12d3-a456-426614174010', 'Suresh Pillai', 'suresh.pillai@example.com', 'ACTIVE', '2023-01-22', 'HDFC Ergo', 'POL-44556-2026', 45000.00, 30000.00, 15000.00)
ON CONFLICT (patient_id) DO NOTHING;

-- Seed invoices
INSERT INTO invoice (id, billing_account_id, description, amount, invoice_date, status)
VALUES
  ('423e4567-e89b-12d3-a456-426614176001', '323e4567-e89b-12d3-a456-426614175000', 'Room Charge - ICU (3 days)', 15000.00, '2026-06-30', 'PAID'),
  ('423e4567-e89b-12d3-a456-426614176002', '323e4567-e89b-12d3-a456-426614175000', 'Lab Work - CBC & ECG', 25000.00, '2026-07-01', 'PAID'),
  ('423e4567-e89b-12d3-a456-426614176003', '323e4567-e89b-12d3-a456-426614175000', 'Consultation Fee - Dr. Sharma', 10000.00, '2026-07-02', 'PENDING'),
  
  ('423e4567-e89b-12d3-a456-426614176004', '323e4567-e89b-12d3-a456-426614175002', 'Dengue Diagnostic Test', 12000.00, '2026-07-01', 'PAID'),
  
  ('423e4567-e89b-12d3-a456-426614176005', '323e4567-e89b-12d3-a456-426614175005', 'Pediatric Consultation', 8000.00, '2026-07-02', 'PENDING'),
  
  ('423e4567-e89b-12d3-a456-426614176006', '323e4567-e89b-12d3-a456-426614175010', 'Angioplasty Procedure', 30000.00, '2026-06-30', 'PAID'),
  ('423e4567-e89b-12d3-a456-426614176007', '323e4567-e89b-12d3-a456-426614175010', 'ICU Room Charge (2 days)', 15000.00, '2026-07-01', 'PENDING')
ON CONFLICT (id) DO NOTHING;
