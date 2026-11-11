SELECT d.id_medico, d.fecha, d.hora_inicio, d.hora_fin, d.duracion_minutos
FROM disponibilidad d
JOIN medico m ON m.id_medico = d.id_medico
JOIN usuario u ON u.id_usuario = m.id_usuario
JOIN especialidad e ON e.id_especialidad = m.id_especialidad
JOIN sede s ON s.id_sede = m.id_sede
WHERE (:sede = 0 OR d.id_sede = :sede)
  AND d.fecha >= :desde
  AND d.fecha < :hasta
  AND d.estado = 'PUBLICADO'
  AND d.id_sede = m.id_sede
  AND m.estado = 'ACTIVO'
  AND u.activo = TRUE
  AND e.estado = 'ACTIVO'
  AND s.estado = 'ACTIVO'
