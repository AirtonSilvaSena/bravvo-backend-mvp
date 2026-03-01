-- V12__add_nome_owner_to_estabelecimentos_pre_cadastros.sql
-- Objetivo: adicionar coluna nome_owner no pré-cadastro do estabelecimento
-- Motivo: armazenar nome do proprietário separadamente do nome do salão

ALTER TABLE estabelecimentos_pre_cadastros
ADD COLUMN nome_owner VARCHAR(150) NULL AFTER nome;