CREATE TABLE IF NOT EXISTS correo_oferta (
    id_oferta INT PRIMARY KEY,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos SMALLINT NOT NULL DEFAULT 0,
    disponible_en DATETIME(6) NOT NULL,
    iniciada_en DATETIME(6) NULL,
    finalizada_en DATETIME(6) NULL,
    CONSTRAINT fk_correo_oferta FOREIGN KEY (id_oferta) REFERENCES reasignacion_inteligente(id_reasignacion),
    CONSTRAINT chk_correo_oferta_estado CHECK (estado IN ('PENDIENTE','ENVIANDO','ENVIADO','NO_CONFIRMADO','OMITIDO')),
    CONSTRAINT chk_correo_oferta_intentos CHECK (intentos BETWEEN 0 AND 3),
    INDEX idx_correo_oferta_pendiente (estado,disponible_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
