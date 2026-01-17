ALTER TABLE `estabelecimentos`
ADD COLUMN `endereco` VARCHAR(255) NULL AFTER `ramo_atuacao`,
ADD COLUMN `numero` VARCHAR(20) NULL AFTER `endereco`,
ADD COLUMN `bairro` VARCHAR(100) NULL AFTER `numero`,
ADD COLUMN `estado` CHAR(2) NULL AFTER `bairro`,
ADD COLUMN `cidade` VARCHAR(100) NULL AFTER `estado`,
ADD COLUMN `telefone` VARCHAR(20) NULL AFTER `nome`;