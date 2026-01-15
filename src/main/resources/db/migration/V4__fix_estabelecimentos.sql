-- V4__fix_estabelecimentos_after_failed_v3.sql
-- Corrige inconsistências após V3 falhada (salao -> estabelecimento)
-- DB: MariaDB 10.11+

DELIMITER $$

CREATE PROCEDURE fix_estabelecimentos_v4()
BEGIN
  DECLARE cnt INT DEFAULT 0;
  DECLARE fkname VARCHAR(128);

  -- 1) Se ainda existir saloes e não existir estabelecimentos, renomeia
  SELECT COUNT(*) INTO cnt
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'estabelecimentos';

  IF cnt = 0 THEN
    SELECT COUNT(*) INTO cnt
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'saloes';

    IF cnt > 0 THEN
      SET @sql = 'RENAME TABLE `saloes` TO `estabelecimentos`';
      PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
  END IF;

  -- 2) Se ainda existir salao_pre_cadastros e não existir estabelecimento_pre_cadastros, renomeia
  SELECT COUNT(*) INTO cnt
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'estabelecimento_pre_cadastros';

  IF cnt = 0 THEN
    SELECT COUNT(*) INTO cnt
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'salao_pre_cadastros';

    IF cnt > 0 THEN
      SET @sql = 'RENAME TABLE `salao_pre_cadastros` TO `estabelecimento_pre_cadastros`';
      PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
  END IF;

  -- 3) Renomear coluna users.salao_id -> users.estabelecimento_id (se ainda não foi)
  SELECT COUNT(*) INTO cnt
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'estabelecimento_id';

  IF cnt = 0 THEN
    SELECT COUNT(*) INTO cnt
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'salao_id';

    IF cnt > 0 THEN
      SET @sql = 'ALTER TABLE `users` RENAME COLUMN `salao_id` TO `estabelecimento_id`';
      PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
  END IF;

  -- 4) Garantir índice novo (e remover o antigo, se existir)
  SELECT COUNT(*) INTO cnt
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND index_name = 'idx_users_estabelecimento_perfil_ativo';

  IF cnt = 0 THEN
    SET @sql = 'ALTER TABLE `users` ADD KEY `idx_users_estabelecimento_perfil_ativo` (`estabelecimento_id`,`perfil`,`ativo`)';
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  SELECT COUNT(*) INTO cnt
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND index_name = 'idx_users_salao_perfil_ativo';

  IF cnt > 0 THEN
    SET @sql = 'ALTER TABLE `users` DROP INDEX `idx_users_salao_perfil_ativo`';
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  -- 5) Remover FK antiga do tenant (se existir) e garantir a nova
  -- Descobrir se existe FK em users que aponta para saloes/estabelecimentos pela coluna (salao_id/estabelecimento_id)
  -- Drop FK antiga: fk_users_salao (se existir)
  SELECT COUNT(*) INTO cnt
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'users'
    AND constraint_name = 'fk_users_salao'
    AND constraint_type = 'FOREIGN KEY';

  IF cnt > 0 THEN
    SET @sql = 'ALTER TABLE `users` DROP FOREIGN KEY `fk_users_salao`';
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  -- Drop qualquer FK existente na coluna estabelecimento_id (pra recriar do jeito certo), se necessário
  SELECT rc.constraint_name INTO fkname
  FROM information_schema.key_column_usage kcu
  JOIN information_schema.referential_constraints rc
    ON rc.constraint_schema = kcu.constraint_schema
   AND rc.constraint_name = kcu.constraint_name
  WHERE kcu.table_schema = DATABASE()
    AND kcu.table_name = 'users'
    AND kcu.column_name = 'estabelecimento_id'
  LIMIT 1;

  IF fkname IS NOT NULL AND fkname <> 'fk_users_estabelecimento' THEN
    SET @sql = CONCAT('ALTER TABLE `users` DROP FOREIGN KEY `', fkname, '`');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  -- Garantir FK nova: fk_users_estabelecimento
  SELECT COUNT(*) INTO cnt
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'users'
    AND constraint_name = 'fk_users_estabelecimento'
    AND constraint_type = 'FOREIGN KEY';

  IF cnt = 0 THEN
    SET @sql = 'ALTER TABLE `users` ADD CONSTRAINT `fk_users_estabelecimento` FOREIGN KEY (`estabelecimento_id`) REFERENCES `estabelecimentos` (`id`)';
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

END$$

DELIMITER ;

CALL fix_estabelecimentos_v4();

DROP PROCEDURE fix_estabelecimentos_v4;
