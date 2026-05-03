DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'url_mapping'
          AND column_name = 'slug'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'url_mapping'
          AND column_name = 'back_half'
    ) THEN
        ALTER TABLE url_mapping RENAME COLUMN slug TO back_half;
    END IF;
END $$;

ALTER TABLE url_mapping
    ALTER COLUMN back_half TYPE VARCHAR(80);
