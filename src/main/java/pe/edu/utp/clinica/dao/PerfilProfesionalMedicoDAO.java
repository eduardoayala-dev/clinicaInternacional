package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public final class PerfilProfesionalMedicoDAO {
    public record Cuenta(int id, String nombre, String correo, boolean activa) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;

    public PerfilProfesionalMedicoDAO() {
        this(DatabaseConfig::getConnection);
    }

    PerfilProfesionalMedicoDAO(AgendaCitaDAO.ConnectionSource conexiones) {
        this.conexiones = conexiones;
    }

    public List<Cuenta> pendientes(int actor) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    int sede = AmbitoAdmisionDAO.sede(c, actor);
                    return List.copyOf(
                            new pe.edu.utp.clinica.repository.PerfilProfesionalRepository(c)
                                    .pendientes(sede));
                });
    }

    public void completar(int actor, int cuenta, String cmp, int especialidad, String ip) {
        String colegiatura = cmp == null ? "" : cmp.strip();
        if (cuenta < 1 || especialidad < 1 || !colegiatura.matches("[A-Za-z0-9-]{4,20}"))
            throw new AgendaException(
                    400,
                    "PERFIL_INVALIDO",
                    "Selecciona una cuenta, una especialidad y un CMP válido.");
        try {
            TransaccionJdbc.ejecutar(
                    conexiones,
                    c -> {
                        int sede = AmbitoAdmisionDAO.sede(c, actor);
                        var repo = new pe.edu.utp.clinica.repository.PerfilProfesionalRepository(c);
                        if (!repo.invitacionEnSede(cuenta, sede))
                            throw new AgendaException(
                                    403,
                                    "SEDE_NO_AUTORIZADA",
                                    "El administrador debe asignar esta cuenta a tu sede antes de"
                                        + " completar su perfil.");
                        if (!repo.cuentaMedica(cuenta))
                            throw new AgendaException(
                                    404,
                                    "CUENTA_NO_ENCONTRADA",
                                    "Selecciona una cuenta médica creada por el administrador.");
                        if (repo.existePerfil(cuenta, colegiatura))
                            throw new AgendaException(
                                    409,
                                    "PERFIL_EXISTENTE",
                                    "La cuenta ya tiene perfil profesional o el CMP ya está"
                                        + " registrado.");
                        if (!repo.especialidadActiva(especialidad))
                            throw new AgendaException(
                                    409,
                                    "ESPECIALIDAD_INACTIVA",
                                    "Selecciona una especialidad activa.");
                        repo.crear(cuenta, colegiatura, especialidad, sede);
                        new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                                .auditar(
                                        actor,
                                        ip,
                                        "PERFIL_MEDICO_COMPLETADO",
                                        "Cuenta #"
                                                + cuenta
                                                + "; sede #"
                                                + sede
                                                + "; especialidad #"
                                                + especialidad,
                                        false);
                        return null;
                    });
        } catch (AgendaException e) {
            throw e;
        }
    }
}
