-- V11__add_estabelecimento_id_multi_tenant.sql
-- Adiciona estabelecimento_id nas tabelas do MVP e faz backfill com base em users.estabelecimento_id.
-- MariaDB 10.11

-- =========================================
-- 1) SERVICOS
-- =========================================
ALTER TABLE servicos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

-- Backfill do estabelecimento_id em servicos:
-- 1) tenta inferir pelo vínculo funcionario_servicos -> users (funcionario)
UPDATE servicos s
JOIN (
  SELECT
    fs.servico_id,
    MIN(u.estabelecimento_id) AS estabelecimento_id
  FROM funcionario_servicos fs
  JOIN users u ON u.id = fs.funcionario_id
  WHERE u.estabelecimento_id IS NOT NULL
  GROUP BY fs.servico_id
) x ON x.servico_id = s.id
SET s.estabelecimento_id = x.estabelecimento_id
WHERE s.estabelecimento_id IS NULL;

-- 2) fallback: se ainda estiver NULL e existir só 1 estabelecimento, joga nele
UPDATE servicos s
JOIN (SELECT MIN(id) AS est_id, COUNT(*) AS cnt FROM estabelecimentos) e
SET s.estabelecimento_id = e.est_id
WHERE s.estabelecimento_id IS NULL
  AND e.cnt = 1;

-- Índices + FK (FK só se não existir; MariaDB não tem IF NOT EXISTS para CONSTRAINT, então usamos nomes fixos)
ALTER TABLE servicos
  ADD INDEX IF NOT EXISTS idx_servicos_estabelecimento (estabelecimento_id),
  ADD INDEX IF NOT EXISTS idx_servicos_estabelecimento_status (estabelecimento_id, status);

-- Se você quer garantir nome único por tenant (recomendado), habilite:
-- ALTER TABLE servicos
--   ADD UNIQUE KEY uk_servicos_estabelecimento_nome (estabelecimento_id, nome);

ALTER TABLE servicos
  ADD CONSTRAINT fk_servicos_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Opcional (recomendado quando você tiver certeza que já preencheu tudo):
-- ALTER TABLE servicos MODIFY estabelecimento_id BIGINT UNSIGNED NOT NULL;


-- =========================================
-- 2) FUNCIONARIO_SERVICOS
-- =========================================
ALTER TABLE funcionario_servicos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER servico_id;

-- Backfill (usa estabelecimento do funcionario)
UPDATE funcionario_servicos fs
JOIN users u ON u.id = fs.funcionario_id
SET fs.estabelecimento_id = u.estabelecimento_id
WHERE fs.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

ALTER TABLE funcionario_servicos
  ADD INDEX IF NOT EXISTS idx_fs_estabelecimento_funcionario (estabelecimento_id, funcionario_id),
  ADD INDEX IF NOT EXISTS idx_fs_estabelecimento_servico (estabelecimento_id, servico_id);

ALTER TABLE funcionario_servicos
  ADD CONSTRAINT fk_fs_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Opcional:
-- ALTER TABLE funcionario_servicos MODIFY estabelecimento_id BIGINT UNSIGNED NOT NULL;


-- =========================================
-- 3) FUNCIONARIO_PREFS
-- =========================================
ALTER TABLE funcionario_prefs
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

UPDATE funcionario_prefs fp
JOIN users u ON u.id = fp.funcionario_id
SET fp.estabelecimento_id = u.estabelecimento_id
WHERE fp.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

ALTER TABLE funcionario_prefs
  ADD INDEX IF NOT EXISTS idx_fp_estabelecimento_funcionario (estabelecimento_id, funcionario_id);

ALTER TABLE funcionario_prefs
  ADD CONSTRAINT fk_fp_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Opcional:
-- ALTER TABLE funcionario_prefs MODIFY estabelecimento_id BIGINT UNSIGNED NOT NULL;


-- =========================================
-- 4) FUNCIONARIO_AGENDA
-- =========================================
ALTER TABLE funcionario_agenda
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

UPDATE funcionario_agenda fa
JOIN users u ON u.id = fa.funcionario_id
SET fa.estabelecimento_id = u.estabelecimento_id
WHERE fa.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

