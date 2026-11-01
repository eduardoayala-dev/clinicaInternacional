CREATE TABLE IF NOT EXISTS configuracion_ofertas (
    id_configuracion INT PRIMARY KEY,
    minutos_respuesta INT NOT NULL DEFAULT 15,
    anticipacion_minima INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    actualizado_por INT NULL,
    actualizado_en DATETIME(6) NULL,
    CONSTRAINT fk_configuracion_ofertas_actor FOREIGN KEY (actualizado_por) REFERENCES usuario(id_usuario),
    CONSTRAINT chk_configuracion_unica CHECK (id_configuracion=1),
    CONSTRAINT chk_configuracion_respuesta CHECK (minutos_respuesta BETWEEN 5 AND 60),
    CONSTRAINT chk_configuracion_anticipacion CHECK (anticipacion_minima BETWEEN 0 AND 10080),
    CONSTRAINT chk_configuracion_version CHECK (version>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO configuracion_ofertas(id_configuracion,minutos_respuesta,anticipacion_minima,version)
SELECT 1,15,0,0 WHERE NOT EXISTS(SELECT 1 FROM configuracion_ofertas WHERE id_configuracion=1);
