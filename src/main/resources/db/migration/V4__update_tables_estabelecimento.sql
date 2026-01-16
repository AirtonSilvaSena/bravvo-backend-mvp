-- V4__add_ramo_atuacao_to_estabelecimentos.sql
-- Adiciona o campo ramo_atuacao aos estabelecimentos
-- DB: MariaDB | Projeto: Bravvo (MVP)

-- =========================================================
-- 1) Tabela: estabelecimentos
-- =========================================================
ALTER TABLE `estabelecimentos`
  ADD COLUMN IF NOT EXISTS `ramo_atuacao` varchar(60) NOT NULL
  AFTER `nome`;

-- =========================================================
-- 2) Tabela: estabelecimentos_pre_cadastros
-- =========================================================
ALTER TABLE `estabelecimentos_pre_cadastros`
  ADD COLUMN IF NOT EXISTS `ramo_atuacao` varchar(60) NOT NULL
  AFTER `nome`;