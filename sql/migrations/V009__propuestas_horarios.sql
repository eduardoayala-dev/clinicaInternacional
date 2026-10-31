CREATE TABLE IF NOT EXISTS propuesta_horario (
    id_propuesta INT AUTO_INCREMENT PRIMARY KEY,
    id_medico INT NOT NULL,
    id_sede INT NOT NULL,
    dias_semana INT NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    duracion_minutos INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creada_en DATETIME(6) NOT NULL,
    revisada_en DATETIME(6) NULL,
    id_revisor INT NULL,
    motivo_revision VARCHAR(500) NULL,
    CONSTRAINT fk_propuesta_medico FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
    CONSTRAINT fk_propuesta_sede FOREIGN KEY (id_sede) REFERENCES sede(id_sede),
    CONSTRAINT fk_propuesta_revisor FOREIGN KEY (id_revisor) REFERENCES usuario(id_usuario),
    CONSTRAINT ck_propuesta_dias CHECK (dias_semana BETWEEN 1 AND 127),
    CONSTRAINT ck_propuesta_vigencia CHECK (vigente_hasta >= vigente_desde),
    CONSTRAINT ck_propuesta_horas CHECK (hora_fin > hora_inicio AND duracion_minutos > 0),
    CONSTRAINT ck_propuesta_estado CHECK (estado IN ('PENDIENTE','PUBLICADO','RECHAZADO')),
    INDEX idx_propuesta_medico (id_medico, id_propuesta),
    INDEX idx_propuesta_revision (id_sede, estado, id_propuesta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @agregar_propuesta = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='disponibilidad' AND column_name='id_propuesta'),
    'SELECT 1',
    'ALTER TABLE disponibilidad ADD COLUMN id_propuesta INT NULL, ADD CONSTRAINT fk_disponibilidad_propuesta FOREIGN KEY (id_propuesta) REFERENCES propuesta_horario(id_propuesta)'
);
PREPARE migracion_propuesta FROM @agregar_propuesta;
EXECUTE migracion_propuesta;
DEALLOCATE PREPARE migracion_propuesta;
