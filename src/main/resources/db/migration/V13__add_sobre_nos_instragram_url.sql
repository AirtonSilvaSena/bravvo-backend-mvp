-- V14__add_instagram_facebook_and_sobre_nos_to_estabelecimentos.sql
-- Adiciona colunas públicas de redes sociais e descrição institucional

ALTER TABLE estabelecimentos
ADD COLUMN sobre_nos TEXT NULL COMMENT 'Texto público de apresentação do estabelecimento (sobre nós)',
ADD COLUMN instagram_url VARCHAR(255) NULL COMMENT 'URL pública do Instagram';
