-- =============================================================================
-- ESQUEMA DE BASE DE DATOS - CLÍNICA INTERNACIONAL (MySQL 8.0)
-- Requisitos: HU003, RNF-01, RNF-02, RNF-03, RNF-04, RNF-05
-- =============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS clinica_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinica_db;

-- Desactivar temporalmente llaves foráneas para recreación limpia
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reporte;
DROP TABLE IF EXISTS indicador;
DROP TABLE IF EXISTS reasignacion_inteligente;
DROP TABLE IF EXISTS correo_cita;
DROP TABLE IF EXISTS notificacion;
DROP TABLE IF EXISTS historial_asistencia;
DROP TABLE IF EXISTS lista_espera;
DROP TABLE IF EXISTS seguimiento_cita;
DROP TABLE IF EXISTS consulta_clinica;
DROP TABLE IF EXISTS atencion_cita;
DROP TABLE IF EXISTS inasistencia_cita;
DROP TABLE IF EXISTS admision_cita;
DROP TABLE IF EXISTS cita;
DROP TABLE IF EXISTS disponibilidad;
DROP TABLE IF EXISTS motivo_cita;
DROP TABLE IF EXISTS propuesta_horario;
DROP TABLE IF EXISTS medico;
DROP TABLE IF EXISTS personal_administrativo;
DROP TABLE IF EXISTS paciente;
DROP TABLE IF EXISTS administrador;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS especialidad;
DROP TABLE IF EXISTS sede;
DROP TABLE IF EXISTS priorizacion_config;
DROP TABLE IF EXISTS auditoria_seguridad;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 1. CLASE/TABLA: usuario (Autenticación y Perfil Base)
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol ENUM('PACIENTE', 'MEDICO', 'PERSONAL_ADMINISTRATIVO', 'ADMINISTRADOR') NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    activo TINYINT(1) DEFAULT 1,
    INDEX idx_usuario_email (email),
    INDEX idx_usuario_rol (rol)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 2. CLASE/TABLA: paciente
