-- V3__refactor_saloes_to_estabelecimentos.sql
-- Refactor completo:
--  - saloes -> estabelecimentos
--  - salao_pre_cadastros -> estabelecimentos_pre_cadastros
--  - users.salao_id -> users.estabelecimento_id
--  - Recria FKs e índices com novos nomes
-- DB: MariaDB / InnoDB

-- =========================
-- 0) Segurança
-- =========================
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 1) Dropar FK users -> saloes (se existir)
-- =========================
SET @fk_users_salao_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND CONSTRAINT_NAME = 'fk_users_salao'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_drop_fk_users_salao := IF(@fk_users_salao_exists > 0,
  'ALTER TABLE `users` DROP FOREIGN KEY `fk_users_salao`;',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_drop_fk_users_salao;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 2) Renomear coluna users.salao_id -> users.estabelecimento_id (se existir)
-- =========================
SET @col_salao_id_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'salao_id'
);

SET @col_estab_id_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'estabelecimento_id'
);

-- Se salao_id existe e estabelecimento_id NÃO existe, renomeia.
SET @sql_rename_users_col := IF(@col_salao_id_exists > 0 AND @col_estab_id_exists = 0,
  'ALTER TABLE `users` CHANGE COLUMN `salao_id` `estabelecimento_id` bigint(20) unsigned DEFAULT NULL;',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_rename_users_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 3) Remover índice antigo do tenant (se existir)
-- =========================
SET @idx_old_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_salao_perfil_ativo'
);

SET @sql_drop_old_idx := IF(@idx_old_exists > 0,
  'ALTER TABLE `users` DROP INDEX `idx_users_salao_perfil_ativo`;',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_drop_old_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 4) Renomear tabelas
-- =========================
-- Só renomeia se a origem existir. (pra evitar erro em ambientes já migrados parcialmente)
SET @tbl_saloes_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'saloes'
);

SET @tbl_estab_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos'
);

SET @tbl_pre_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'salao_pre_cadastros'
);

SET @tbl_pre_new_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos_pre_cadastros'
);

-- Renomeia saloes -> estabelecimentos (se aplicável)
SET @sql_rename_saloes := IF(@tbl_saloes_exists > 0 AND @tbl_estab_exists = 0,
  'RENAME TABLE `saloes` TO `estabelecimentos`;',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_rename_saloes;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Renomeia salao_pre_cadastros -> estabelecimentos_pre_cadastros (se aplicável)
SET @sql_rename_pre := IF(@tbl_pre_exists > 0 AND @tbl_pre_new_exists = 0,
  'RENAME TABLE `salao_pre_cadastros` TO `estabelecimentos_pre_cadastros`;',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_rename_pre;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 5) Renomear índices da tabela estabelecimentos (se existirem com nomes antigos)
-- =========================
-- uk_saloes_slug -> uk_estabelecimentos_slug
SET @idx_uk_saloes_slug := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos'
    AND INDEX_NAME = 'uk_saloes_slug'
);

SET @sql_rename_uk_slug := IF(@idx_uk_saloes_slug > 0,
  'ALTER TABLE `estabelecimentos` RENAME INDEX `uk_saloes_slug` TO `uk_estabelecimentos_slug`;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql_rename_uk_slug;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- idx_saloes_status -> idx_estabelecimentos_status
SET @idx_saloes_status := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos'
    AND INDEX_NAME = 'idx_saloes_status'
);

SET @sql_rename_idx_status := IF(@idx_saloes_status > 0,
  'ALTER TABLE `estabelecimentos` RENAME INDEX `idx_saloes_status` TO `idx_estabelecimentos_status`;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql_rename_idx_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- idx_saloes_owner -> idx_estabelecimentos_owner
SET @idx_saloes_owner := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos'
    AND INDEX_NAME = 'idx_saloes_owner'
);

SET @sql_rename_idx_owner := IF(@idx_saloes_owner > 0,
  'ALTER TABLE `estabelecimentos` RENAME INDEX `idx_saloes_owner` TO `idx_estabelecimentos_owner`;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql_rename_idx_owner;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 6) Ajustar FK do owner (se tiver nome antigo)
-- =========================
SET @fk_owner_old_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos'
    AND CONSTRAINT_NAME = 'fk_saloes_owner_user'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_drop_fk_owner_old := IF(@fk_owner_old_exists > 0,
  'ALTER TABLE `estabelecimentos` DROP FOREIGN KEY `fk_saloes_owner_user`;',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_drop_fk_owner_old;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Criar FK owner nova (se ainda não existir)
SET @fk_owner_new_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos'
    AND CONSTRAINT_NAME = 'fk_estabelecimentos_owner_user'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_add_fk_owner_new := IF(@fk_owner_new_exists = 0,
  'ALTER TABLE `estabelecimentos`
     ADD CONSTRAINT `fk_estabelecimentos_owner_user`
     FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`);',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_add_fk_owner_new;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 7) Renomear índices do pre-cadastro (se existirem com nomes antigos)
-- =========================
-- uk_pre_email -> uk_est_pre_email
SET @pre_uk_email := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos_pre_cadastros'
    AND INDEX_NAME = 'uk_pre_email'
);

SET @sql_rename_pre_uk_email := IF(@pre_uk_email > 0,
  'ALTER TABLE `estabelecimentos_pre_cadastros` RENAME INDEX `uk_pre_email` TO `uk_est_pre_email`;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql_rename_pre_uk_email;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- uk_pre_slug -> uk_est_pre_slug
SET @pre_uk_slug := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos_pre_cadastros'
    AND INDEX_NAME = 'uk_pre_slug'
);

SET @sql_rename_pre_uk_slug := IF(@pre_uk_slug > 0,
  'ALTER TABLE `estabelecimentos_pre_cadastros` RENAME INDEX `uk_pre_slug` TO `uk_est_pre_slug`;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql_rename_pre_uk_slug;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- idx_pre_expires -> idx_est_pre_expires
SET @pre_idx_exp := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'estabelecimentos_pre_cadastros'
    AND INDEX_NAME = 'idx_pre_expires'
);

SET @sql_rename_pre_idx_exp := IF(@pre_idx_exp > 0,
  'ALTER TABLE `estabelecimentos_pre_cadastros` RENAME INDEX `idx_pre_expires` TO `idx_est_pre_expires`;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql_rename_pre_idx_exp;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================
-- 8) Recriar índice e FK do tenant em users agora usando estabelecimento_id
-- =========================
SET @idx_new_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_estabelecimento_perfil_ativo'
);

SET @sql_add_new_idx := IF(@idx_new_exists = 0,
  'ALTER TABLE `users` ADD KEY `idx_users_estabelecimento_perfil_ativo` (`estabelecimento_id`,`perfil`,`ativo`);',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_add_new_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_users_estab_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND CONSTRAINT_NAME = 'fk_users_estabelecimento'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql_add_fk_users_estab := IF(@fk_users_estab_exists = 0,
  'ALTER TABLE `users`
     ADD CONSTRAINT `fk_users_estabelecimento`
     FOREIGN KEY (`estabelecimento_id`) REFERENCES `estabelecimentos` (`id`);',
  'SELECT 1;'
);

PREPARE stmt FROM @sql_add_fk_users_estab;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
