-- V{N}__contract_users_remove_tenant_columns.sql
-- Migration 2 (CONTRACT)
-- Objetivo:
-- - Consolidar users por email (global) para evitar duplicidade
-- - Atualizar todas as FKs que apontam para users(id)
-- - Remover colunas tenant/perfil do users: estabelecimento_id, perfil
-- - Criar UNIQUE global por email

-- ============================================================
-- 0) MERGE AUTOMÁTICO DE USERS DUPLICADOS POR EMAIL
--    - mantém o menor id como canônico
--    - atualiza todas as FKs do schema que referenciam users(id)
--    - remove os users duplicados (não-canônicos)
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_merge_duplicate_users_by_email$$

CREATE PROCEDURE sp_merge_duplicate_users_by_email()
BEGIN
  -- Cria tabela temporária de mapeamento old_id -> new_id
  DROP TEMPORARY TABLE IF EXISTS tmp_user_merge_map;
  CREATE TEMPORARY TABLE tmp_user_merge_map (
    old_id BIGINT UNSIGNED NOT NULL,
    new_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (old_id),
    KEY idx_new_id (new_id)
  ) ENGINE=Memory;

  -- Popula o mapa para emails duplicados:
  -- new_id = menor id do email, old_id = demais ids
  INSERT INTO tmp_user_merge_map (old_id, new_id)
  SELECT u.id AS old_id, t.keep_id AS new_id
  FROM users u
  JOIN (
    SELECT email, MIN(id) AS keep_id, COUNT(*) AS cnt
    FROM users
    GROUP BY email
    HAVING cnt > 1
  ) t ON t.email = u.email
  WHERE u.id <> t.keep_id;

  -- Se não houver duplicados, sai
  IF (SELECT COUNT(*) FROM tmp_user_merge_map) = 0 THEN
    LEAVE BEGIN;
  END IF;

  -- Atualiza TODAS as FKs do schema que referenciam users(id)
  -- (inclui estabelecimentos.owner_user_id, estabelecimento_users.user_id, refresh_tokens.user_id, etc.)
  BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_table_name VARCHAR(255);
    DECLARE v_column_name VARCHAR(255);

    DECLARE cur CURSOR FOR
      SELECT kcu.TABLE_NAME, kcu.COLUMN_NAME
      FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
      WHERE kcu.TABLE_SCHEMA = DATABASE()
        AND kcu.REFERENCED_TABLE_NAME = 'users'
        AND kcu.REFERENCED_COLUMN_NAME = 'id';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;

    read_loop: LOOP
      FETCH cur INTO v_table_name, v_column_name;
      IF done = 1 THEN
        LEAVE read_loop;
      END IF;

      -- Monta e executa:
      -- UPDATE <table> t
      -- JOIN tmp_user_merge_map m ON t.<col> = m.old_id
      -- SET t.<col> = m.new_id;
      SET @sql = CONCAT(
        'UPDATE `', v_table_name, '` t ',
        'JOIN tmp_user_merge_map m ON t.`', v_column_name, '` = m.old_id ',
        'SET t.`', v_column_name, '` = m.new_id'
      );

      PREPARE stmt FROM @sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END LOOP;

    CLOSE cur;
  END;

  -- Remove os users duplicados (old_id)
  DELETE u
  FROM users u
  JOIN tmp_user_merge_map m ON u.id = m.old_id;

  -- Limpa temp
  DROP TEMPORARY TABLE IF EXISTS tmp_user_merge_map;
END$$

CALL sp_merge_duplicate_users_by_email()$$

DROP PROCEDURE IF EXISTS sp_merge_duplicate_users_by_email$$

DELIMITER ;

-- ============================================================
-- 1) DROP constraints/índices antigos dependentes de estabelecimento_id/perfil
-- ============================================================

-- FK users -> estabelecimentos (tenant antigo)
ALTER TABLE users
  DROP FOREIGN KEY fk_users_estabelecimento;

-- Uniques por tenant
ALTER TABLE users
  DROP INDEX uk_users_estabelecimento_email,
  DROP INDEX uk_users_estabelecimento_telefone;

-- Índices que dependem de estabelecimento_id e/ou perfil
ALTER TABLE users
  DROP INDEX idx_users_estabelecimento_perfil_ativo,
  DROP INDEX idx_users_perfil_ativo;

-- CHECK antigo de perfil (nomeado)
ALTER TABLE users
  DROP CHECK chk_users_perfil;

-- ============================================================
-- 2) DROP colunas tenant/perfil no users
-- ============================================================

ALTER TABLE users
  DROP COLUMN estabelecimento_id,
  DROP COLUMN perfil;

-- ============================================================
-- 3) Cria UNIQUE global por email
-- ============================================================

-- Garanta que email está normalizado fora daqui (ideal: lower no app).
-- Aqui apenas aplicamos a constraint.
ALTER TABLE users
  ADD CONSTRAINT uk_users_email UNIQUE (email);

-- (Opcional, mas recomendado) índice útil para buscas por telefone
-- Mantém idx_users_telefone que já existe; não cria unique global em telefone aqui.

-- Índice útil para filtros por ativo
CREATE INDEX idx_users_ativo ON users (ativo);