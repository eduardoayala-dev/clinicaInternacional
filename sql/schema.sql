-- =============================================================================
-- ESQUEMA DE BASE DE DATOS - CLÍNICA INTERNACIONAL (MySQL 8.0)
-- Coincidencia 100% con el Diagrama de Clases (17 Tablas / Entidades)
-- Requisitos: HU003, RNF-01, RNF-02, RNF-03, RNF-04, RNF-05
-- =============================================================================

CREATE DATABASE IF NOT EXISTS clinica_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinica_db;

-- Desactivar temporalmente llaves foráneas para recreación limpia
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reporte;
DROP TABLE IF EXISTS indicador;
DROP TABLE IF EXISTS reasignacion_inteligente;
DROP TABLE IF EXISTS notificacion;
DROP TABLE IF EXISTS historial_asistencia;
DROP TABLE IF EXISTS lista_espera;
DROP TABLE IF EXISTS cita;
DROP TABLE IF EXISTS disponibilidad;
DROP TABLE IF EXISTS motivo_cita;
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
CREATE TABLE disponibilidad (
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
    estado ENUM('SOLICITADA', 'CONFIRMADA', 'EN_SALA', 'ATENDIDA', 'NO_ASISTIO', 'REPROGRAMADA', 'CANCELADA') DEFAULT 'SOLICITADA',
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
    id_sede INT NOT NULL,
    id_motivo INT DEFAULT NULL,
    nivel_urgencia ENUM('ALTA', 'MEDIA', 'BAJA') DEFAULT 'BAJA',
    rango_horario ENUM('MANANA', 'TARDE', 'NOCHE', 'INDIFERENTE') DEFAULT 'INDIFERENTE',
    canal_notificacion ENUM('CORREO', 'SMS', 'WHATSAPP') DEFAULT 'CORREO',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('ACTIVA', 'OFERTADA', 'ATENDIDA', 'CANCELADA') DEFAULT 'ACTIVA',
    posicion INT DEFAULT 0,
    CONSTRAINT fk_lista_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT fk_lista_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad),
    CONSTRAINT fk_lista_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
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
-- DATOS SINTÉTICOS DE PRUEBA INITIAL SEED (CLÍNICA INTERNACIONAL)
-- =============================================================================

INSERT INTO sede (codigo, nombre, direccion, telefono, estado) VALUES
('SED-NORTE', 'Sede Lima Norte', 'Av. Alfredo Mendiola 1400, Los Olivos', '01-610-3333', 'ACTIVO'),
('SED-CENTRO', 'Sede Central Garcilaso', 'Av. Garcilaso de la Vega 1420, Lima', '01-610-1111', 'ACTIVO'),
('SED-SUR', 'Sede San Borja', 'Av. Guardia Civil 385, San Borja', '01-610-2222', 'ACTIVO'),
('SED-LIMA', 'Sede Lima Principal', 'Av. Inca Garcilaso de la Vega 1420, Cercado de Lima', '01-610-4444', 'ACTIVO');

INSERT INTO especialidad (nombre, descripcion, estado) VALUES
('Cardiología', 'Atención y diagnóstico especializado en salud cardiovascular', 'ACTIVO'),
('Dermatología', 'Cuidado de la piel, cabello y uñas', 'ACTIVO'),
('Pediatría', 'Atención médica integral infantil', 'ACTIVO'),
('Medicina Interna', 'Atención del adulto y enfermedades complejas', 'ACTIVO'),
('Oftalmología', 'Salud visual y cirugías oculares', 'ACTIVO');

