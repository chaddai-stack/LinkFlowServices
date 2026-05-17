CREATE TABLE IF NOT EXISTS bulk_link_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',
    total_count INTEGER NOT NULL,
    succeeded_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_bulk_link_jobs_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL_FAILED', 'FAILED')),
    CONSTRAINT ck_bulk_link_jobs_counts
        CHECK (total_count >= 0 AND succeeded_count >= 0 AND failed_count >= 0)
);

CREATE TABLE IF NOT EXISTS bulk_link_job_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES bulk_link_jobs(id) ON DELETE CASCADE,
    item_index INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    long_url TEXT NOT NULL,
    title VARCHAR(300),
    custom_back_half VARCHAR(80),
    channel VARCHAR(80),
    url_mapping_id BIGINT REFERENCES url_mapping(id) ON DELETE SET NULL,
    risk_level VARCHAR(20),
    risk_score INTEGER,
    category VARCHAR(40),
    error_code VARCHAR(80),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_bulk_link_job_items_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT uk_bulk_link_job_items_index UNIQUE (job_id, item_index)
);

CREATE INDEX IF NOT EXISTS idx_bulk_link_job_items_job
    ON bulk_link_job_items(job_id, item_index);
