-- Establish durable workflow states and database-level invariants while retaining
-- legacy columns for rolling application upgrades.

ALTER TABLE patients ADD COLUMN IF NOT EXISTS patient_id BIGSERIAL;
ALTER TABLE patients ADD CONSTRAINT uq_patients_patient_id UNIQUE (patient_id);
ALTER TABLE patients ADD CONSTRAINT uq_patients_phone UNIQUE (phno);
CREATE INDEX IF NOT EXISTS idx_patients_phone ON patients(phno);
CREATE INDEX IF NOT EXISTS idx_patients_created_at ON patients(created_at);

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS appointment_type VARCHAR(20) NOT NULL DEFAULT 'PRE_BOOKED';
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE appointments SET status = 'BOOKED' WHERE status = 'SCHEDULED';
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check
    CHECK (status IN ('BOOKED', 'CHECKED_IN', 'IN_CONSULTATION', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_type_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_type_check
    CHECK (appointment_type IN ('PRE_BOOKED', 'WALK_IN'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_idempotency_key
    ON appointments(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_patient_day
    ON appointments(hospital_id, patient_phno, date)
    WHERE status NOT IN ('CANCELLED', 'NO_SHOW');
CREATE INDEX IF NOT EXISTS idx_appointments_queue
    ON appointments(hospital_id, date, status, doctor_phno);

ALTER TABLE otp_tokens ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
ALTER TABLE otp_tokens ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0;
ALTER TABLE otp_tokens ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP;
ALTER TABLE otp_tokens ADD COLUMN IF NOT EXISTS channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL';
CREATE INDEX IF NOT EXISTS idx_otp_phone_purpose ON otp_tokens(phone_number, purpose, consumed, expires_at);

ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255);
ALTER TABLE payment_orders DROP CONSTRAINT IF EXISTS payment_orders_status_check;
ALTER TABLE payment_orders ADD CONSTRAINT payment_orders_status_check
    CHECK (status IN ('CREATED', 'PENDING', 'SUCCESS', 'FAILED', 'REFUNDED', 'CANCELLED', 'PAID'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_idempotency_key
    ON payment_orders(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_payment_id
    ON payment_orders(razorpay_payment_id) WHERE razorpay_payment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payment_orders_provider_status
    ON payment_orders(razorpay_order_id, status);

ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'FINALIZED';
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS finalized_at TIMESTAMP;
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS next_visit_date DATE;
ALTER TABLE prescriptions DROP CONSTRAINT IF EXISTS prescriptions_status_check;
ALTER TABLE prescriptions ADD CONSTRAINT prescriptions_status_check
    CHECK (status IN ('DRAFT', 'PAYMENT_PENDING', 'PAYMENT_SUCCESS', 'FINALIZED'));
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient_created
    ON prescriptions(patient_phno, created_at DESC);

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    actor_phno VARCHAR(20),
    patient_phno VARCHAR(20),
    resource_type VARCHAR(40),
    resource_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_events_resource ON audit_events(resource_type, resource_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at DESC);

CREATE TABLE IF NOT EXISTS prescription_medications (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    medication_type VARCHAR(20) NOT NULL,
    medication_name VARCHAR(150) NOT NULL,
    dosage VARCHAR(50) NOT NULL,
    quantity VARCHAR(30) NOT NULL,
    duration VARCHAR(50) NOT NULL,
    food_instruction VARCHAR(50),
    additional_instructions VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT prescription_medication_type_check
        CHECK (medication_type IN ('TAB', 'CAP', 'SYRUP', 'OINT', 'POWDER', 'INJC', 'OTHERS'))
);
CREATE INDEX IF NOT EXISTS idx_prescription_medications_prescription
    ON prescription_medications(prescription_id);
