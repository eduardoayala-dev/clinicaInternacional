SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS consulta_clinica (
    id_cita INT PRIMARY KEY,
    motivo VARCHAR(1000) NOT NULL,
    antecedentes VARCHAR(2000) NOT NULL,
    hallazgos VARCHAR(2000) NOT NULL,
    evaluacion VARCHAR(2000) NOT NULL,
    diagnostico VARCHAR(1000) NOT NULL,
    plan VARCHAR(2000) NOT NULL,
    CONSTRAINT fk_consulta_clinica_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS seguimiento_cita (
    id_cita_origen INT PRIMARY KEY,
    id_cita_control INT NOT NULL,
    id_usuario_medico INT NOT NULL,
    fecha_vinculacion DATETIME NOT NULL,
    CONSTRAINT fk_seguimiento_origen FOREIGN KEY (id_cita_origen) REFERENCES cita(id_cita),
    CONSTRAINT fk_seguimiento_control FOREIGN KEY (id_cita_control) REFERENCES cita(id_cita),
    CONSTRAINT fk_seguimiento_responsable FOREIGN KEY (id_usuario_medico) REFERENCES usuario(id_usuario),
    CONSTRAINT ck_seguimiento_distinto CHECK (id_cita_origen <> id_cita_control),
    INDEX idx_seguimiento_control (id_cita_control)
) ENGINE=InnoDB;

CREATE INDEX idx_cita_medico_paciente_fecha ON cita(id_medico,id_paciente,fecha_hora);
