<%@ page import="java.sql.*" %>
<%@ page import="pe.edu.utp.clinica.config.DatabaseConfig" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<body>
<%
    try (Connection conn = DatabaseConfig.getConnection()) {
        out.println("<h3>Usuarios</h3><table border=1><tr><th>ID</th><th>Nombres</th><th>Apellidos</th></tr>");
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id_usuario, nombres, apellidos FROM usuario")) {
            while (rs.next()) {
                out.println("<tr><td>" + rs.getInt(1) + "</td><td>" + rs.getString(2) + "</td><td>" + rs.getString(3) + "</td></tr>");
            }
        }
        out.println("</table>");

        out.println("<h3>Medicos</h3><table border=1><tr><th>ID Medico</th><th>ID Usuario</th><th>CMP</th></tr>");
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id_medico, id_usuario, cmp FROM medico")) {
            while (rs.next()) {
                out.println("<tr><td>" + rs.getInt(1) + "</td><td>" + rs.getInt(2) + "</td><td>" + rs.getString(3) + "</td></tr>");
            }
        }
        out.println("</table>");
    } catch (Exception e) {
        out.println("Error: " + e.getMessage());
    }
%>
</body>
</html>
