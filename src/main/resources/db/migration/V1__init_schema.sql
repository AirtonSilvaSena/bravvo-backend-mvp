


-- Copiando estrutura do banco de dados para bravvo_mvp
CREATE DATABASE IF NOT EXISTS `bravvo_mvp` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;
USE `bravvo_mvp`;

-- Copiando estrutura para tabela bravvo_mvp.agendamentos
CREATE TABLE IF NOT EXISTS `agendamentos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `protocolo` varchar(30) NOT NULL,
  `tipo` varchar(50) NOT NULL DEFAULT 'hora_marcada',
  `servico_id` bigint(20) unsigned NOT NULL,
  `funcionario_id` bigint(20) unsigned NOT NULL,
  `cliente_id` bigint(20) unsigned DEFAULT NULL,
  `cliente_nome` varchar(120) DEFAULT NULL,
  `cliente_telefone` varchar(30) DEFAULT NULL,
  `cliente_email` varchar(180) DEFAULT NULL,
  `inicio` datetime NOT NULL,
  `fim` datetime NOT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'pendente',
  `observacoes` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ag_protocolo` (`protocolo`),
  KEY `idx_ag_funcionario_inicio` (`funcionario_id`,`inicio`),
  KEY `idx_ag_status_inicio` (`status`,`inicio`),
  KEY `idx_ag_cliente_id` (`cliente_id`),
  KEY `fk_ag_servico` (`servico_id`),
  CONSTRAINT `fk_ag_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_ag_funcionario` FOREIGN KEY (`funcionario_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_ag_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.auditoria_logs
CREATE TABLE IF NOT EXISTS `auditoria_logs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `actor_user_id` bigint(20) unsigned DEFAULT NULL,
  `acao` varchar(80) NOT NULL,
  `entidade` varchar(80) NOT NULL,
  `entidade_id` varchar(80) DEFAULT NULL,
  `detalhes_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`detalhes_json`)),
  `ip` varchar(60) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_actor` (`actor_user_id`),
  CONSTRAINT `fk_audit_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.configuracoes
CREATE TABLE IF NOT EXISTS `configuracoes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `cfg_key` varchar(120) NOT NULL,
  `cfg_value` text NOT NULL,
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cfg_key` (`cfg_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.fila_espera
CREATE TABLE IF NOT EXISTS `fila_espera` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `protocolo` varchar(30) NOT NULL,
  `servico_id` bigint(20) unsigned DEFAULT NULL,
  `funcionario_id` bigint(20) unsigned DEFAULT NULL,
  `cliente_id` bigint(20) unsigned DEFAULT NULL,
  `cliente_nome` varchar(120) NOT NULL,
  `cliente_telefone` varchar(30) NOT NULL,
  `cliente_email` varchar(180) DEFAULT NULL,
  `status` enum('aguardando','em_atendimento','concluido','cancelado') NOT NULL DEFAULT 'aguardando',
  `chamado_em` datetime DEFAULT NULL,
  `concluido_em` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fila_protocolo` (`protocolo`),
  KEY `idx_fila_status_created` (`status`,`created_at`),
  KEY `idx_fila_funcionario` (`funcionario_id`),
  KEY `fk_fila_servico` (`servico_id`),
  KEY `fk_fila_cliente` (`cliente_id`),
  CONSTRAINT `fk_fila_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_fila_funcionario` FOREIGN KEY (`funcionario_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_fila_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.flyway_schema_history
CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.funcionario_agenda
CREATE TABLE IF NOT EXISTS `funcionario_agenda` (
  `funcionario_id` bigint(20) unsigned NOT NULL,
  `dia_semana` int(11) NOT NULL,
  `inicio_1` time DEFAULT NULL,
  `fim_1` time DEFAULT NULL,
  `inicio_2` time DEFAULT NULL,
  `fim_2` time DEFAULT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT 1,
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`funcionario_id`,`dia_semana`),
  KEY `idx_func_agenda_funcionario` (`funcionario_id`),
  CONSTRAINT `fk_func_agenda_user` FOREIGN KEY (`funcionario_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.funcionario_bloqueios
CREATE TABLE IF NOT EXISTS `funcionario_bloqueios` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `funcionario_id` bigint(20) unsigned NOT NULL,
  `start_dt` datetime NOT NULL,
  `end_dt` datetime NOT NULL,
  `motivo` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_func_bloq_funcionario` (`funcionario_id`),
  KEY `idx_func_bloq_periodo` (`funcionario_id`,`start_dt`,`end_dt`),
  CONSTRAINT `fk_func_bloq_user` FOREIGN KEY (`funcionario_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.funcionario_prefs
CREATE TABLE IF NOT EXISTS `funcionario_prefs` (
  `funcionario_id` bigint(20) unsigned NOT NULL,
  `prefs_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`prefs_json`)),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`funcionario_id`),
  CONSTRAINT `fk_prefs_funcionario` FOREIGN KEY (`funcionario_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.funcionario_servicos
CREATE TABLE IF NOT EXISTS `funcionario_servicos` (
  `funcionario_id` bigint(20) unsigned NOT NULL,
  `servico_id` bigint(20) unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`funcionario_id`,`servico_id`),
  KEY `fk_fs_servico` (`servico_id`),
  CONSTRAINT `fk_fs_funcionario` FOREIGN KEY (`funcionario_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_fs_servico` FOREIGN KEY (`servico_id`) REFERENCES `servicos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.protocolos
CREATE TABLE IF NOT EXISTS `protocolos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(30) NOT NULL,
  `tipo` varchar(50) NOT NULL DEFAULT '',
  `dados_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`dados_json`)),
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_protocolos_codigo` (`codigo`),
  KEY `idx_protocolos_tipo_created` (`tipo`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.refresh_tokens
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `token_hash` varchar(255) NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_rt_user` (`user_id`),
  KEY `idx_rt_expires` (`expires_at`),
  CONSTRAINT `fk_rt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.servicos
CREATE TABLE IF NOT EXISTS `servicos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `nome` varchar(120) NOT NULL,
  `descricao` varchar(500) DEFAULT NULL,
  `duracao_min` int(11) NOT NULL,
  `valor` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'ativo',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_servicos_status` (`status`),
  KEY `idx_servicos_nome` (`nome`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

-- Copiando estrutura para tabela bravvo_mvp.users
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `nome` varchar(120) NOT NULL,
  `email` varchar(180) DEFAULT NULL,
  `telefone` varchar(30) DEFAULT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `perfil` varchar(50) NOT NULL DEFAULT '',
  `ativo` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_perfil_ativo` (`perfil`,`ativo`),
  KEY `idx_users_telefone` (`telefone`),
  CONSTRAINT `chk_users_perfil` CHECK (`perfil` in ('ADMIN','FUNCIONARIO','CLIENTE'))
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exportação de dados foi desmarcado.

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
