-- =============================================================================
-- ESQUEMA DE BASE DE DATOS - CLÍNICA INTERNACIONAL (MySQL 8.0)
-- Requisitos: HU003, RNF-01, RNF-02, RNF-03, RNF-04, RNF-05
-- =============================================================================

CREATE DATABASE IF NOT EXISTS clinica_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinica_db;

-- Desactivar temporalmente llaves foráneas para recreación limpia
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reasignacion_inteligente;
DROP TABLE IF EXISTS notificacion;
DROP TABLE IF EXISTS historial_asistencia;
DROP TABLE IF EXISTS lista_espera;
DROP TABLE IF EXISTS cita;
DROP TABLE IF EXISTS disponibilidad;
DROP TABLE IF EXISTS motivo_consulta;
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
-- 1. TABLA: usuario (Autenticación y Perfil Base)
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
-- 2. TABLA: paciente
-- -----------------------------------------------------------------------------
CREATE TABLE paciente (
    id_paciente INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    dni VARCHAR(15) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    CONSTRAINT fk_paciente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    INDEX idx_paciente_dni (dni)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 3. TABLA: sede
-- -----------------------------------------------------------------------------
CREATE TABLE sede (
    id_sede INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    imagen_url VARCHAR(255) DEFAULT NULL,
    estado ENUM('ACTIVO', 'MANTENIMIENTO', 'INACTIVO') DEFAULT 'ACTIVO',
    INDEX idx_sede_codigo (codigo),
    INDEX idx_sede_estado (estado)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 4. TABLA: especialidad
-- -----------------------------------------------------------------------------
CREATE TABLE especialidad (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT DEFAULT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO'
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 5. TABLA: medico
-- -----------------------------------------------------------------------------
CREATE TABLE medico (
    id_medico INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    cmp VARCHAR(20) NOT NULL UNIQUE,
    id_especialidad INT NOT NULL,
    id_sede INT NOT NULL,
    estado ENUM('ACTIVO', 'LICENCIA', 'INACTIVO') DEFAULT 'ACTIVO',
    CONSTRAINT fk_medico_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_medico_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad),
    CONSTRAINT fk_medico_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede),
    INDEX idx_medico_cmp (cmp)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 6. TABLA: personal_administrativo
-- -----------------------------------------------------------------------------
CREATE TABLE personal_administrativo (
    id_personal INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    id_sede INT NOT NULL,
    codigo_empleado VARCHAR(20) NOT NULL UNIQUE,
    cargo VARCHAR(100) NOT NULL,
    CONSTRAINT fk_personal_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_personal_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 7. TABLA: administrador
-- -----------------------------------------------------------------------------
CREATE TABLE administrador (
    id_administrador INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    CONSTRAINT fk_admin_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 8. TABLA: motivo_consulta
-- -----------------------------------------------------------------------------
CREATE TABLE motivo_consulta (
    id_motivo INT AUTO_INCREMENT PRIMARY KEY,
    id_especialidad INT NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    estado ENUM('ACTIVO', 'INACTIVO') DEFAULT 'ACTIVO',
    CONSTRAINT fk_motivo_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 9. TABLA: disponibilidad (Horarios publicables)
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
    CONSTRAINT fk_disp_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede),
    INDEX idx_disp_busqueda (id_sede, fecha, estado)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 10. TABLA: cita
-- -----------------------------------------------------------------------------
CREATE TABLE cita (
    id_cita INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_medico INT NOT NULL,
    id_disponibilidad INT NOT NULL,
    id_motivo INT DEFAULT NULL,
    fecha_hora DATETIME NOT NULL,
    estado ENUM('SOLICITADA', 'CONFIRMADA', 'ATENDIDA', 'NO_ASISTIO', 'REPROGRAMADA', 'CANCELADA') DEFAULT 'SOLICITADA',
    nivel_urgencia ENUM('ALTA', 'MEDIA', 'BAJA') DEFAULT 'BAJA',
    fecha_recomendada_proxima DATE DEFAULT NULL,
    riesgo_cancelacion ENUM('BAJO', 'MEDIO', 'ALTO', 'SIN_HISTORIAL') DEFAULT 'SIN_HISTORIAL',
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT fk_cita_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
    CONSTRAINT fk_cita_disponibilidad FOREIGN KEY (id_disponibilidad) REFERENCES disponibilidad(id_disponibilidad),
    CONSTRAINT fk_cita_motivo FOREIGN KEY (id_motivo) REFERENCES motivo_consulta(id_motivo),
    INDEX idx_cita_paciente (id_paciente),
    INDEX idx_cita_medico_fecha (id_medico, fecha_hora),
    INDEX idx_cita_estado (estado)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 11. TABLA: lista_espera (Módulo de Reasignación Inteligente)
-- -----------------------------------------------------------------------------
CREATE TABLE lista_espera (
    id_lista_espera INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_especialidad INT NOT NULL,
    id_sede INT NOT NULL,
    nivel_urgencia ENUM('ALTA', 'MEDIA', 'BAJA') DEFAULT 'BAJA',
    rango_horario ENUM('MANANA', 'TARDE', 'NOCHE', 'INDIFERENTE') DEFAULT 'INDIFERENTE',
    canal_notificacion ENUM('CORREO', 'SMS', 'WHATSAPP') DEFAULT 'CORREO',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('ACTIVA', 'OFERTADA', 'ATENDIDA', 'CANCELADA') DEFAULT 'ACTIVA',
    posicion INT DEFAULT 0,
    CONSTRAINT fk_lista_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
    CONSTRAINT fk_lista_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad),
    CONSTRAINT fk_lista_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede),
    INDEX idx_lista_espera_prioridad (id_especialidad, id_sede, nivel_urgencia, fecha_registro)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 12. TABLA: reasignacion_inteligente (Ofertas de Cupos Liberados)
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
-- 13. TABLA: notificacion
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
-- 14. TABLA: historial_asistencia
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
-- 15. TABLA: priorizacion_config (Parámetros Operativos del Algoritmo)
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
-- 16. TABLA: auditoria_seguridad (Audit Logs para RNF-05)
-- -----------------------------------------------------------------------------
CREATE TABLE auditoria_seguridad (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT DEFAULT NULL,
    ip_origen VARCHAR(45) NOT NULL,
    evento VARCHAR(100) NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_evento DATETIME DEFAULT CURRENT_TIMESTAMP,
    sospechoso TINYINT(1) DEFAULT 0
) ENGINE=InnoDB;

-- =============================================================================
-- DATOS SINTÉTICOS DE PRUEBA (HU003 - Criterio CA-3 y CA-5)
-- Contraseñas hasheadas con BCrypt (Valor plano: Admin123! / Paciente123! / Medico123!)
-- Hash BCrypt estándar: $2a$10$7R9oD7L/.wYtLd3y/J50g.pUe04l/0.60Z0MhS7C4a2K2e9oJzXre
-- =============================================================================

-- 1. Insertar Sedes
INSERT INTO sede (codigo, nombre, direccion, telefono, estado) VALUES
('SED-NORTE', 'Sede Lima Norte', 'Av. Alfredo Mendiola 1400, Los Olivos', '01-610-3333', 'ACTIVO'),
('SED-CENTRO', 'Sede Central Garcilaso', 'Av. Garcilaso de la Vega 1420, Lima', '01-610-1111', 'ACTIVO'),
('SED-SUR', 'Sede San Borja', 'Av. Guardia Civil 385, San Borja', '01-610-2222', 'ACTIVO');

-- 2. Insertar Especialidades
INSERT INTO especialidad (nombre, descripcion, estado) VALUES
('Cardiología', 'Atención y diagnóstico especializado en salud cardiovascular', 'ACTIVO'),
('Dermatología', 'Cuidado de la piel, cabello y uñas', 'ACTIVO'),
('Pediatría', 'Atención médica integral infantil', 'ACTIVO'),
('Medicina Interna', 'Atención del adulto y enfermedades complejas', 'ACTIVO');

-- 3. Insertar Usuarios Sintéticos
-- Passwords: Admin123! (hash ficticio sintético para desarrollo)
INSERT INTO usuario (nombres, apellidos, email, password_hash, rol, activo) VALUES
('Roberto', 'Vargas', 'admin@clinicainternacional.pe', '$2a$10$e80x6uJt7p41Z1c7xL2k8.5jW9hQ1wY0xZ2aB3c4d5e6f7g8h9i0j', 'ADMINISTRADOR', 1),
('Marina', 'Salvatierra', 'personal@clinicainternacional.pe', '$2a$10$e80x6uJt7p41Z1c7xL2k8.5jW9hQ1wY0xZ2aB3c4d5e6f7g8h9i0j', 'PERSONAL_ADMINISTRATIVO', 1),
('Carlos', 'Mendoza', 'cmendoza@clinicainternacional.pe', '$2a$10$e80x6uJt7p41Z1c7xL2k8.5jW9hQ1wY0xZ2aB3c4d5e6f7g8h9i0j', 'MEDICO', 1),
('Elena', 'Rostova', 'erostova@clinicainternacional.pe', '$2a$10$e80x6uJt7p41Z1c7xL2k8.5jW9hQ1wY0xZ2aB3c4d5e6f7g8h9i0j', 'MEDICO', 1),
('Juan Alberto', 'Pérez Gómez', 'juan.perez@correo.com', '$2a$10$e80x6uJt7p41Z1c7xL2k8.5jW9hQ1wY0xZ2aB3c4d5e6f7g8h9i0j', 'PACIENTE', 1),
('Jhenifer', 'Monterroso', 'jmonterroso@gmail.com', '$2a$10$e80x6uJt7p41Z1c7xL2k8.5jW9hQ1wY0xZ2aB3c4d5e6f7g8h9i0j', 'PACIENTE', 1);

-- 4. Insertar Perfiles
INSERT INTO administrador (id_usuario, estado) VALUES (1, 'ACTIVO');

INSERT INTO personal_administrativo (id_usuario, id_sede, codigo_empleado, cargo) VALUES 
(2, 1, 'ADM-001', 'Coordinador de Admisión');

INSERT INTO medico (id_usuario, cmp, id_especialidad, id_sede, estado) VALUES 
(3, 'CMP-45211', 1, 1, 'ACTIVO'), -- Dr. Carlos Mendoza - Cardiología Norte
(4, 'CMP-45892', 2, 2, 'ACTIVO'); -- Dra. Elena Rostova - Dermatología Centro

INSERT INTO paciente (id_usuario, dni, fecha_nacimiento, telefono, direccion) VALUES 
(5, '72345678', '1988-05-14', '987654321', 'Av. Las Flores 123, Los Olivos'),
(6, '74125896', '1995-09-20', '955889901', 'Av. Javier Prado 450, San Borja');

-- 5. Configuración por defecto de priorización
INSERT INTO priorizacion_config (nombre, criterio, peso, nivel, prioridad_activa, tiempo_limite_oferta_minutos, candidatos_simultaneos) VALUES
('Especialidad Requerida', 'ESPECIALIDAD', 1.0, 'ALTA', 1, 15, 1),
('Sede Requerida', 'SEDE', 1.0, 'ALTA', 1, 15, 1),
('Prioridad Clínica', 'PRIORIDAD_CLINICA', 1.0, 'ALTA', 1, 15, 1),
('Tiempo en Lista de Espera', 'FIFO', 1.0, 'MEDIA', 1, 15, 1);

