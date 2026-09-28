CREATE TABLE consultations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(patient_id),
    patient_phno VARCHAR(20) NOT NULL REFERENCES patients(phno),
    doctor_phno VARCHAR(20) NOT NULL,
    appointment_id BIGINT REFERENCES appointments(id),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    symptoms TEXT,
    diagnosis TEXT,
    clinical_notes TEXT,
    general_notes TEXT,
    bp VARCHAR(20),
    grbs VARCHAR(20),
    spo2 VARCHAR(20),
    temperature VARCHAR(20),
    next_visit_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalized_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT consultation_status_check CHECK (status IN ('CREATED', 'IN_PROGRESS', 'AWAITING_PAYMENT', 'FINALIZED')),
    CONSTRAINT uq_consultation_appointment UNIQUE (appointment_id)
);
CREATE INDEX idx_consultations_patient_created ON consultations(patient_id, created_at DESC);
CREATE INDEX idx_consultations_doctor_status ON consultations(doctor_phno, status, created_at DESC);
CREATE INDEX idx_consultations_appointment ON consultations(appointment_id);

ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS consultation_id BIGINT REFERENCES consultations(id);
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS diagnosis TEXT;
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS notes TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS uq_prescriptions_consultation ON prescriptions(consultation_id)
    WHERE consultation_id IS NOT NULL;

CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT medication_category_check CHECK (category IN ('TAB', 'CAP', 'SYRUP', 'OINT', 'POWDER', 'INJC', 'OTHERS'))
);
CREATE UNIQUE INDEX uq_medications_name_category ON medications(lower(name), category);
CREATE INDEX idx_medications_search ON medications(lower(name));
INSERT INTO medications(name, category) VALUES
    ('Dolo 650', 'TAB'),
    ('Amoxicillin', 'CAP'),
    ('Augmentin', 'TAB'),
    ('Paracetamol Syrup', 'SYRUP'),
    ('Betnovate', 'OINT')
ON CONFLICT DO NOTHING;

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    patient_phno VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    recipient VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    provider_reference VARCHAR(160),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    next_attempt_at TIMESTAMP,
    CONSTRAINT notification_channel_check CHECK (channel IN ('SMS', 'EMAIL', 'WHATSAPP', 'PUSH')),
    CONSTRAINT notification_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED'))
);
CREATE INDEX idx_notifications_pending ON notifications(status, next_attempt_at, created_at);
CREATE INDEX idx_notifications_patient ON notifications(patient_phno, created_at DESC);
