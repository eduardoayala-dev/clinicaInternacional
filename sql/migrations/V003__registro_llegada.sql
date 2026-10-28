SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS admision_cita (
    id_cita INT PRIMARY KEY,
    id_usuario_personal INT NOT NULL,
    fecha_llegada DATETIME NOT NULL,
    tolerancia_minutos SMALLINT NOT NULL,
    llego_tarde BOOLEAN NOT NULL,
    CONSTRAINT fk_admision_cita FOREIGN KEY (id_cita) REFERENCES cita(id_cita),
    CONSTRAINT fk_admision_usuario FOREIGN KEY (id_usuario_personal) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;
