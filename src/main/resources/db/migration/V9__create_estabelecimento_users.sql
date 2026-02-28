-- ============================================================
-- 1) Criar tabela de relacionamento estabelecimento_users
-- ============================================================

CREATE TABLE `estabelecimento_users` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `estabelecimento_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `perfil` varchar(50) NOT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),

  -- Um usuário só pode ter um vínculo por estabelecimento
  UNIQUE KEY `uk_estabelecimento_user` (`estabelecimento_id`,`user_id`),

  KEY `idx_eu_user` (`user_id`),
  KEY `idx_eu_estabelecimento` (`estabelecimento_id`),
  KEY `idx_eu_perfil_ativo` (`perfil`,`ativo`),

  CONSTRAINT `fk_eu_estabelecimento`
    FOREIGN KEY (`estabelecimento_id`)
    REFERENCES `estabelecimentos` (`id`)
    ON DELETE CASCADE,

  CONSTRAINT `fk_eu_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE,

  CONSTRAINT `chk_eu_perfil`
    CHECK (`perfil` in ('ADMIN','FUNCIONARIO','CLIENTE'))

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;


-- ============================================================
-- 2) Backfill (copiar dados atuais)
-- ============================================================

INSERT INTO estabelecimento_users
    (estabelecimento_id, user_id, perfil, ativo, created_at, updated_at)
SELECT
    u.estabelecimento_id,
    u.id,
    u.perfil,
    u.ativo,
    u.created_at,
    u.updated_at
FROM users u
WHERE u.estabelecimento_id IS NOT NULL;