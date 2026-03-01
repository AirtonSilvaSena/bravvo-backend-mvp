-- V11__add_estabelecimento_id_multi_tenant.sql
-- MariaDB 10.11 - idempotente (pode rodar mais de uma vez)
-- Adiciona estabelecimento_id + backfill + índices + FKs + unique protocolo por tenant

-- ==========================================================
-- Helpers: cria índice se não existir (via information_schema)
-- e cria FK se não existir (via information_schema)
-- ==========================================================

-- -------------------------
-- 1) SERVICOS
-- -------------------------
ALTER TABLE servicos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

-- Backfill:
UPDATE servicos s
JOIN (
  SELECT fs.servico_id, MIN(u.estabelecimento_id) AS estabelecimento_id
  FROM funcionario_servicos fs
  JOIN users u ON u.id = fs.funcionario_id
  WHERE u.estabelecimento_id IS NOT NULL
  GROUP BY fs.servico_id
) x ON x.servico_id = s.id
SET s.estabelecimento_id = x.estabelecimento_id
WHERE s.estabelecimento_id IS NULL;

UPDATE servicos s
JOIN (SELECT MIN(id) AS est_id, COUNT(*) AS cnt FROM estabelecimentos) e
SET s.estabelecimento_id = e.est_id
WHERE s.estabelecimento_id IS NULL AND e.cnt = 1;

-- Índices (sem IF NOT EXISTS)
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'servicos'
    AND index_name = 'idx_servicos_estabelecimento'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_servicos_estabelecimento ON servicos (estabelecimento_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'servicos'
    AND index_name = 'idx_servicos_estabelecimento_status'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_servicos_estabelecimento_status ON servicos (estabelecimento_id, status)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK servicos -> estabelecimentos (só se não existir)
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

-- -------------------------
-- 2) FUNCIONARIO_SERVICOS
-- -------------------------
ALTER TABLE funcionario_servicos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER servico_id;

UPDATE funcionario_servicos fs
JOIN users u ON u.id = fs.funcionario_id
SET fs.estabelecimento_id = u.estabelecimento_id
WHERE fs.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'funcionario_servicos'
    AND index_name = 'idx_fs_estabelecimento_funcionario'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fs_estabelecimento_funcionario ON funcionario_servicos (estabelecimento_id, funcionario_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'funcionario_servicos'
    AND index_name = 'idx_fs_estabelecimento_servico'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fs_estabelecimento_servico ON funcionario_servicos (estabelecimento_id, servico_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- -------------------------
-- 3) FUNCIONARIO_PREFS
-- -------------------------
ALTER TABLE funcionario_prefs
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

UPDATE funcionario_prefs fp
JOIN users u ON u.id = fp.funcionario_id
SET fp.estabelecimento_id = u.estabelecimento_id
WHERE fp.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'funcionario_prefs'
    AND index_name = 'idx_fp_estabelecimento_funcionario'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fp_estabelecimento_funcionario ON funcionario_prefs (estabelecimento_id, funcionario_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- -------------------------
-- 4) FUNCIONARIO_AGENDA
-- -------------------------
ALTER TABLE funcionario_agenda
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

UPDATE funcionario_agenda fa
JOIN users u ON u.id = fa.funcionario_id
SET fa.estabelecimento_id = u.estabelecimento_id
WHERE fa.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'funcionario_agenda'
    AND index_name = 'idx_fa_estabelecimento_funcionario'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fa_estabelecimento_funcionario ON funcionario_agenda (estabelecimento_id, funcionario_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- -------------------------
-- 5) FUNCIONARIO_BLOQUEIOS
-- -------------------------
ALTER TABLE funcionario_bloqueios
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

UPDATE funcionario_bloqueios fb
JOIN users u ON u.id = fb.funcionario_id
SET fb.estabelecimento_id = u.estabelecimento_id
WHERE fb.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'funcionario_bloqueios'
    AND index_name = 'idx_fb_estabelecimento_funcionario'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fb_estabelecimento_funcionario ON funcionario_bloqueios (estabelecimento_id, funcionario_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'funcionario_bloqueios'
    AND index_name = 'idx_fb_estabelecimento_periodo'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_fb_estabelecimento_periodo ON funcionario_bloqueios (estabelecimento_id, funcionario_id, start_dt, end_dt)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- -------------------------
-- 6) AGENDAMENTOS
-- -------------------------
ALTER TABLE agendamentos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

UPDATE agendamentos a
JOIN users u ON u.id = a.funcionario_id
SET a.estabelecimento_id = u.estabelecimento_id
WHERE a.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'agendamentos'
    AND index_name = 'idx_ag_estabelecimento_inicio'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_ag_estabelecimento_inicio ON agendamentos (estabelecimento_id, inicio)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'agendamentos'
    AND index_name = 'idx_ag_estabelecimento_funcionario_inicio'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_ag_estabelecimento_funcionario_inicio ON agendamentos (estabelecimento_id, funcionario_id, inicio)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'agendamentos'
    AND index_name = 'idx_ag_estabelecimento_status_inicio'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_ag_estabelecimento_status_inicio ON agendamentos (estabelecimento_id, status, inicio)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- Unique protocolo por tenant:
-- drop uk_ag_protocolo se existir
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

-- cria unique (estabelecimento_id, protocolo) se não existir
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

-- -------------------------
-- 7) AUDITORIA_LOGS
-- -------------------------
ALTER TABLE auditoria_logs
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

UPDATE auditoria_logs al
JOIN users u ON u.id = al.actor_user_id
SET al.estabelecimento_id = u.estabelecimento_id
WHERE al.estabelecimento_id IS NULL
  AND al.actor_user_id IS NOT NULL
  AND u.estabelecimento_id IS NOT NULL;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'auditoria_logs'
    AND index_name = 'idx_audit_estabelecimento_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_audit_estabelecimento_created ON auditoria_logs (estabelecimento_id, created_at)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'auditoria_logs'
    AND index_name = 'idx_audit_estabelecimento_actor'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_audit_estabelecimento_actor ON auditoria_logs (estabelecimento_id, actor_user_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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