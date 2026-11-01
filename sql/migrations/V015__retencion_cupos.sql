CREATE TABLE retencion_cupo (
 id_paciente INT PRIMARY KEY,
 id_medico INT NOT NULL,
 inicio DATETIME NOT NULL,
 duracion_minutos INT NOT NULL,
 identificador VARCHAR(36) NOT NULL UNIQUE,
 vence DATETIME NOT NULL,
 UNIQUE KEY uq_retencion_horario(id_medico,inicio),
 FOREIGN KEY(id_paciente) REFERENCES paciente(id_paciente),
 FOREIGN KEY(id_medico) REFERENCES medico(id_medico),
 CHECK(duracion_minutos>0 AND duracion_minutos<=1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
