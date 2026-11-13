SET NAMES utf8mb4;

ALTER TABLE lista_espera MODIFY COLUMN id_sede INT NULL DEFAULT NULL;
ALTER TABLE lista_espera MODIFY COLUMN canal_notificacion VARCHAR(40) NOT NULL DEFAULT 'CORREO';

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE reasignacion_inteligente ADD COLUMN id_cita_asignada INT NULL DEFAULT NULL, ADD CONSTRAINT fk_reasig_asignada FOREIGN KEY (id_cita_asignada) REFERENCES cita(id_cita)',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'reasignacion_inteligente' AND column_name = 'id_cita_asignada'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lista_espera ADD COLUMN anticipacion_minutos INT NOT NULL DEFAULT 180',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'lista_espera' AND column_name = 'anticipacion_minutos'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lista_espera ADD COLUMN id_medico_preferido INT NULL DEFAULT NULL, ADD CONSTRAINT fk_lista_medico_preferido FOREIGN KEY (id_medico_preferido) REFERENCES medico(id_medico)',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'lista_espera' AND column_name = 'id_medico_preferido'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