INSERT INTO usuario (nombres, apellidos, email, password_hash, rol, activo) VALUES
('Roberto', 'Vargas', 'admin@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'ADMINISTRADOR', 1),
('Marina', 'Salvatierra', 'personal@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PERSONAL_ADMINISTRATIVO', 1),
('Carlos', 'Mendoza Valdivia', 'cmendoza@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
('Elena', 'Rostova K.', 'erostova@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
('Fernando', 'Solís Bravo', 'fsolis@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
('Valeria', 'Rivas M.', 'vrivas@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
('Roberto', 'Salas Gomez', 'rsalas@clinicainternacional.pe', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'MEDICO', 1),
('Jorge', 'Valdivia Núñez', 'jorge.valdivia@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
('Juan Alberto', 'Pérez Gómez', 'juan.perez@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
('Alberto', 'Rivera González', 'a.rivera@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
('Elena', 'Marroquín Silva', 'elena.m@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1),
('Beatriz', 'Sandoval Ramos', 'b.sandoval@correo.com', '$2a$10$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa', 'PACIENTE', 1);

INSERT INTO administrador (id_usuario, estado) VALUES (1, 'ACTIVO');
INSERT INTO personal_administrativo (id_usuario, id_sede, codigo_empleado, cargo) VALUES (2, 1, 'ADM-001', 'Coordinador de Admisión');

INSERT INTO medico (id_usuario, cmp, id_especialidad, id_sede, estado) VALUES 
(3, 'CMP-45211', 1, 4, 'ACTIVO'),
(4, 'CMP-45892', 4, 2, 'ACTIVO'),
(5, 'CMP-38982', 1, 1, 'ACTIVO'),
(6, 'CMP-51284', 1, 1, 'ACTIVO'),
(7, 'CMP-39871', 2, 3, 'ACTIVO');

INSERT INTO paciente (id_usuario, dni, fecha_nacimiento, telefono, direccion) VALUES 
(8, '10492837', '1976-05-14', '+51 987 654 321', 'Av. Las Palmeras 450, Distrito Norte'),
(9, '72345678', '1982-05-14', '+51 987 654 321', 'Av. Universitaria 1200, Distrito Norte'),
(10, '44091234', '1984-11-03', '+51 944 112 098', 'Jr. Los Sauces 210, Distrito Central'),
(11, '29841029', '1969-01-22', '+51 977 432 110', 'Av. El Sol 890, Distrito Sur'),
(12, '76128938', '1994-12-11', '+51 955 889 001', 'Av. Primavera 1540, Distrito Este');

INSERT INTO disponibilidad (id_medico, id_sede, fecha, hora_inicio, hora_fin, duracion_minutos, estado) VALUES
(1, 4, '2026-10-07', '08:00:00', '14:00:00', 30, 'PUBLICADO'),
(2, 2, '2026-10-07', '08:30:00', '13:00:00', 30, 'PUBLICADO'),
(3, 1, '2026-10-07', '09:00:00', '15:00:00', 30, 'PUBLICADO'),
(4, 1, '2026-10-07', '10:00:00', '16:00:00', 30, 'PUBLICADO'),
(5, 3, '2026-10-07', '11:00:00', '17:00:00', 30, 'PUBLICADO');

INSERT INTO cita (id_paciente, id_medico, id_disponibilidad, fecha_hora, estado, nivel_urgencia, riesgo_cancelacion) VALUES
(1, 1, 1, '2026-10-07 08:30:00', 'CONFIRMADA', 'BAJA', 'BAJO'),
(2, 2, 2, '2026-10-07 08:45:00', 'SOLICITADA', 'MEDIA', 'BAJO'),
(3, 3, 3, '2026-10-07 09:00:00', 'CONFIRMADA', 'BAJA', 'MEDIO'),
(4, 5, 5, '2026-10-07 09:15:00', 'CONFIRMADA', 'ALTA', 'ALTO'),
(5, 1, 1, '2026-10-07 11:30:00', 'SOLICITADA', 'ALTA', 'ALTO');

INSERT INTO lista_espera (id_paciente, id_especialidad, id_sede, nivel_urgencia, rango_horario, estado, posicion) VALUES
(2, 1, 1, 'ALTA', 'MANANA', 'ACTIVA', 1),
(3, 2, 2, 'MEDIA', 'TARDE', 'ACTIVA', 2);

