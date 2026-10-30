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
