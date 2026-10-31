package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class EsquemaPruebas {
    private EsquemaPruebas() { }
    public static void limpiar(Connection connection) throws SQLException {
        String url=connection.getMetaData().getURL();
        if (url.startsWith("jdbc:mysql:")) {
            MySqlPruebas.validarUrl(url);
            if (!connection.getCatalog().startsWith("clinica_agendas_test_")) throw new SQLException("Catálogo de prueba inválido.");
        } else if (!url.startsWith("jdbc:h2:mem:")) throw new SQLException("Solo se permiten esquemas de prueba.");
        try (var st=connection.createStatement()) {
            for(String table:List.of("correo_cita","verificacion_cuenta","recuperacion_contrasena","limite_recuperacion","sesion_autenticacion","auditoria_seguridad","seguimiento_cita","consulta_clinica","atencion_cita",
                    "inasistencia_cita","reasignacion_inteligente","historial_asistencia","admision_cita",
                    "personal_administrativo","notificacion","lista_espera","cita","motivo_cita",
                    "disponibilidad","propuesta_horario","medico","paciente","sede","especialidad","usuario")) {
                st.execute("DROP TABLE IF EXISTS "+table);
            }
        }
    }
}
