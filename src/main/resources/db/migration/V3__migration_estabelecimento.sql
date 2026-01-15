-- V3__refactor_salao_to_estabelecimento.sql
-- Refactor: salao/saloes -> estabelecimento/estabelecimentos
-- DB: MariaDB 10.11+
-- Objetivo: renomear tabelas/colunas e recriar constraints/indexes com nova nomenclatura

-- =========================================
-- 0) Segurança
-- =========================================
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================
-- 1) USERS: dropar FK/índice antigos e renomear coluna do tenant
-- =========================================

-- Drop FK antiga (users.salao_id -> saloes.id)
ALTER TABLE `users`
  DROP FOREIGN KEY `fk_users_salao`;

-- Drop índice antigo (caso exista)
ALTER TABLE `users`
  DROP INDEX `idx_users_salao_perfil_ativo`;

-- Renomear coluna
ALTER TABLE `users`
  RENAME COLUMN `salao_id` TO `estabelecimento_id`;

-- =========================================
-- 2) Renomear tabelas
-- =========================================

RENAME TABLE `saloes` TO `estabelecimentos`;
RENAME TABLE `salao_pre_cadastros` TO `estabelecimento_pre_cadastros`;

-- =========================================
-- 3) ESTABELECIMENTOS: recriar constraints/indexes com nomes novos
--    (vamos dropar os antigos herdados e criar com nomes atualizados)
-- =========================================

-- 3.1) Drop FK antiga (nome antigo) e recria com nome novo
ALTER TABLE `estabelecimentos`
  DROP FOREIGN KEY `fk_saloes_owner_user`;

ALTER TABLE `estabelecimentos`
  ADD CONSTRAINT `fk_estabelecimentos_owner_user`
  FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`);

-- 3.2) Drop CHECK antigo e recria com nome novo
ALTER TABLE `estabelecimentos`
  DROP CHECK `chk_saloes_status`;

ALTER TABLE `estabelecimentos`
  ADD CONSTRAINT `chk_estabelecimentos_status`
  CHECK (`status_assinatura` in ('TRIAL','ATIVO','INADIMPLENTE','CANCELADO'));

-- 3.3) Recriar índices com nomes novos (drop dos antigos e add novos)
ALTER TABLE `estabelecimentos`
  DROP INDEX `uk_saloes_slug`,
  DROP INDEX `idx_saloes_status`,
  DROP INDEX `idx_saloes_owner`;

ALTER TABLE `estabelecimentos`
  ADD UNIQUE KEY `uk_estabelecimentos_slug` (`slug`),
  ADD KEY `idx_estabelecimentos_status` (`status_assinatura`),
  ADD KEY `idx_estabelecimentos_owner` (`owner_user_id`);

-- =========================================
-- 4) USERS: recriar índice e FK do tenant com nomenclatura nova
-- =========================================

ALTER TABLE `users`
  ADD KEY `idx_users_estabelecimento_perfil_ativo` (`estabelecimento_id`,`perfil`,`ativo`);

ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_estabelecimento`
  FOREIGN KEY (`estabelecimento_id`) REFERENCES `estabelecimentos` (`id`);

-- =========================================
-- 5) Finalizar
-- =========================================
SET FOREIGN_KEY_CHECKS = 1;
