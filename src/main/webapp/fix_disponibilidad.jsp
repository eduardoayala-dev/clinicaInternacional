<%@ page import="java.sql.*" %>
<%@ page import="pe.edu.utp.clinica.config.DatabaseConfig" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<body>
<%
    try (Connection conn = DatabaseConfig.getConnection()) {
        // Insertamos disponibilidad para el médico 1 (Carlos Mendoza)
        String sql = "INSERT INTO disponibilidad (id_medico, id_sede, fecha, hora_inicio, hora_fin, duracion_minutos, estado) " +
                     "VALUES (1, 1, DATE_ADD(CURDATE(), INTERVAL ? DAY), '08:00:00', '13:00:00', 30, 'PUBLICADO')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i <= 5; i++) {
                ps.setInt(1, i);
                try {
                    ps.executeUpdate();
                } catch (Exception ignored) { } // Ignore duplicates if any
            }
        }
        out.println("<h3>Disponibilidad insertada con éxito para el Médico 1 (Cardiología)</h3>");
        
        // Insertamos citas falsas en el pasado para el paciente 5 para probar el algoritmo de IA (riesgo)
        String sqlCitaFalta = "INSERT INTO cita (id_paciente, id_medico, id_disponibilidad, fecha_hora, estado, riesgo_cancelacion) " +
                              "VALUES (5, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 'CANCELADA', 'BAJO')";
        try (PreparedStatement ps = conn.prepareStatement(sqlCitaFalta)) {
            ps.executeUpdate();
            ps.executeUpdate();
            ps.executeUpdate(); // 3 faltas
        } catch (Exception ignored) {}

        out.println("<h3>Datos de IA insertados.</h3>");
    } catch (Exception e) {
        out.println("Error: " + e.getMessage());
    }
%>
</body>
</html>
