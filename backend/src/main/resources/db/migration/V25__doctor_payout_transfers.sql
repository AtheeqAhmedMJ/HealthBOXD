ALTER TABLE users ADD COLUMN IF NOT EXISTS razorpay_account_id VARCHAR(40);
ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS transfer_id VARCHAR(100);
ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS transfer_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS transfer_error VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_payment_orders_transfer_status ON payment_orders(transfer_status, created_at DESC);