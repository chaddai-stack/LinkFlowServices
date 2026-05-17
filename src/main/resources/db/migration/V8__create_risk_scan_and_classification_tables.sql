CREATE TABLE IF NOT EXISTS risk_scan_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url_mapping_id BIGINT REFERENCES url_mapping(id) ON DELETE SET NULL,
    long_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    risk_score INTEGER NOT NULL DEFAULT 0,
    reason_codes TEXT NOT NULL DEFAULT '',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    CONSTRAINT ck_risk_scan_tasks_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_risk_scan_tasks_level
        CHECK (risk_level IN ('UNKNOWN', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_risk_scan_tasks_score
        CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_risk_scan_tasks_link_time
    ON risk_scan_tasks(url_mapping_id, created_at DESC);

CREATE TABLE IF NOT EXISTS risk_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url_mapping_id BIGINT REFERENCES url_mapping(id) ON DELETE SET NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_score INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    reason_codes TEXT NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reporter VARCHAR(120) NOT NULL DEFAULT 'rule-risk-engine',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_risk_alerts_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_risk_alerts_status
        CHECK (status IN ('PENDING', 'APPROVED', 'BLOCKED', 'BLACKLISTED')),
    CONSTRAINT ck_risk_alerts_score
        CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_risk_alerts_status_time
    ON risk_alerts(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_risk_alerts_level_time
    ON risk_alerts(risk_level, created_at DESC);

CREATE TABLE IF NOT EXISTS risk_blacklist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(20) NOT NULL,
    value VARCHAR(2048) NOT NULL,
    reason VARCHAR(1000),
    source VARCHAR(40) NOT NULL DEFAULT 'manual',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_risk_blacklist_entries_type
        CHECK (type IN ('DOMAIN', 'URL')),
    CONSTRAINT uk_risk_blacklist_type_value UNIQUE (type, value)
);

CREATE TABLE IF NOT EXISTS link_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url_mapping_id BIGINT NOT NULL REFERENCES url_mapping(id) ON DELETE CASCADE,
    category VARCHAR(40) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    labels TEXT NOT NULL DEFAULT '',
    source VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_link_classifications_link UNIQUE (url_mapping_id),
    CONSTRAINT ck_link_classifications_confidence
        CHECK (confidence >= 0 AND confidence <= 1)
);