-- -----------------------------------------------------------------------------
CREATE TABLE paciente (
    id_paciente INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    dni VARCHAR(15) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    canal_aviso VARCHAR(20) DEFAULT 'WhatsApp',
    sede_preferida VARCHAR(50) DEFAULT 'Sede San Borja',
    alergias VARCHAR(100) DEFAULT NULL,
    tipo_cobertura VARCHAR(50) DEFAULT 'EPS',
    CONSTRAINT fk_paciente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    INDEX idx_paciente_dni (dni)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 3. CLASE/TABLA: medico
-- -----------------------------------------------------------------------------
CREATE TABLE medico (
    id_medico INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    cmp VARCHAR(20) NOT NULL UNIQUE,
    id_especialidad INT NOT NULL,
    id_sede INT NOT NULL,
    estado ENUM('ACTIVO', 'LICENCIA', 'INACTIVO') DEFAULT 'ACTIVO',
    CONSTRAINT fk_medico_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    INDEX idx_medico_cmp (cmp)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 4. CLASE/TABLA: personal_administrativo
-- -----------------------------------------------------------------------------
CREATE TABLE personal_administrativo (
    id_personal INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    id_sede INT NOT NULL,
    codigo_empleado VARCHAR(20) NOT NULL UNIQUE,
    cargo VARCHAR(100) NOT NULL,
    CONSTRAINT fk_personal_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 5. CLASE/TABLA: administrador
-- -----------------------------------------------------------------------------
CREATE TABLE administrador (
    id_administrador INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    CONSTRAINT fk_admin_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 6. CLASE/TABLA: sede
-- -----------------------------------------------------------------------------
CREATE TABLE sede (
    id_sede INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    imagen_url VARCHAR(255) DEFAULT NULL,
    estado ENUM('ACTIVO', 'MANTENIMIENTO', 'INACTIVO') DEFAULT 'ACTIVO',
    INDEX idx_sede_codigo (codigo)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 7. CLASE/TABLA: especialidad
-- -----------------------------------------------------------------------------
CREATE TABLE especialidad (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT DEFAULT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO'
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 8. CLASE/TABLA: motivo_cita
-- -----------------------------------------------------------------------------
CREATE TABLE motivo_cita (
    id_motivo INT AUTO_INCREMENT PRIMARY KEY,
    id_especialidad INT NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO',
    CONSTRAINT fk_motivo_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 9. CLASE/TABLA: disponibilidad
-- -----------------------------------------------------------------------------
CREATE TABLE propuesta_horario (
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


CREATE TABLE disponibilidad (
    id_propuesta INT NULL,
    CONSTRAINT fk_disponibilidad_propuesta FOREIGN KEY (id_propuesta) REFERENCES propuesta_horario(id_propuesta),
    id_disponibilidad INT AUTO_INCREMENT PRIMARY KEY,
    id_medico INT NOT NULL,
    id_sede INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    duracion_minutos INT DEFAULT 30,
    estado ENUM('PENDIENTE', 'PUBLICADO', 'RECHAZADO', 'CANCELADO') DEFAULT 'PENDIENTE',
    CONSTRAINT fk_disp_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
    CONSTRAINT fk_disp_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 10. CLASE/TABLA: cita
-- -----------------------------------------------------------------------------
CREATE TABLE cita (
    id_cita INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_medico INT NOT NULL,
    id_disponibilidad INT NOT NULL,
    id_motivo INT DEFAULT NULL,
    fecha_hora DATETIME NOT NULL,
    estado ENUM('SOLICITADA', 'CONFIRMADA', 'EN_SALA', 'EN_ATENCION', 'ATENDIDA', 'NO_ATENDIDA', 'NO_ASISTIO', 'REPROGRAMADA', 'CANCELADA') DEFAULT 'SOLICITADA',
    nivel_urgencia ENUM('ALTA', 'MEDIA', 'BAJA') DEFAULT 'BAJA',
    fecha_recomendada_proxima DATE DEFAULT NULL,
    riesgo_cancelacion ENUM('BAJO', 'MEDIO', 'ALTO', 'SIN_HISTORIAL') DEFAULT 'SIN_HISTORIAL',
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT fk_cita_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
    CONSTRAINT fk_cita_disponibilidad FOREIGN KEY (id_disponibilidad) REFERENCES disponibilidad(id_disponibilidad),
    CONSTRAINT fk_cita_motivo FOREIGN KEY (id_motivo) REFERENCES motivo_cita(id_motivo)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 11. CLASE/TABLA: lista_espera
-- -----------------------------------------------------------------------------
CREATE TABLE lista_espera (
    id_lista_espera INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_especialidad INT NOT NULL,
    id_sede INT DEFAULT NULL,
    id_motivo INT DEFAULT NULL,
    nivel_urgencia ENUM('ALTA', 'MEDIA', 'BAJA') DEFAULT 'BAJA',
    rango_horario ENUM('MANANA', 'TARDE', 'NOCHE', 'INDIFERENTE') DEFAULT 'INDIFERENTE',
    canal_notificacion VARCHAR(40) NOT NULL DEFAULT 'CORREO',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('ACTIVA', 'OFERTADA', 'ATENDIDA', 'CANCELADA') DEFAULT 'ACTIVA',
    posicion INT DEFAULT 0,
    anticipacion_minutos INT NOT NULL DEFAULT 180,
    id_medico_preferido INT DEFAULT NULL,
    CONSTRAINT fk_lista_medico_preferido FOREIGN KEY (id_medico_preferido) REFERENCES medico(id_medico),
    CONSTRAINT fk_lista_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT fk_lista_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad),
    CONSTRAINT fk_lista_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
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

-- 12. CLASE/TABLA: notificacion
-- -----------------------------------------------------------------------------
CREATE TABLE notificacion (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_cita INT DEFAULT NULL,
    tipo VARCHAR(50) NOT NULL,
    canal ENUM('CORREO', 'SMS', 'PLATAFORMA') DEFAULT 'PLATAFORMA',
    mensaje TEXT NOT NULL,
    fecha_envio DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado_envio ENUM('ENVIADA', 'ENTREGADA', 'LEIDA', 'FALLIDA') DEFAULT 'ENVIADA',
    leida TINYINT(1) DEFAULT 0,
    CONSTRAINT fk_notif_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_notif_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita) ON DELETE SET NULL
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 13. CLASE/TABLA: historial (historial_asistencia)
-- -----------------------------------------------------------------------------
CREATE TABLE admision_cita (
    id_cita INT PRIMARY KEY,
    id_usuario_personal INT NOT NULL,
    fecha_llegada DATETIME NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL,
    llego_tarde BOOLEAN NOT NULL,
    CONSTRAINT fk_admision_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_admision_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE atencion_cita (
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

CREATE TABLE inasistencia_cita (
    id_cita INT PRIMARY KEY,
    id_usuario_personal INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL,
    CONSTRAINT fk_inasistencia_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_inasistencia_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE historial_asistencia (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_cita INT NOT NULL,
    asistio TINYINT(1) NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    observacion VARCHAR(255) DEFAULT NULL,
    CONSTRAINT fk_hist_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT fk_hist_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 14. CLASE/TABLA: reasignacion_inteligente
-- -----------------------------------------------------------------------------
CREATE TABLE reasignacion_inteligente (
    id_reasignacion INT AUTO_INCREMENT PRIMARY KEY,
    id_cita_original INT NOT NULL,
    id_lista_espera INT NOT NULL,
    id_disponibilidad INT NOT NULL,
    fecha_oferta DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_limite_respuesta DATETIME NOT NULL,
    estado ENUM('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'EXPIRADA', 'CANCELADA') DEFAULT 'PENDIENTE',
    motivo VARCHAR(255) DEFAULT NULL,
    id_cita_asignada INT DEFAULT NULL,
    CONSTRAINT fk_reasig_asignada FOREIGN KEY (id_cita_asignada) REFERENCES cita(id_cita),
    CONSTRAINT fk_reasig_cita FOREIGN KEY (id_cita_original) REFERENCES cita(id_cita),
    CONSTRAINT fk_reasig_lista FOREIGN KEY (id_lista_espera) REFERENCES lista_espera(id_lista_espera),
    CONSTRAINT fk_reasig_disp FOREIGN KEY (id_disponibilidad) REFERENCES disponibilidad(id_disponibilidad)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 15. CLASE/TABLA: priorizacion (priorizacion_config)
-- -----------------------------------------------------------------------------
CREATE TABLE priorizacion_config (
    id_config INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    criterio VARCHAR(100) NOT NULL,
    peso DECIMAL(5,2) DEFAULT 1.0,
    nivel ENUM('ALTA', 'MEDIA', 'BAJA') DEFAULT 'MEDIA',
    prioridad_activa TINYINT(1) DEFAULT 1,
    tiempo_limite_oferta_minutos INT DEFAULT 15,
    candidatos_simultaneos INT DEFAULT 1,
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 16. CLASE/TABLA: indicador (KPIs Operativos del Sistema)
-- -----------------------------------------------------------------------------
CREATE TABLE indicador (
    id_indicador INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    periodo VARCHAR(50) NOT NULL,
    fecha_calculo DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO'
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 17. CLASE/TABLA: reporte (Generación de Reportes Ejecutivos)
-- -----------------------------------------------------------------------------
CREATE TABLE reporte (
    id_reporte INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    formato ENUM('PDF', 'EXCEL', 'CSV') DEFAULT 'PDF',
    fecha_generacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NOT NULL,
    CONSTRAINT fk_reporte_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 18. CLASE/TABLA: auditoria_seguridad (Para registro de eventos de log en AuthService)
-- -----------------------------------------------------------------------------
CREATE TABLE auditoria_seguridad (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT,
    ip_origen VARCHAR(45) NOT NULL,
    evento VARCHAR(100) NOT NULL,
    descripcion TEXT,
    sospechoso TINYINT(1) DEFAULT 0,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================================
-- DATOS SINTÉTICOS DE PRUEBA INITIAL SEED (CLÍNICA INTERNACIONAL - SEDE SAN BORJA)
-- =============================================================================

-- 1. SEDE (ÚNICA SEDE ACTIVA: SAN BORJA)
INSERT INTO sede (id_sede, codigo, nombre, direccion, telefono, estado) VALUES
(1, 'SED-SB', 'Sede San Borja', 'Av. Guardia Civil 385, San Borja, Lima', '01-610-2222', 'ACTIVO');

-- 2. ESPECIALIDADES MÉDICAS (12 ESPECIALIDADES INSTITUCIONALES)
INSERT INTO especialidad (id_especialidad, nombre, descripcion, estado) VALUES
(1, 'Cardiología', 'Diagnóstico y tratamiento integral de salud cardiovascular, arritmias e hipertensión.', 'ACTIVO'),
(2, 'Pediatría', 'Atención médica integral pediátrica, control de crecimiento y vacunas.', 'ACTIVO'),
(3, 'Dermatología', 'Cuidado y diagnóstico clínico de piel, lunares, acné, cabello y uñas.', 'ACTIVO'),
(4, 'Medicina Interna', 'Atención integral del adulto, enfermedades metabólicas y chequeos preventivos.', 'ACTIVO'),
(5, 'Traumatología y Ortopedia', 'Manejo de lesiones articulares, columna, fracturas y medicina deportiva.', 'ACTIVO'),
(6, 'Ginecología y Obstetricia', 'Salud integral de la mujer, control ginecológico anual y salud prenatal.', 'ACTIVO'),
(7, 'Oftalmología', 'Salud ocular integral, refracción, glaucoma, cataratas y fondo de ojo.', 'ACTIVO'),
(8, 'Gastroenterología', 'Aparato digestivo, gastritis, colon irritable, reflujo y endoscopías.', 'ACTIVO'),
(9, 'Neurología', 'Tratamiento de cefaleas, migrañas, trastornos del sueño y neuropatías.', 'ACTIVO'),
(10, 'Otorrinolaringología', 'Patologías de oído, nariz, garganta, rinitis, sinusitis y vértigo.', 'ACTIVO'),
(11, 'Endocrinología', 'Control de tiroides, diabetes mellitus, obesidad y metabolismo.', 'ACTIVO'),
(12, 'Urología', 'Salud urológica integral, despistaje prostático y litiasis renal.', 'ACTIVO');

-- 3. MOTIVOS DE CONSULTA FRECUENTES POR ESPECIALIDAD
INSERT INTO motivo_cita (id_motivo, id_especialidad, descripcion, estado) VALUES
-- Cardiología (1)
(1, 1, 'Consulta cardiológica preventiva', 'ACTIVO'),
(2, 1, 'Control de hipertensión arterial', 'ACTIVO'),
(3, 1, 'Evaluación de riesgo prequirúrgico', 'ACTIVO'),
(4, 1, 'Evaluación de arritmias y palpitaciones', 'ACTIVO'),
-- Pediatría (2)
(5, 2, 'Control de crecimiento y desarrollo (CRED)', 'ACTIVO'),
(6, 2, 'Consulta pediátrica por fiebre o malestar', 'ACTIVO'),
(7, 2, 'Evaluación de vacunas y nutrición infantil', 'ACTIVO'),
(8, 2, 'Infección respiratoria pediátrica', 'ACTIVO'),
-- Dermatología (3)
(9, 3, 'Evaluación de lunares y manchas solares', 'ACTIVO'),
(10, 3, 'Tratamiento de acné y rosácea', 'ACTIVO'),
(11, 3, 'Dermatitis, alergias y eccemas', 'ACTIVO'),
(12, 3, 'Alopecia y caída del cabello', 'ACTIVO'),
-- Medicina Interna (4)
(13, 4, 'Chequeo médico integral de rutina', 'ACTIVO'),
(14, 4, 'Control de diabetes mellitus tipo 2', 'ACTIVO'),
(15, 4, 'Evaluación de síndrome metabólico', 'ACTIVO'),
(16, 4, 'Evaluación clínica preoperatoria', 'ACTIVO'),
-- Traumatología (5)
(17, 5, 'Dolor de espalda y lumbalgia', 'ACTIVO'),
(18, 5, 'Lesiones deportivas y esguinces', 'ACTIVO'),
(19, 5, 'Dolor articular de rodilla / Artrosis', 'ACTIVO'),
(20, 5, 'Evaluación post-traumatismo o caída', 'ACTIVO'),
-- Ginecología (6)
(21, 6, 'Chequeo ginecológico anual / Papanicoláu', 'ACTIVO'),
(22, 6, 'Control prenatal y ecografía obstétrica', 'ACTIVO'),
(23, 6, 'Orientación en planificación familiar', 'ACTIVO'),
(24, 6, 'Alteraciones menstruales o dolor pélvico', 'ACTIVO'),
-- Oftalmología (7)
(25, 7, 'Medida de la vista computarizada', 'ACTIVO'),
(26, 7, 'Despistaje de glaucoma y cataratas', 'ACTIVO'),
(27, 7, 'Síndrome de ojo seco e irritación', 'ACTIVO'),
(28, 7, 'Fondo de ojo diabético o hipertensivo', 'ACTIVO'),
-- Gastroenterología (8)
(29, 8, 'Reflujo gastroesofágico y acidez', 'ACTIVO'),
(30, 8, 'Gastritis y dolor estomacal', 'ACTIVO'),
(31, 8, 'Colon irritable y distensión abdominal', 'ACTIVO'),
(32, 8, 'Evaluación para video-endoscopía', 'ACTIVO'),
-- Neurología (9)
(33, 9, 'Cefalea tensional y migraña recurrente', 'ACTIVO'),
(34, 9, 'Trastornos del sueño e insomnio', 'ACTIVO'),
(35, 9, 'Evaluación de memoria y concentración', 'ACTIVO'),
(36, 9, 'Neuropatías y adormecimiento de extremidades', 'ACTIVO'),
-- Otorrinolaringología (10)
(37, 10, 'Rinitis alérgica y sinusitis crónica', 'ACTIVO'),
(38, 10, 'Dolor de oído y disminución auditiva', 'ACTIVO'),
(39, 10, 'Vértigo, mareos e inestabilidad', 'ACTIVO'),
(40, 10, 'Ronquido nocturno y apnea del sueño', 'ACTIVO'),
-- Endocrinología (11)
(41, 11, 'Control de tiroides / Hipotiroidismo', 'ACTIVO'),
(42, 11, 'Control integral de diabetes y glucosa', 'ACTIVO'),
(43, 11, 'Sobrepeso, obesidad y resistencia a insulina', 'ACTIVO'),
(44, 11, 'Evaluación y descarte de osteoporosis', 'ACTIVO'),
-- Urología (12)
(45, 12, 'Chequeo preventivo de próstata', 'ACTIVO'),
(46, 12, 'Infección urinaria recurrente', 'ACTIVO'),
(47, 12, 'Litiasis renal (cálculos en las vías)', 'ACTIVO'),
(48, 12, 'Consulta urológica general', 'ACTIVO');

-- 4. USUARIOS DEL SISTEMA (ADMIN, PERSONAL ADM, PACIENTES Y 48 MÉDICOS)
-- PasswordHash: $2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa (Password123!)
INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES
-- Roles Institucionales Solicitados:
(1, 'Eduardo', 'Ayala', 'eduardo@admin.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'ADMINISTRADOR', 1),
(2, 'Eduardo', 'Ayala', 'eduardo@padmin.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PERSONAL_ADMINISTRATIVO', 1),
(3, 'Eduardo', 'Ayala', 'eduardo@paciente.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),

-- 48 Médicos (4 por cada especialidad, todos en Sede San Borja):
-- Especialidad 1: Cardiología
(4, 'Eduardo', 'Ayala', 'eduardo@medico.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(5, 'Valeria', 'Rivas Montoya', 'vrivas@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(6, 'Carlos', 'Mendoza Valdivia', 'cmendoza@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(7, 'Roberto', 'Salas Gómez', 'rsalas@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 2: Pediatría
(8, 'Fernando', 'Solís Bravo', 'fsolis@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(9, 'Lucía', 'Benavides Carranza', 'lbenavides@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(10, 'Mariana', 'Castro Alva', 'mcastro@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(11, 'Gonzalo', 'Paredes Zegarra', 'gparedes@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 3: Dermatología
(12, 'Elena', 'Rostova Kovalska', 'erostova@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(13, 'Javier', 'Barrenechea Ugarte', 'jbarrenechea@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(14, 'Sofía', 'Málaga Cisneros', 'smalaga@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(15, 'Andrés', 'De la Puente Silva', 'adelapuente@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 4: Medicina Interna
(16, 'Alberto', 'Rivera González', 'arivera@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(17, 'Carmen Rosa', 'Palacios Bellido', 'cpalacios@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(18, 'Mauricio', 'Thorne Pardo', 'mthorne@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(19, 'Claudia', 'Forsyth Romero', 'cforsyth@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 5: Traumatología y Ortopedia
(20, 'Ricardo', 'Miró Quesada', 'rmiroquesada@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(21, 'César', 'Hildebrandt Luna', 'childebrandt@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(22, 'Patricia', 'Del Río Vega', 'pdelrio@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(23, 'Diego', 'Bustamante Romero', 'dbustamante@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 6: Ginecología y Obstetricia
(24, 'Beatriz', 'Sandoval Ramos', 'bsandoval@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(25, 'Cecilia', 'Valenzuela Ponce', 'cvalenzuela@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(26, 'Martín', 'Belaúnde Terry', 'mbelaunde@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(27, 'Gisela', 'Ortiz Morán', 'gortiz@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 7: Oftalmología
(28, 'Juan Carlos', 'Tafur Roca', 'jctafur@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(29, 'Rosa María', 'Palacios McBride', 'rmpalacios@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(30, 'Aldo', 'Mariátegui Flores', 'amariategui@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(31, 'Roxana', 'Cueva Mejía', 'rcueva@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 8: Gastroenterología
(32, 'Renato', 'Cisneros Campodónico', 'rcisneros@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(33, 'Milagros', 'Leiva Gálvez', 'mleiva@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(34, 'Jaime', 'Bayly Letts', 'jbayly@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(35, 'Beto', 'Ortiz Pajuelo', 'bortiz@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 9: Neurología
(36, 'Daniel', 'Alarcón Pease', 'dalarcon@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(37, 'Gabriela', 'Wiener Ramos', 'gwiener@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(38, 'Santiago', 'Roncagliolo Lohmann', 'sroncagliolo@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(39, 'Karina', 'Pacheco Medrano', 'kpacheco@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 10: Otorrinolaringología
(40, 'Alonso', 'Cueto Fernandini', 'acueto@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(41, 'Claudia', 'Llosa Bueno', 'cllosa@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(42, 'Francisco', 'Lombardi García', 'flombardi@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(43, 'Josué', 'Méndez Barrenechea', 'jmendez@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 11: Endocrinología
(44, 'Salvador', 'Del Solar Labarthe', 'sdelsolar@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(45, 'Mónica', 'Sánchez Cuadros', 'msanchez@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(46, 'Christian', 'Meier Zender', 'cmeier@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(47, 'Stephanie', 'Cayo Sanguinetti', 'scayo@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Especialidad 12: Urología
(48, 'Marco', 'Zunino Costa', 'mzunino@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(49, 'Bruno', 'Ascenzo Bravo', 'bascenzo@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(50, 'Carlos', 'Carlín González', 'ccarlin@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
(51, 'Giovanni', 'Ciccia Ridoutt', 'gciccia@clinica.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),

-- Pacientes adicionales:
(52, 'Jorge', 'Valdivia Núñez', 'jorge.valdivia@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(53, 'Juan Alberto', 'Pérez Gómez', 'juan.perez@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(54, 'Elena', 'Marroquín Silva', 'elena.m@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(55, 'Rosa María', 'Chávez Quispe', 'rosa.chavez@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(56, 'Miguel Ángel', 'Huamán Flores', 'miguel.huaman@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(57, 'Carla Jimena', 'Ríos Barreto', 'carla.rios@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(58, 'Luis Fernando', 'Castillo Benítez', 'luis.castillo@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(59, 'Diana Patricia', 'Vargas Morales', 'diana.vargas@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(60, 'Víctor Hugo', 'Navarro Paredes', 'victor.navarro@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(61, 'Luciana', 'García Miró', 'luciana.gm@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(62, 'Raúl Ernesto', 'Tello Sotomayor', 'raul.tello@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(63, 'Andrea', 'Salazar Corbacho', 'andrea.salazar@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(64, 'Héctor', 'Villanueva Prado', 'hector.v@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
(65, 'Camila', 'Montesinos León', 'camila.m@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1);

-- 5. ADMINISTRADOR
INSERT INTO administrador (id_administrador, id_usuario, estado) VALUES
(1, 1, 'ACTIVO');

-- 6. PERSONAL ADMINISTRATIVO (SEDE SAN BORJA)
INSERT INTO personal_administrativo (id_personal, id_usuario, id_sede, codigo_empleado, cargo) VALUES
(1, 2, 1, 'ADM-001', 'Coordinador de Admisión y Operaciones');

-- 7. MÉDICOS (48 MÉDICOS ASIGNADOS 100% A SEDE SAN BORJA)
INSERT INTO medico (id_medico, id_usuario, cmp, id_especialidad, id_sede, estado) VALUES
-- Cardiología (id_especialidad = 1, id_sede = 1)
(1, 4, 'CMP-10001', 1, 1, 'ACTIVO'),
(2, 5, 'CMP-10002', 1, 1, 'ACTIVO'),
(3, 6, 'CMP-10003', 1, 1, 'ACTIVO'),
(4, 7, 'CMP-10004', 1, 1, 'ACTIVO'),
-- Pediatría (id_especialidad = 2, id_sede = 1)
(5, 8, 'CMP-10005', 2, 1, 'ACTIVO'),
(6, 9, 'CMP-10006', 2, 1, 'ACTIVO'),
(7, 10, 'CMP-10007', 2, 1, 'ACTIVO'),
(8, 11, 'CMP-10008', 2, 1, 'ACTIVO'),
-- Dermatología (id_especialidad = 3, id_sede = 1)
(9, 12, 'CMP-10009', 3, 1, 'ACTIVO'),
(10, 13, 'CMP-10010', 3, 1, 'ACTIVO'),
(11, 14, 'CMP-10011', 3, 1, 'ACTIVO'),
(12, 15, 'CMP-10012', 3, 1, 'ACTIVO'),
-- Medicina Interna (id_especialidad = 4, id_sede = 1)
(13, 16, 'CMP-10013', 4, 1, 'ACTIVO'),
(14, 17, 'CMP-10014', 4, 1, 'ACTIVO'),
(15, 18, 'CMP-10015', 4, 1, 'ACTIVO'),
(16, 19, 'CMP-10016', 4, 1, 'ACTIVO'),
-- Traumatología y Ortopedia (id_especialidad = 5, id_sede = 1)
(17, 20, 'CMP-10017', 5, 1, 'ACTIVO'),
(18, 21, 'CMP-10018', 5, 1, 'ACTIVO'),
(19, 22, 'CMP-10019', 5, 1, 'ACTIVO'),
(20, 23, 'CMP-10020', 5, 1, 'ACTIVO'),
-- Ginecología y Obstetricia (id_especialidad = 6, id_sede = 1)
(21, 24, 'CMP-10021', 6, 1, 'ACTIVO'),
(22, 25, 'CMP-10022', 6, 1, 'ACTIVO'),
(23, 26, 'CMP-10023', 6, 1, 'ACTIVO'),
(24, 27, 'CMP-10024', 6, 1, 'ACTIVO'),
-- Oftalmología (id_especialidad = 7, id_sede = 1)
(25, 28, 'CMP-10025', 7, 1, 'ACTIVO'),
(26, 29, 'CMP-10026', 7, 1, 'ACTIVO'),
(27, 30, 'CMP-10027', 7, 1, 'ACTIVO'),
(28, 31, 'CMP-10028', 7, 1, 'ACTIVO'),
-- Gastroenterología (id_especialidad = 8, id_sede = 1)
(29, 32, 'CMP-10029', 8, 1, 'ACTIVO'),
(30, 33, 'CMP-10030', 8, 1, 'ACTIVO'),
(31, 34, 'CMP-10031', 8, 1, 'ACTIVO'),
(32, 35, 'CMP-10032', 8, 1, 'ACTIVO'),
-- Neurología (id_especialidad = 9, id_sede = 1)
(33, 36, 'CMP-10033', 9, 1, 'ACTIVO'),
(34, 37, 'CMP-10034', 9, 1, 'ACTIVO'),
(35, 38, 'CMP-10035', 9, 1, 'ACTIVO'),
(36, 39, 'CMP-10036', 9, 1, 'ACTIVO'),
-- Otorrinolaringología (id_especialidad = 10, id_sede = 1)
(37, 40, 'CMP-10037', 10, 1, 'ACTIVO'),
(38, 41, 'CMP-10038', 10, 1, 'ACTIVO'),
(39, 42, 'CMP-10039', 10, 1, 'ACTIVO'),
(40, 43, 'CMP-10040', 10, 1, 'ACTIVO'),
-- Endocrinología (id_especialidad = 11, id_sede = 1)
(41, 44, 'CMP-10041', 11, 1, 'ACTIVO'),
(42, 45, 'CMP-10042', 11, 1, 'ACTIVO'),
(43, 46, 'CMP-10043', 11, 1, 'ACTIVO'),
(44, 47, 'CMP-10044', 11, 1, 'ACTIVO'),
-- Urología (id_especialidad = 12, id_sede = 1)
(45, 48, 'CMP-10045', 12, 1, 'ACTIVO'),
(46, 49, 'CMP-10046', 12, 1, 'ACTIVO'),
(47, 50, 'CMP-10047', 12, 1, 'ACTIVO'),
(48, 51, 'CMP-10048', 12, 1, 'ACTIVO');

-- 8. PACIENTES (SEDE PREFERIDA SAN BORJA)
INSERT INTO paciente (id_paciente, id_usuario, dni, fecha_nacimiento, telefono, direccion, canal_aviso, sede_preferida, alergias, tipo_cobertura) VALUES
(1, 3, '72345678', '1994-06-15', '+51 987 112 334', 'Av. San Borja Sur 420, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(2, 52, '10492837', '1976-05-14', '+51 987 654 321', 'Av. Aviación 2850, San Borja', 'WhatsApp', 'Sede San Borja', 'Penicilina', 'EPS'),
(3, 53, '44091234', '1982-08-20', '+51 944 112 098', 'Jr. Monterrico 120, Surco', 'WhatsApp', 'Sede San Borja', 'AINEs', 'PARTICULAR'),
(4, 54, '29841029', '1969-01-22', '+51 977 432 110', 'Av. Primavera 650, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(5, 55, '41238910', '1985-03-12', '+51 961 223 445', 'Calle Las Artes 140, San Borja', 'WhatsApp', 'Sede San Borja', 'Sulfas', 'EPS'),
(6, 56, '47891230', '1991-11-05', '+51 954 887 112', 'Av. Javier Prado Este 2450, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(7, 57, '70981245', '1998-04-18', '+51 932 445 667', 'Jr. Rousseau 320, San Borja', 'WhatsApp', 'Sede San Borja', 'Polvo/Acaros', 'PARTICULAR'),
(8, 58, '09823411', '1965-09-30', '+51 981 776 554', 'Av. San Luis 1890, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(9, 59, '72109843', '1995-12-08', '+51 923 118 990', 'Calle Carpaccio 210, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(10, 60, '18293041', '1973-07-25', '+51 945 667 889', 'Av. Guardia Civil 680, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'PARTICULAR'),
(11, 61, '74892019', '2000-02-14', '+51 998 776 112', 'Calle Morelli 180, San Borja', 'WhatsApp', 'Sede San Borja', 'Lactosa', 'EPS'),
(12, 62, '43120984', '1983-10-10', '+51 966 332 119', 'Jr. Bellini 450, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(13, 63, '76128938', '1997-06-28', '+51 955 889 001', 'Av. Gálvez Barrenechea 890, San Borja', 'WhatsApp', 'Sede San Borja', 'Mariscos', 'PARTICULAR'),
(14, 64, '08192847', '1960-01-15', '+51 912 334 556', 'Calle Pietro Marchand 110, San Borja', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS'),
(15, 65, '73819204', '1996-08-04', '+51 978 990 123', 'Av. Angamos Este 1800, Surquillo', 'WhatsApp', 'Sede San Borja', 'Ninguna', 'EPS');


-- =============================================================================
-- FIN DEL SEED INICIAL (TABLAS TRANSACCIONALES LIMPIAS PARA PRUEBAS DEL USUARIO)
-- =============================================================================




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
