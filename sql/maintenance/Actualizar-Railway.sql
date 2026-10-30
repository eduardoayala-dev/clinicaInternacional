USE railway;
SET NAMES utf8mb4;


SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN canal_aviso VARCHAR(20) DEFAULT ''WhatsApp''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'canal_aviso'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN sede_preferida VARCHAR(50) DEFAULT ''Sede San Borja''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'sede_preferida'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN alergias VARCHAR(100) DEFAULT ''''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'alergias'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN tipo_cobertura VARCHAR(50) DEFAULT ''EPS''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'tipo_cobertura'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET NAMES utf8mb4;

ALTER TABLE lista_espera MODIFY COLUMN id_sede INT NULL DEFAULT NULL;
ALTER TABLE lista_espera MODIFY COLUMN canal_notificacion VARCHAR(40) NOT NULL DEFAULT 'CORREO';

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE reasignacion_inteligente ADD COLUMN id_cita_asignada INT NULL DEFAULT NULL, ADD CONSTRAINT fk_reasig_asignada FOREIGN KEY (id_cita_asignada) REFERENCES cita(id_cita)',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'reasignacion_inteligente' AND column_name = 'id_cita_asignada'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lista_espera ADD COLUMN anticipacion_minutos INT NOT NULL DEFAULT 180',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'lista_espera' AND column_name = 'anticipacion_minutos'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lista_espera ADD COLUMN id_medico_preferido INT NULL DEFAULT NULL, ADD CONSTRAINT fk_lista_medico_preferido FOREIGN KEY (id_medico_preferido) REFERENCES medico(id_medico)',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'lista_espera' AND column_name = 'id_medico_preferido'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS admision_cita (
    id_cita INT PRIMARY KEY,
    id_usuario_personal INT NOT NULL,
    fecha_llegada DATETIME NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL,
    llego_tarde BOOLEAN NOT NULL,
    CONSTRAINT fk_admision_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_admision_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET NAMES utf8mb4;

ALTER TABLE cita MODIFY COLUMN estado ENUM('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION','ATENDIDA','NO_ATENDIDA','NO_ASISTIO','REPROGRAMADA','CANCELADA') DEFAULT 'SOLICITADA';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS inasistencia_cita (
    id_cita INT PRIMARY KEY,
    id_usuario_personal INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL,
    CONSTRAINT fk_inasistencia_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_inasistencia_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'CREATE INDEX idx_cita_medico_paciente_fecha ON cita(id_medico,id_paciente,fecha_hora)',
        'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'cita'
      AND index_name = 'idx_cita_medico_paciente_fecha'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

CREATE TABLE IF NOT EXISTS sesion_autenticacion (
    id_sesion CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    id_usuario INT NOT NULL,
    refresh_hash BINARY(32) NOT NULL,
    csrf_token CHAR(43) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rol_inicial VARCHAR(32) NOT NULL,
    credencial_hash BINARY(32) NOT NULL,
    creada_en DATETIME(6) NOT NULL,
    ultima_actividad_en DATETIME(6) NOT NULL,
    vence_en DATETIME(6) NOT NULL,
    revocada_en DATETIME(6) NULL,
    CONSTRAINT fk_sesion_auth_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    UNIQUE KEY uk_sesion_refresh (refresh_hash),
    INDEX idx_sesion_usuario (id_usuario,revocada_en),
    INDEX idx_sesion_vencimiento (vence_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT 'Actualizacion completada: comprobar estructura y flujos' AS resultado;
