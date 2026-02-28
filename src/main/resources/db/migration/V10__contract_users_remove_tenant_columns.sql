-- V10__contract_users_remove_tenant_columns.sql
-- Contract migration: remove colunas antigas de multi-tenant em users (estabelecimento_id, perfil)
-- Pré-requisito: tabela estabelecimento_users já existe e está com vínculos preenchidos (expand/backfill já executado).

/*
  ✅ Estratégia:
  1) (Opcional, porém recomendado) Mesclar usuários duplicados por email em um único user global:
     - keep_id = menor id por email
     - move FKs conhecidas para keep_id
     - deleta duplicados
  2) Remover constraints/indexes que dependem de users.estabelecimento_id / users.perfil
  3) Dropar colunas estabelecimento_id e perfil de users
  4) Criar UNIQUE global para users.email (agora email vira identidade global)
*/

-- =========================
-- 1) MERGE DE DUPLICADOS (POR EMAIL)
-- =========================

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_merge_duplicate_users_by_email $$

CREATE PROCEDURE sp_merge_duplicate_users_by_email()
proc: BEGIN
  DECLARE dup_count INT DEFAULT 0;

  -- Tabela temporária old_id -> keep_id
  DROP TEMPORARY TABLE IF EXISTS tmp_user_merge_map;
  CREATE TEMPORARY TABLE tmp_user_merge_map (
    old_id BIGINT UNSIGNED NOT NULL,
    keep_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (old_id),
    KEY idx_keep_id (keep_id)
  ) ENGINE=Memory;

  -- Monta mapa: para cada email duplicado, mantém menor id e marca os demais como old_id
  INSERT INTO tmp_user_merge_map (old_id, keep_id)
  SELECT u.id AS old_id, t.keep_id AS keep_id
  FROM users u
  JOIN (
    SELECT email, MIN(id) AS keep_id, COUNT(*) AS cnt
    FROM users
    GROUP BY email
    HAVING cnt > 1
  ) t ON t.email = u.email
  WHERE u.id <> t.keep_id;

  SELECT COUNT(*) INTO dup_count FROM tmp_user_merge_map;

  IF dup_count = 0 THEN
    DROP TEMPORARY TABLE IF EXISTS tmp_user_merge_map;
    LEAVE proc;
  END IF;

  /*
    ⚠️ IMPORTANTE:
    Aqui atualizamos FKs mais prováveis no seu schema.
    Se você tiver outras tabelas referenciando users(id), adicione mais blocos UPDATE no mesmo padrão.
  */

  -- 1) refresh_tokens.user_id
  UPDATE refresh_tokens rt
  JOIN tmp_user_merge_map m ON rt.user_id = m.old_id
  SET rt.user_id = m.keep_id;

  -- 2) estabelecimentos.owner_user_id
  UPDATE estabelecimentos e
  JOIN tmp_user_merge_map m ON e.owner_user_id = m.old_id
  SET e.owner_user_id = m.keep_id;

  -- 3) estabelecimento_users.user_id
  UPDATE estabelecimento_users eu
  JOIN tmp_user_merge_map m ON eu.user_id = m.old_id
  SET eu.user_id = m.keep_id;

  -- Remove duplicados (old_id)
  DELETE u
  FROM users u
  JOIN tmp_user_merge_map m ON u.id = m.old_id;

  DROP TEMPORARY TABLE IF EXISTS tmp_user_merge_map;
END $$

DELIMITER ;

-- Executa merge
CALL sp_merge_duplicate_users_by_email();

-- Remove procedure (não deixar lixo no schema)
DROP PROCEDURE IF EXISTS sp_merge_duplicate_users_by_email;

-- =========================
-- 2) DROP CONSTRAINTS/INDEXES ANTIGOS
-- =========================

-- FK users -> estabelecimentos (estabelecimento_id)
-- (nome no seu schema: fk_users_estabelecimento)
ALTER TABLE users
  DROP FOREIGN KEY fk_users_estabelecimento;

-- Unique composto (estabelecimento_id, email / telefone)
DROP INDEX uk_users_estabelecimento_email ON users;
DROP INDEX uk_users_estabelecimento_telefone ON users;

-- Indexes que dependem de estabelecimento_id/perfil
DROP INDEX idx_users_estabelecimento_perfil_ativo ON users;
DROP INDEX idx_users_perfil_ativo ON users;

-- chk antigo do perfil na users (vamos remover junto com a coluna)
-- Em MariaDB, o CHECK existe como constraint; dependendo da versão ele pode ter nome interno.
-- Como você mostrou o nome "chk_users_perfil", vamos tentar dropar explicitamente:
ALTER TABLE users
  DROP CONSTRAINT chk_users_perfil;

-- =========================
-- 3) DROP COLUNAS ANTIGAS
-- =========================

ALTER TABLE users
  DROP COLUMN estabelecimento_id,
  DROP COLUMN perfil;

-- =========================
-- 4) GARANTIAS NOVAS (email global)
-- =========================

-- Agora o email vira identidade global do usuário (um usuário único pode ter múltiplos vínculos em estabelecimento_users)
-- Se já existir index/constraint diferente, ajuste o nome:
ALTER TABLE users
  ADD CONSTRAINT uk_users_email UNIQUE (email);

-- (Opcional) Índice útil para login e busca
CREATE INDEX idx_users_email_ativo ON users (email, ativo);