ALTER TABLE funcionario_agenda
  ADD INDEX IF NOT EXISTS idx_fa_estabelecimento_funcionario (estabelecimento_id, funcionario_id);

ALTER TABLE funcionario_agenda
  ADD CONSTRAINT fk_fa_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Opcional:
-- ALTER TABLE funcionario_agenda MODIFY estabelecimento_id BIGINT UNSIGNED NOT NULL;


-- =========================================
-- 5) FUNCIONARIO_BLOQUEIOS
-- =========================================
ALTER TABLE funcionario_bloqueios
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER funcionario_id;

UPDATE funcionario_bloqueios fb
JOIN users u ON u.id = fb.funcionario_id
SET fb.estabelecimento_id = u.estabelecimento_id
WHERE fb.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

ALTER TABLE funcionario_bloqueios
  ADD INDEX IF NOT EXISTS idx_fb_estabelecimento_funcionario (estabelecimento_id, funcionario_id),
  ADD INDEX IF NOT EXISTS idx_fb_estabelecimento_periodo (estabelecimento_id, funcionario_id, start_dt, end_dt);

ALTER TABLE funcionario_bloqueios
  ADD CONSTRAINT fk_fb_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Opcional:
-- ALTER TABLE funcionario_bloqueios MODIFY estabelecimento_id BIGINT UNSIGNED NOT NULL;


-- =========================================
-- 6) AGENDAMENTOS
-- =========================================
ALTER TABLE agendamentos
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

-- Backfill pelo funcionario (agendamento pertence ao tenant do funcionario)
UPDATE agendamentos a
JOIN users u ON u.id = a.funcionario_id
SET a.estabelecimento_id = u.estabelecimento_id
WHERE a.estabelecimento_id IS NULL
  AND u.estabelecimento_id IS NOT NULL;

ALTER TABLE agendamentos
  ADD INDEX IF NOT EXISTS idx_ag_estabelecimento_inicio (estabelecimento_id, inicio),
  ADD INDEX IF NOT EXISTS idx_ag_estabelecimento_funcionario_inicio (estabelecimento_id, funcionario_id, inicio),
  ADD INDEX IF NOT EXISTS idx_ag_estabelecimento_status_inicio (estabelecimento_id, status, inicio);

ALTER TABLE agendamentos
  ADD CONSTRAINT fk_ag_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Ajuste do protocolo: o mais correto no multi-tenant é ser único por estabelecimento
-- (senão um protocolo de um salão pode "colidir" com outro)
ALTER TABLE agendamentos
  DROP INDEX uk_ag_protocolo;

ALTER TABLE agendamentos
  ADD UNIQUE KEY uk_ag_estabelecimento_protocolo (estabelecimento_id, protocolo);

-- Opcional:
-- ALTER TABLE agendamentos MODIFY estabelecimento_id BIGINT UNSIGNED NOT NULL;


-- =========================================
-- 7) AUDITORIA_LOGS
-- =========================================
ALTER TABLE auditoria_logs
  ADD COLUMN IF NOT EXISTS estabelecimento_id BIGINT UNSIGNED NULL AFTER id;

-- Backfill pelo actor (quando existir)
UPDATE auditoria_logs al
JOIN users u ON u.id = al.actor_user_id
SET al.estabelecimento_id = u.estabelecimento_id
WHERE al.estabelecimento_id IS NULL
  AND al.actor_user_id IS NOT NULL
  AND u.estabelecimento_id IS NOT NULL;

ALTER TABLE auditoria_logs
  ADD INDEX IF NOT EXISTS idx_audit_estabelecimento_created (estabelecimento_id, created_at),
  ADD INDEX IF NOT EXISTS idx_audit_estabelecimento_actor (estabelecimento_id, actor_user_id);

ALTER TABLE auditoria_logs
  ADD CONSTRAINT fk_audit_estabelecimentos
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimentos(id);

-- Observação: auditoria pode ter eventos "do sistema" sem actor_user_id,
-- então aqui faz sentido manter estabelecimento_id como NULL quando não houver contexto.