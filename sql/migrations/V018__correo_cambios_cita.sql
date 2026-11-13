CREATE TABLE IF NOT EXISTS correo_cambio_cita (
    id_aviso INT AUTO_INCREMENT PRIMARY KEY,
    id_cita INT NOT NULL,
    id_cita_anterior INT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos SMALLINT NOT NULL DEFAULT 0,
    disponible_en DATETIME(6) NOT NULL,
    iniciada_en DATETIME(6) NULL,
    finalizada_en DATETIME(6) NULL,
    CONSTRAINT fk_aviso_cambio_cita FOREIGN KEY(id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_aviso_cambio_anterior FOREIGN KEY(id_cita_anterior) REFERENCES cita(id_cita),
    CONSTRAINT uq_aviso_cambio UNIQUE(id_cita,tipo),
    CONSTRAINT ck_aviso_cambio_tipo CHECK(tipo IN ('CANCELADA','REPROGRAMADA')),
    CONSTRAINT ck_aviso_cambio_estado CHECK(estado IN ('PENDIENTE','ENVIANDO','ENVIADO','NO_CONFIRMADO','OMITIDO')),
    CONSTRAINT ck_aviso_cambio_intentos CHECK(intentos BETWEEN 0 AND 3),
    INDEX idx_aviso_cambio_pendiente(estado,disponible_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
