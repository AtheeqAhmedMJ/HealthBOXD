CREATE TABLE IF NOT EXISTS service_fee_brackets (
    id BIGSERIAL PRIMARY KEY,
    min_amount_paise BIGINT NOT NULL,
    max_amount_paise BIGINT,
    fee_type VARCHAR(20) NOT NULL,
    fee_value BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT service_fee_type_check CHECK (fee_type IN ('FIXED', 'PERCENTAGE')),
    CONSTRAINT service_fee_range_check CHECK (max_amount_paise IS NULL OR max_amount_paise >= min_amount_paise),
    CONSTRAINT service_fee_value_check CHECK (fee_value >= 0)
);
CREATE INDEX IF NOT EXISTS idx_service_fee_brackets_active_min ON service_fee_brackets(active, min_amount_paise);