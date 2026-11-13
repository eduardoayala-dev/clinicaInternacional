ALTER TABLE correo_cambio_cita ADD CONSTRAINT ck_aviso_cambio_tipo_v2 CHECK(tipo IN ('CANCELADA','REPROGRAMADA','CONFIRMACION_VENCIDA'));
ALTER TABLE correo_cambio_cita DROP CONSTRAINT ck_aviso_cambio_tipo;
