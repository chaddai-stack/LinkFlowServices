CREATE TABLE IF NOT EXISTS link_qr_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    link_id BIGINT NOT NULL REFERENCES url_mapping(id) ON DELETE CASCADE,
    format VARCHAR(20) NOT NULL,
    size INTEGER NOT NULL,
    storage_key TEXT NOT NULL,
    storage_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_link_qr_assets_variant UNIQUE (link_id, format, size)
);

CREATE INDEX IF NOT EXISTS idx_link_qr_assets_link_id
    ON link_qr_assets(link_id);
