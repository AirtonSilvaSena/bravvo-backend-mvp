-- V11__add_estabelecimento_id_multi_tenant.sql
-- MariaDB 10.11 - idempotente (DB limpo, sem backfill)

-- ==========================================================
-- 1) SERVICOS
-- ==========================================================
ALTER TABLE servicos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

CREATE INDEX IF NOT EXISTS idx_servicos_estabelecimento
  ON servicos (estabelecimento_id);

CREATE INDEX IF NOT EXISTS idx_servicos_estabelecimento_status
  ON servicos (estabelecimento_id, status);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'servicos'
    AND constraint_name = 'fk_servicos_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE servicos ADD CONSTRAINT fk_servicos_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================
-- 2) FUNCIONARIO_SERVICOS
-- ==========================================================
ALTER TABLE funcionario_servicos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER servico_id;

CREATE INDEX IF NOT EXISTS idx_fs_estabelecimento_funcionario
  ON funcionario_servicos (estabelecimento_id, funcionario_id);

CREATE INDEX IF NOT EXISTS idx_fs_estabelecimento_servico
  ON funcionario_servicos (estabelecimento_id, servico_id);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'funcionario_servicos'
    AND constraint_name = 'fk_fs_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE funcionario_servicos ADD CONSTRAINT fk_fs_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================
-- 3) FUNCIONARIO_PREFS
-- ==========================================================
ALTER TABLE funcionario_prefs
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

CREATE INDEX IF NOT EXISTS idx_fp_estabelecimento_funcionario
  ON funcionario_prefs (estabelecimento_id, funcionario_id);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'funcionario_prefs'
    AND constraint_name = 'fk_fp_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE funcionario_prefs ADD CONSTRAINT fk_fp_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================
-- 4) FUNCIONARIO_AGENDA
-- ==========================================================
ALTER TABLE funcionario_agenda
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

CREATE INDEX IF NOT EXISTS idx_fa_estabelecimento_funcionario
  ON funcionario_agenda (estabelecimento_id, funcionario_id);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'funcionario_agenda'
    AND constraint_name = 'fk_fa_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE funcionario_agenda ADD CONSTRAINT fk_fa_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================
-- 5) FUNCIONARIO_BLOQUEIOS
-- ==========================================================
ALTER TABLE funcionario_bloqueios
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

CREATE INDEX IF NOT EXISTS idx_fb_estabelecimento_funcionario
  ON funcionario_bloqueios (estabelecimento_id, funcionario_id);

CREATE INDEX IF NOT EXISTS idx_fb_estabelecimento_periodo
  ON funcionario_bloqueios (estabelecimento_id, funcionario_id, start_dt, end_dt);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'funcionario_bloqueios'
    AND constraint_name = 'fk_fb_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE funcionario_bloqueios ADD CONSTRAINT fk_fb_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================
-- 6) AGENDAMENTOS
-- ==========================================================
ALTER TABLE agendamentos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

CREATE INDEX IF NOT EXISTS idx_ag_estabelecimento_inicio
  ON agendamentos (estabelecimento_id, inicio);

CREATE INDEX IF NOT EXISTS idx_ag_estabelecimento_funcionario_inicio
  ON agendamentos (estabelecimento_id, funcionario_id, inicio);

CREATE INDEX IF NOT EXISTS idx_ag_estabelecimento_status_inicio
  ON agendamentos (estabelecimento_id, status, inicio);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'agendamentos'
    AND constraint_name = 'fk_ag_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE agendamentos ADD CONSTRAINT fk_ag_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop uk_ag_protocolo se existir
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'agendamentos'
    AND index_name = 'uk_ag_protocolo'
);
SET @sql := IF(@idx_exists > 0,
  'ALTER TABLE agendamentos DROP INDEX uk_ag_protocolo',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add unique por tenant (se não existir)
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'agendamentos'
    AND index_name = 'uk_ag_estabelecimento_protocolo'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE agendamentos ADD UNIQUE KEY uk_ag_estabelecimento_protocolo (estabelecimento_id, protocolo)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ==========================================================
-- 7) AUDITORIA_LOGS
-- ==========================================================
ALTER TABLE auditoria_logs
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

CREATE INDEX IF NOT EXISTS idx_audit_estabelecimento_created
  ON auditoria_logs (estabelecimento_id, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_estabelecimento_actor
  ON auditoria_logs (estabelecimento_id, actor_user_id);

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'auditoria_logs'
    AND constraint_name = 'fk_audit_estabelecimentos'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE auditoria_logs ADD CONSTRAINT fk_audit_estabelecimentos FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;