CREATE TABLE IF NOT EXISTS doctor_clinic_memberships (
    id BIGSERIAL PRIMARY KEY,
    doctor_phno VARCHAR(30) NOT NULL,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_doctor_clinic_membership UNIQUE (doctor_phno, hospital_id)
);
CREATE INDEX IF NOT EXISTS idx_doctor_clinic_memberships_doctor ON doctor_clinic_memberships(doctor_phno, active);
INSERT INTO doctor_clinic_memberships (doctor_phno, hospital_id)
SELECT phno, hospital_id FROM users
WHERE role = 'ADMIN' AND hospital_id IS NOT NULL
ON CONFLICT (doctor_phno, hospital_id) DO NOTHING;

ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check
CHECK (status IN ('BOOKED', 'CHECKED_IN', 'IN_CONSULTATION', 'COMPLETED', 'CANCELLED', 'NO_SHOW', 'PAUSED'));
