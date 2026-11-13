CREATE TABLE IF NOT EXISTS invitacion_personal (
    id_usuario INT PRIMARY KEY,
    token_hash BINARY(32) NOT NULL UNIQUE,
    creada_por INT NOT NULL,
    creada_en DATETIME(6) NOT NULL,
    vence_en DATETIME(6) NOT NULL,
    utilizada_en DATETIME(6) NULL,
    revocada_en DATETIME(6) NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT fk_invitacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    CONSTRAINT fk_invitacion_actor FOREIGN KEY (creada_por) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_invitacion_envio CHECK (estado_envio IN ('PENDIENTE','ENVIADO','NO_CONFIRMADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
