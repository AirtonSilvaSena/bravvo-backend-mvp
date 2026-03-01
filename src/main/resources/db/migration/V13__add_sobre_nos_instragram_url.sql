
ALTER TABLE estabelecimentos
ADD COLUMN sobre_nos TEXT NULL COMMENT 'Texto público de apresentação do estabelecimento (sobre nós)',
ADD COLUMN instagram_url VARCHAR(255) NULL COMMENT 'URL pública do Instagram';
