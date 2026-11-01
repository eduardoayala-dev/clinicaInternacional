ALTER TABLE atencion_cita ADD COLUMN id_especialidad_prioridad INT NULL;
ALTER TABLE atencion_cita ADD CONSTRAINT fk_atencion_especialidad_prioridad FOREIGN KEY(id_especialidad_prioridad) REFERENCES especialidad(id_especialidad);
