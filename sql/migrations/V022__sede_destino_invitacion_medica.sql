SET @migration_sql = (
    SELECT IF(COUNT(*)=0,
        'ALTER TABLE invitacion_personal ADD COLUMN id_sede_destino INT NULL',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='invitacion_personal' AND column_name='id_sede_destino'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE invitacion_personal i JOIN medico m ON m.id_usuario=i.id_usuario
SET i.id_sede_destino=m.id_sede WHERE i.id_sede_destino IS NULL;
