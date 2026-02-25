ALTER TABLE estabelecimentos
    ADD COLUMN logo_key VARCHAR(255) NULL AFTER updated_at,
    ADD COLUMN logo_mime_type VARCHAR(100) NULL AFTER logo_key,
    ADD COLUMN logo_size_bytes BIGINT NULL AFTER logo_mime_type,
    ADD COLUMN logo_updated_at DATETIME NULL AFTER logo_size_bytes;