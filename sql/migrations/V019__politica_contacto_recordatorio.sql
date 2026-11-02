ALTER TABLE configuracion_ofertas ADD COLUMN recordatorio_horas INT NOT NULL DEFAULT 48;
ALTER TABLE configuracion_ofertas ADD COLUMN intentos_confirmacion INT NOT NULL DEFAULT 2;
ALTER TABLE configuracion_ofertas ADD CONSTRAINT ck_config_recordatorio CHECK(recordatorio_horas BETWEEN 1 AND 168);
ALTER TABLE configuracion_ofertas ADD CONSTRAINT ck_config_intentos CHECK(intentos_confirmacion BETWEEN 1 AND 10);
