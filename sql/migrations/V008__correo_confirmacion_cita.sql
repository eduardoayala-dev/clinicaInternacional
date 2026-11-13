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
