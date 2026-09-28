ALTER TABLE consultations ADD COLUMN IF NOT EXISTS patient_type VARCHAR(2) NOT NULL DEFAULT 'OP';
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS patient_type VARCHAR(2) NOT NULL DEFAULT 'OP';
ALTER TABLE consultations DROP CONSTRAINT IF EXISTS consultations_patient_type_check;
ALTER TABLE consultations ADD CONSTRAINT consultations_patient_type_check CHECK (patient_type IN ('OP', 'IP'));
ALTER TABLE prescriptions DROP CONSTRAINT IF EXISTS prescriptions_patient_type_check;
ALTER TABLE prescriptions ADD CONSTRAINT prescriptions_patient_type_check CHECK (patient_type IN ('OP', 'IP'));