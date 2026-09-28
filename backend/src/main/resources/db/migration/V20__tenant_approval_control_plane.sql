ALTER TABLE hospitals ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE hospitals ADD COLUMN IF NOT EXISTS approval_reason VARCHAR(500);
ALTER TABLE hospitals ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
UPDATE hospitals SET approval_status = 'APPROVED', approved_at = COALESCE(approved_at, created_at)
WHERE approval_status IS NULL OR approval_status = '';

CREATE TABLE IF NOT EXISTS approval_requests (
    id BIGSERIAL PRIMARY KEY,
    request_type VARCHAR(60) NOT NULL,
    hospital_id BIGINT REFERENCES hospitals(id),
    requested_by VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    details VARCHAR(1000),
    reviewed_by VARCHAR(30),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT approval_request_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS idx_approval_requests_status ON approval_requests(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_approval_requests_hospital ON approval_requests(hospital_id, created_at DESC);
