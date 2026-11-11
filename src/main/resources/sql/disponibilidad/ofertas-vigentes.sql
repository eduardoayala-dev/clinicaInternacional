SELECT c.id_medico, c.fecha_hora AS inicio, d.duracion_minutos
FROM reasignacion_inteligente r
JOIN cita c ON c.id_cita = r.id_cita_original
JOIN disponibilidad d ON d.id_disponibilidad = r.id_disponibilidad
JOIN medico m ON m.id_medico = c.id_medico
WHERE (:sede = 0 OR m.id_sede = :sede)
  AND c.fecha_hora >= :desde
  AND c.fecha_hora < :hasta
  AND r.estado = 'PENDIENTE'
  AND r.fecha_limite_respuesta > :ahora
  AND c.fecha_hora > :ahora
  AND c.estado = 'CANCELADA'
  AND d.estado = 'PUBLICADO'
  AND d.id_sede = m.id_sede
  AND EXISTS (
      SELECT 1
      FROM lista_espera le
      JOIN paciente p ON p.id_paciente = le.id_paciente
      JOIN usuario u ON u.id_usuario = p.id_usuario
      WHERE le.id_lista_espera = r.id_lista_espera
        AND le.estado = 'OFERTADA'
        AND u.activo = TRUE
        AND u.rol = 'PACIENTE'
  )
