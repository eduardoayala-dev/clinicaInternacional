ALTER TABLE configuracion_ofertas ALTER COLUMN recordatorio_horas SET DEFAULT 24;
UPDATE configuracion_ofertas SET recordatorio_horas=24 WHERE id_configuracion=1 AND version=0 AND recordatorio_horas=48;
