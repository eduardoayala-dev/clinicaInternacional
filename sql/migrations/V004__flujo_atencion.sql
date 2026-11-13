SET NAMES utf8mb4;

ALTER TABLE cita MODIFY COLUMN estado ENUM('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION','ATENDIDA','NO_ASISTIO','NO_ATENDIDA','REPROGRAMADA','CANCELADA') DEFAULT 'SOLICITADA';

CREATE TABLE IF NOT EXISTS atencion_cita (
    id_cita INT PRIMARY KEY,
    id_medico INT NOT NULL,
    id_paciente INT NOT NULL,
    fecha_inicio DATETIME NULL,
    fecha_fin DATETIME NULL,
    motivo_no_atencion VARCHAR(200) NULL,
    medico_activo INT GENERATED ALWAYS AS (CASE WHEN fecha_inicio IS NOT NULL AND fecha_fin IS NULL THEN id_medico ELSE NULL END),
    paciente_activo INT GENERATED ALWAYS AS (CASE WHEN fecha_inicio IS NOT NULL AND fecha_fin IS NULL THEN id_paciente ELSE NULL END),
    CONSTRAINT uk_atencion_medico_activo UNIQUE (medico_activo),
    CONSTRAINT uk_atencion_paciente_activo UNIQUE (paciente_activo),
    CONSTRAINT fk_atencion_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_atencion_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
    CONSTRAINT fk_atencion_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT ck_atencion_fechas CHECK (fecha_inicio IS NOT NULL OR fecha_fin IS NOT NULL),
    CONSTRAINT ck_atencion_orden CHECK (fecha_inicio IS NULL OR fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inasistencia_cita (
    id_cita INT PRIMARY KEY,
    id_usuario_personal INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL,
    CONSTRAINT fk_inasistencia_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_inasistencia_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;
