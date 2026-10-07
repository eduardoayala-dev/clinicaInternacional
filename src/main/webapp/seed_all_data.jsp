<%@ page import="java.sql.*" %>
<%@ page import="pe.edu.utp.clinica.config.DatabaseConfig" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>Seeding Database - Clínica Internacional</title></head>
<body style="font-family: sans-serif; padding: 20px; background: #f0f8ff;">
<h2>Poblando Datos de Prueba Completos...</h2>
<pre>
<%
    try (Connection conn = DatabaseConfig.getConnection()) {
        conn.setAutoCommit(false);
        out.println("[+] Conexión establecida.");

        // 1. Asegurar Especialidades
        String[] especialidades = {"Cardiología", "Neurología", "Medicina General", "Traumatología", "Dermatología", "Ginecología"};
        for (int i = 0; i < especialidades.length; i++) {
            int idEsp = i + 1;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO especialidad (id_especialidad, nombre, descripcion, estado) VALUES (?, ?, 'Especialidad Médica', 'ACTIVO') " +
                    "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), estado='ACTIVO'")) {
                ps.setInt(1, idEsp);
                ps.setString(2, especialidades[i]);
                ps.executeUpdate();
            }
        }
        out.println("[+] 6 Especialidades verificadas.");

        // 2. Asegurar Sedes
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sede (id_sede, codigo, nombre, direccion, telefono, estado) VALUES (1, 'SED-001', 'Sede Principal Lima', 'Av. Javier Prado Este 1066', '01-619-6161', 'ACTIVO') " +
                "ON DUPLICATE KEY UPDATE estado='ACTIVO'")) {
            ps.executeUpdate();
        }
        out.println("[+] Sede Principal verificada.");

        // 3. Crear Médicos (2 por especialidad)
        String[][] medicosData = {
            // idUsuario, nombres, apellidos, email, cmp, idEspecialidad
            {"101", "Dra. María", "Rodríguez", "mrodriguez@clinica.pe", "CMP-48291", "1"},
            {"102", "Dr. Carlos", "Mendoza", "cmendoza@clinica.pe", "CMP-39102", "1"},
            {"103", "Dr. Alejandro", "Vargas", "avargas@clinica.pe", "CMP-51204", "2"},
            {"104", "Dra. Sofia", "Benítez", "sbenitez@clinica.pe", "CMP-62819", "2"},
            {"105", "Dr. Fernando", "Alvarez", "falvarez@clinica.pe", "CMP-29401", "3"},
            {"106", "Dra. Lucía", "Morales", "lmorales@clinica.pe", "CMP-71029", "3"},
            {"107", "Dr. Jorge", "Herrera", "jherrera@clinica.pe", "CMP-43910", "4"},
            {"108", "Dra. Patricia", "Salazar", "psalazar@clinica.pe", "CMP-58219", "4"},
            {"109", "Dr. Ricardo", "Guerrero", "rguerrero@clinica.pe", "CMP-31902", "5"},
            {"110", "Dra. Elena", "Castro", "ecastro@clinica.pe", "CMP-69481", "5"},
            {"111", "Dra. Ana", "Gómez", "agomez@clinica.pe", "CMP-50291", "6"},
            {"112", "Dr. Gabriel", "Torres", "gtorres@clinica.pe", "CMP-48102", "6"}
        };

        for (int i = 0; i < medicosData.length; i++) {
            int idUser = Integer.parseInt(medicosData[i][0]);
            String nombres = medicosData[i][1];
            String apellidos = medicosData[i][2];
            String email = medicosData[i][3];
            String cmp = medicosData[i][4];
            int idEsp = Integer.parseInt(medicosData[i][5]);
            int idMed = i + 1;

            // Usuario
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO usuario (id_usuario, nombres, apellidos, email, password_hash, rol, activo) VALUES (?, ?, ?, ?, '123456', 'MEDICO', 1) " +
                    "ON DUPLICATE KEY UPDATE nombres=VALUES(nombres), apellidos=VALUES(apellidos), activo=1")) {
                ps.setInt(1, idUser);
                ps.setString(2, nombres);
                ps.setString(3, apellidos);
                ps.setString(4, email);
                ps.executeUpdate();
            }

            // Médico
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO medico (id_medico, id_usuario, cmp, id_especialidad, id_sede, estado) VALUES (?, ?, ?, ?, 1, 'ACTIVO') " +
                    "ON DUPLICATE KEY UPDATE cmp=VALUES(cmp), id_especialidad=VALUES(id_especialidad), estado='ACTIVO'")) {
                ps.setInt(1, idMed);
                ps.setInt(2, idUser);
                ps.setString(3, cmp);
                ps.setInt(4, idEsp);
                ps.executeUpdate();
            }
        }
        out.println("[+] 12 Médicos creados/actualizados con CMP y nombres completos.");

        // 4. Crear Disponibilidad para los próximos 60 días para todos los médicos
        java.time.LocalDate hoy = java.time.LocalDate.now();
        int dispCount = 0;

        for (int idMed = 1; idMed <= 12; idMed++) {
            for (int dia = 0; dia < 60; dia++) {
                java.time.LocalDate fecha = hoy.plusDays(dia);
                String fechaStr = fecha.toString();

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO disponibilidad (id_medico, id_sede, fecha, hora_inicio, hora_fin, duracion_minutos, estado) " +
                        "VALUES (?, 1, ?, '08:00:00', '13:00:00', 30, 'PUBLICADO') " +
                        "ON DUPLICATE KEY UPDATE estado='PUBLICADO'")) {
                    ps.setInt(1, idMed);
                    ps.setString(2, fechaStr);
                    ps.executeUpdate();
                    dispCount++;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO disponibilidad (id_medico, id_sede, fecha, hora_inicio, hora_fin, duracion_minutos, estado) " +
                        "VALUES (?, 1, ?, '14:00:00', '18:00:00', 30, 'PUBLICADO') " +
                        "ON DUPLICATE KEY UPDATE estado='PUBLICADO'")) {
                    ps.setInt(1, idMed);
                    ps.setString(2, fechaStr);
                    ps.executeUpdate();
                    dispCount++;
                }
            }
        }
        out.println("[+] " + dispCount + " bloques de disponibilidad insertados para los próximos 60 días (2 meses completos).");

        conn.commit();
        out.println("<h3>¡POBLACIÓN DE DATOS EXITOSA!</h3>");
    } catch (Exception e) {
        out.println("<h3 style='color:red;'>ERROR: " + e.getMessage() + "</h3>");
        e.printStackTrace(new java.io.PrintWriter(out));
    }
%>
</pre>
</body>
</html>
