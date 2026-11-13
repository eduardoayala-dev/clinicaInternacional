CREATE TABLE IF NOT EXISTS confirmacion_asistencia (
    id_cita INT PRIMARY KEY,
    creada_en DATETIME(6) NOT NULL,
    vence_en DATETIME(6) NOT NULL,
    confirmada_en DATETIME(6) NULL,
    canal VARCHAR(16) NULL,
    id_usuario_confirmo INT NULL,
    CONSTRAINT fk_confirmacion_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_confirmacion_usuario FOREIGN KEY (id_usuario_confirmo) REFERENCES usuario(id_usuario),
    CONSTRAINT ck_confirmacion_canal CHECK (canal IS NULL OR canal IN ('WEB','ADMISION')),
    INDEX idx_confirmacion_pendiente (confirmada_en, vence_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contacto_admision_cita (
    id_contacto BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_cita INT NOT NULL,
    id_usuario_personal INT NOT NULL,
    fecha_contacto DATETIME(6) NOT NULL,
    canal VARCHAR(16) NOT NULL,
    resultado VARCHAR(24) NOT NULL,
    CONSTRAINT fk_contacto_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_contacto_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario),
    CONSTRAINT ck_contacto_canal CHECK (canal IN ('LLAMADA','CORREO')),
    CONSTRAINT ck_contacto_resultado CHECK (resultado IN ('SIN_RESPUESTA','CONFIRMA','NO_CONFIRMA')),
    INDEX idx_contacto_cita_fecha (id_cita, fecha_contacto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO confirmacion_asistencia(id_cita, creada_en, vence_en)
SELECT c.id_cita, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 HOUR),
       CASE WHEN DATE_SUB(c.fecha_hora, INTERVAL 24 HOUR) > DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 HOUR)
            THEN DATE_SUB(c.fecha_hora, INTERVAL 24 HOUR)
            ELSE c.fecha_hora END
FROM cita c
WHERE c.estado IN ('SOLICITADA','CONFIRMADA')
  AND c.fecha_hora > DATE_SUB(UTC_TIMESTAMP(), INTERVAL 5 HOUR)
  AND NOT EXISTS (SELECT 1 FROM confirmacion_asistencia ca WHERE ca.id_cita=c.id_cita);
