ALTER TABLE consultations ADD COLUMN IF NOT EXISTS inpatient_details JSONB;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS injections JSONB;
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS inpatient_details JSONB;
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS injections JSONB;