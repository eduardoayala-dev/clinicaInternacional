-- V001: columnas de perfil. Ejecutar con una cuenta de migración y la BD seleccionada.
-- No elimina ni recrea tablas. Se puede repetir; conserva columnas y datos existentes.

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN canal_aviso VARCHAR(20) DEFAULT ''WhatsApp''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'canal_aviso'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN sede_preferida VARCHAR(50) DEFAULT ''Sede San Borja''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'sede_preferida'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN alergias VARCHAR(100) DEFAULT ''''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'alergias'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE paciente ADD COLUMN tipo_cobertura VARCHAR(50) DEFAULT ''EPS''', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'paciente' AND column_name = 'tipo_cobertura'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
