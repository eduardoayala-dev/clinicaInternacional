package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.repository.PrioridadClinicaRepository;
import pe.edu.utp.clinica.repository.ReservaCitaRepository;
import pe.edu.utp.clinica.repository.UsuarioRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

final class PrioridadClinicaJdbc {
    private PrioridadClinicaJdbc() {}

    static String resolver(Connection conexion, int paciente, int especialidad, LocalDate hoy)
            throws SQLException {
        return new ReservaCitaRepository(conexion).prioridad(paciente, especialidad, hoy);
    }

    static void auditar(Connection conexion, int medico, int cita, String prioridad)
            throws SQLException {
        var repo = new PrioridadClinicaRepository(conexion);
        if (repo.vincularEspecialidad(medico, cita) != 1)
            throw new SQLException("No se pudo vincular la especialidad de la evaluación");
        Integer usuario = repo.usuarioMedico(medico);
        if (usuario == null) throw new SQLException("No se pudo auditar la prioridad clínica");
        new UsuarioRepository(conexion)
                .auditar(
                        usuario,
                        null,
                        "PRIORIDAD_CLINICA_REGISTRADA",
                        "Cita #" + cita + "; prioridad " + prioridad,
                        false);
    }
}
