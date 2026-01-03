CREATE TABLE IF NOT EXISTS funcionario_agenda (
  funcionario_id BIGINT UNSIGNED NOT NULL,
  dia_semana TINYINT NOT NULL,
  inicio_1 TIME NULL,
  fim_1 TIME NULL,
  inicio_2 TIME NULL,
  fim_2 TIME NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (funcionario_id, dia_semana),
  CONSTRAINT fk_func_agenda_user
    FOREIGN KEY (funcionario_id)
    REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_func_agenda_funcionario
  ON funcionario_agenda(funcionario_id);

CREATE TABLE IF NOT EXISTS funcionario_bloqueios (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  funcionario_id BIGINT UNSIGNED NOT NULL,
  start_dt DATETIME NOT NULL,
  end_dt DATETIME NOT NULL,
  motivo VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_func_bloq_user
    FOREIGN KEY (funcionario_id)
    REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_func_bloq_funcionario
  ON funcionario_bloqueios(funcionario_id);

CREATE INDEX idx_func_bloq_periodo
  ON funcionario_bloqueios(funcionario_id, start_dt, end_dt);
