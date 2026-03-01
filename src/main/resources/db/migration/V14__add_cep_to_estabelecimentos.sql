-- V14__add_cep_to_estabelecimentos.sql
-- Adiciona coluna CEP ao estabelecimento

ALTER TABLE estabelecimentos
ADD COLUMN cep VARCHAR(9) NULL COMMENT 'CEP do estabelecimento (formato 99999-999)';