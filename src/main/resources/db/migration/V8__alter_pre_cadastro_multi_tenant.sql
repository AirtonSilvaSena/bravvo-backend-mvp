-- Flyway (use o próximo número da sua sequência)
-- Ex: V12__multitenant_email_por_estabelecimento.sql

/* =========================================================
   0) PRE-CHECK (opcional, mas recomendado)
   - Garanta que não existam duplicidades dentro do mesmo estabelecimento
   ========================================================= */
-- Duplicidade de email dentro do mesmo estabelecimento
-- SELECT estabelecimento_id, email, COUNT(*) qtd
-- FROM users
-- GROUP BY estabelecimento_id, email
-- HAVING COUNT(*) > 1;

-- Duplicidade de telefone dentro do mesmo estabelecimento (se você já tiver dados)
-- SELECT estabelecimento_id, telefone, COUNT(*) qtd
-- FROM users
-- WHERE telefone IS NOT NULL AND telefone <> ''
-- GROUP BY estabelecimento_id, telefone
-- HAVING COUNT(*) > 1;


/* =========================================================
   1) PRE-CADASTRO
   - Permitir mesmo email em slugs diferentes
   - Manter slug único
   ========================================================= */
ALTER TABLE estabelecimentos_pre_cadastros
  DROP INDEX uk_est_pre_email;

/* uk_est_pre_slug permanece */


/* =========================================================
   2) USERS
   - Email obrigatório
   - Remover UNIQUE global(email)
   - Criar UNIQUE por estabelecimento (email e telefone)
   ========================================================= */
ALTER TABLE users
  MODIFY email VARCHAR(180) NOT NULL;

ALTER TABLE users
  DROP INDEX uk_users_email;

ALTER TABLE users
  ADD UNIQUE KEY uk_users_estabelecimento_email (estabelecimento_id, email);

-- Se telefone também for obrigatório para todos, você pode tornar NOT NULL também.
-- Por enquanto deixo como está (DEFAULT NULL) mas com UNIQUE por estabelecimento.
ALTER TABLE users
  ADD UNIQUE KEY uk_users_estabelecimento_telefone (estabelecimento_id, telefone);