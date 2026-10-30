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
