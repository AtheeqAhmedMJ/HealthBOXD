ALTER TABLE appointments ADD COLUMN IF NOT EXISTS appointment_time TIME;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS reason VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_appointments_date_time ON appointments(hospital_id, date, appointment_time, status);
