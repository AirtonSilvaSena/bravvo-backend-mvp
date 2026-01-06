-- V2__saloes_and_pre_cadastro.sql
-- SaaS Core (MVP): saloes + pre-cadastro com confirmação de e-mail
-- DB: MariaDB

-- =========================================================
-- 1) Tabela: saloes
-- =========================================================
CREATE TABLE IF NOT EXISTS `saloes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `nome` varchar(120) NOT NULL,
  `slug` varchar(60) NOT NULL,
  `status_assinatura` varchar(20) NOT NULL DEFAULT 'TRIAL',
  `trial_ends_at` datetime DEFAULT NULL,
  `owner_user_id` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_saloes_slug` (`slug`),
  KEY `idx_saloes_status` (`status_assinatura`),
  KEY `idx_saloes_owner` (`owner_user_id`),
  CONSTRAINT `chk_saloes_status` CHECK (`status_assinatura` in ('TRIAL','ATIVO','INADIMPLENTE','CANCELADO')),
  CONSTRAINT `fk_saloes_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =========================================================
-- 2) Tabela: salao_pre_cadastros
--    (guarda dados até confirmar o e-mail)
-- =========================================================
CREATE TABLE IF NOT EXISTS `salao_pre_cadastros` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `nome` varchar(120) NOT NULL,
  `email` varchar(180) NOT NULL,
  `telefone` varchar(30) DEFAULT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `slug` varchar(60) NOT NULL,
  `codigo_hash` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL,
  `attempts` int(11) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pre_email` (`email`),
  UNIQUE KEY `uk_pre_slug` (`slug`),
  KEY `idx_pre_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =========================================================
-- 3) Ajustes em users: multi-tenant + email_verificado
-- =========================================================
ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `salao_id` bigint(20) unsigned DEFAULT NULL AFTER `senha_hash`,
  ADD COLUMN IF NOT EXISTS `email_verificado` tinyint(1) NOT NULL DEFAULT 0 AFTER `ativo`;

-- Index para filtragem por tenant
ALTER TABLE `users`
  ADD KEY IF NOT EXISTS `idx_users_salao_perfil_ativo` (`salao_id`,`perfil`,`ativo`);

-- FK do tenant
ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_salao`
  FOREIGN KEY (`salao_id`) REFERENCES `saloes` (`id`);
