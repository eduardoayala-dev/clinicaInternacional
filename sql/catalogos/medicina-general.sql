START TRANSACTION;
INSERT INTO especialidad(nombre,descripcion,estado)
SELECT 'Medicina General','Consulta inicial y evaluación general.','ACTIVO'
WHERE NOT EXISTS(SELECT 1 FROM especialidad WHERE nombre='Medicina General');

INSERT INTO motivo_cita(id_especialidad,descripcion,estado)
SELECT e.id_especialidad,n.descripcion,'ACTIVO'
FROM especialidad e
JOIN (
    SELECT 'Evaluación general' AS descripcion
    UNION ALL SELECT 'Dolor de cabeza'
    UNION ALL SELECT 'Fiebre y malestar general'
    UNION ALL SELECT 'Control preventivo'
    UNION ALL SELECT 'Revisión de resultados'
) n ON 1=1
WHERE e.nombre='Medicina General' AND e.estado='ACTIVO'
AND NOT EXISTS(SELECT 1 FROM motivo_cita m WHERE m.id_especialidad=e.id_especialidad AND m.descripcion=n.descripcion);
COMMIT;
