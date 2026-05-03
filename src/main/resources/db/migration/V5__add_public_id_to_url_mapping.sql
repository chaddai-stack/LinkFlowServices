ALTER TABLE url_mapping
    ADD COLUMN IF NOT EXISTS public_id UUID;

UPDATE url_mapping
SET public_id = gen_random_uuid()
WHERE public_id IS NULL;

ALTER TABLE url_mapping
    ALTER COLUMN public_id SET NOT NULL,
    ALTER COLUMN public_id SET DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS uk_url_mapping_public_id
    ON url_mapping(public_id);
