-- Keep phone as the unique business identifier, but use the internal generated key
-- as the relational/database primary key.
ALTER TABLE prescriptions DROP CONSTRAINT IF EXISTS fk_patient;
ALTER TABLE billing DROP CONSTRAINT IF EXISTS fk_patient;
ALTER TABLE patients DROP CONSTRAINT IF EXISTS patients_pkey;
ALTER TABLE patients DROP CONSTRAINT IF EXISTS uq_patients_patient_id;
ALTER TABLE patients ADD CONSTRAINT patients_pkey PRIMARY KEY (patient_id);
ALTER TABLE patients ALTER COLUMN patient_id SET NOT NULL;
ALTER TABLE prescriptions ADD CONSTRAINT fk_patient FOREIGN KEY (patient_phno)
	REFERENCES patients(phno) ON DELETE CASCADE;
ALTER TABLE billing ADD CONSTRAINT fk_patient FOREIGN KEY (patient_phno)
	REFERENCES patients(phno) ON DELETE CASCADE;
