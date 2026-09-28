ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS consultation_id BIGINT REFERENCES consultations(id);
ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS prescription_id BIGINT REFERENCES prescriptions(id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_consultation ON payment_orders(consultation_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_prescription ON payment_orders(prescription_id);
