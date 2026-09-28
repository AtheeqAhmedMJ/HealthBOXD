WITH ranked_phone AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY phone_number, purpose ORDER BY created_at DESC, id DESC) AS rank
    FROM otp_tokens WHERE phone_number IS NOT NULL AND consumed = FALSE
)
UPDATE otp_tokens SET consumed = TRUE WHERE id IN (SELECT id FROM ranked_phone WHERE rank > 1);

WITH ranked_email AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY email, purpose ORDER BY created_at DESC, id DESC) AS rank
    FROM otp_tokens WHERE phone_number IS NULL AND consumed = FALSE
)
UPDATE otp_tokens SET consumed = TRUE WHERE id IN (SELECT id FROM ranked_email WHERE rank > 1);

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_phone_otp
    ON otp_tokens(phone_number, purpose) WHERE phone_number IS NOT NULL AND consumed = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_email_otp
    ON otp_tokens(email, purpose) WHERE phone_number IS NULL AND consumed = FALSE;
