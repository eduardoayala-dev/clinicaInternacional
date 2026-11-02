ALTER TABLE configuracion_ofertas ADD COLUMN confirmacion_horas INT NOT NULL DEFAULT 24;
ALTER TABLE configuracion_ofertas ADD COLUMN tolerancia_llegada INT NOT NULL DEFAULT 10;
ALTER TABLE configuracion_ofertas ADD CONSTRAINT ck_config_confirmacion CHECK(confirmacion_horas BETWEEN 1 AND 168);
ALTER TABLE configuracion_ofertas ADD CONSTRAINT ck_config_tolerancia CHECK(tolerancia_llegada BETWEEN 0 AND 120);
