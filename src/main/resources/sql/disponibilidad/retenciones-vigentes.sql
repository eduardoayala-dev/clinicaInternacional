SELECT r.id_medico, r.inicio, r.duracion_minutos
FROM retencion_cupo r
JOIN medico m ON m.id_medico = r.id_medico
WHERE (:sede = 0 OR m.id_sede = :sede)
  AND r.inicio >= :desde
  AND r.inicio < :hasta
  AND r.vence > :ahora
