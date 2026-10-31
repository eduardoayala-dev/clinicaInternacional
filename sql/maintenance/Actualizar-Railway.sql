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

CREATE TABLE IF NOT EXISTS verificacion_cuenta (
    id_verificacion CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    id_usuario INT NOT NULL,
    proposito VARCHAR(24) NOT NULL,
    id_sesion CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    vinculo_hash BINARY(32) NOT NULL,
    credencial_hash BINARY(32) NOT NULL,
    correo_hash BINARY(32) NOT NULL,
    codigo_hash BINARY(32) NULL,
    etapa VARCHAR(24) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    correo_nuevo VARCHAR(254) NULL,
    creada_en DATETIME(6) NOT NULL,
    vence_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_verificacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_verificacion_intentos CHECK (intentos BETWEEN 0 AND 5),
    CONSTRAINT chk_verificacion_proposito CHECK (proposito IN ('RECUPERAR','CAMBIAR_PASSWORD','CAMBIAR_CORREO')),
    CONSTRAINT chk_verificacion_etapa CHECK (etapa IN ('CODIGO_ACTUAL','AUTORIZADO','CODIGO_NUEVO','FINALIZADO','BLOQUEADO')),
    INDEX idx_verificacion_usuario (id_usuario,proposito),
    INDEX idx_verificacion_vence (vence_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS limite_recuperacion (
    clave_hash BINARY(32) PRIMARY KEY,
    solicitudes INT NOT NULL,
    vence_en DATETIME(6) NOT NULL,
    INDEX idx_limite_recuperacion_vence (vence_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS correo_cita (
    id_cita INT PRIMARY KEY,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos SMALLINT NOT NULL DEFAULT 0,
    disponible_en DATETIME(6) NOT NULL,
    iniciada_en DATETIME(6) NULL,
    finalizada_en DATETIME(6) NULL,
    CONSTRAINT fk_correo_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT chk_correo_cita_estado CHECK (estado IN ('PENDIENTE','ENVIANDO','ENVIADO','NO_CONFIRMADO','OMITIDO')),
    CONSTRAINT chk_correo_cita_intentos CHECK (intentos BETWEEN 0 AND 3),
    INDEX idx_correo_cita_pendiente (estado,disponible_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS propuesta_horario (
    id_propuesta INT AUTO_INCREMENT PRIMARY KEY,
    id_medico INT NOT NULL,
    id_sede INT NOT NULL,
    dias_semana INT NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    duracion_minutos INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creada_en DATETIME(6) NOT NULL,
    revisada_en DATETIME(6) NULL,
    id_revisor INT NULL,
    motivo_revision VARCHAR(500) NULL,
    CONSTRAINT fk_propuesta_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
    CONSTRAINT fk_propuesta_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede),
    CONSTRAINT fk_propuesta_revisor FOREIGN KEY (id_revisor) REFERENCES usuario(id_usuario),
    CONSTRAINT ck_propuesta_dias CHECK (dias_semana BETWEEN 1 AND 127),
    CONSTRAINT ck_propuesta_vigencia CHECK (vigente_hasta >= vigente_desde),
    CONSTRAINT ck_propuesta_horas CHECK (hora_fin > hora_inicio AND duracion_minutos > 0),
    CONSTRAINT ck_propuesta_estado CHECK (estado IN ('PENDIENTE','PUBLICADO','RECHAZADO')),
    INDEX idx_propuesta_medico (id_medico, id_propuesta),
    INDEX idx_propuesta_revision (id_sede, estado, id_propuesta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @agregar_propuesta = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='disponibilidad' AND column_name='id_propuesta'),
    'SELECT 1',
    'ALTER TABLE disponibilidad ADD COLUMN id_propuesta INT NULL, ADD CONSTRAINT fk_disponibilidad_propuesta FOREIGN KEY (id_propuesta) REFERENCES propuesta_horario(id_propuesta)'
);
PREPARE migracion_propuesta FROM @agregar_propuesta;
EXECUTE migracion_propuesta;
DEALLOCATE PREPARE migracion_propuesta;
