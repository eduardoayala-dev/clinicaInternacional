SELECT c.id_medico, c.fecha_hora AS inicio, d.duracion_minutos
FROM cita c
JOIN disponibilidad d ON d.id_disponibilidad = c.id_disponibilidad
JOIN medico m ON m.id_medico = c.id_medico
WHERE (:sede = 0 OR m.id_sede = :sede)
  AND c.fecha_hora >= :desde
  AND c.fecha_hora < :hasta
  AND c.estado NOT IN ('CANCELADA', 'REPROGRAMADA')
