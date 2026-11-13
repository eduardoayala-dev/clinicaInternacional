package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.correo.Correo;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.recuperacion.VerificacionJdbc;
import pe.edu.utp.clinica.repository.InvitacionPersonalRepository;
import pe.edu.utp.clinica.util.SecurityUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.Locale;
import java.util.Set;

public class InvitacionPersonalDAO {
    private static final String ROL_MEDICO = "MEDICO";
    private static final String ROL_ADMISION = "PERSONAL_ADMINISTRATIVO";
    private static final String ROL_ADMIN = "ADMINISTRADOR";
    private final AgendaCitaDAO.ConnectionSource conexiones;
    private final Clock reloj;

    public record Solicitud(
            String nombres,
            String apellidos,
            String correo,
            String rol,
            String cmp,
            Integer especialidad,
            Integer sede,
            String cargo) {}

    public record Entrega(int usuario, String correo) {
        @Override
        public String toString() {
            return "Entrega[privada]";
        }
    }

    public InvitacionPersonalDAO() {
        this(DatabaseConfig::getConnection, Clock.systemUTC());
    }

    InvitacionPersonalDAO(AgendaCitaDAO.ConnectionSource conexiones, Clock reloj) {
        this.conexiones = conexiones;
        this.reloj = reloj;
    }

    public Entrega crear(int actor, Solicitud s, byte[] token) {
        return crearValidado(actor, s, token, false);
    }

    public Entrega crearAcceso(int actor, Solicitud s, byte[] token) {
        if (s == null) throw error(400, "DATOS_INVALIDOS", "Completa los datos de acceso.");
        boolean accesoMedico = ROL_MEDICO.equals(s.rol());
        var datos =
                accesoMedico
                        ? new Solicitud(
                                s.nombres(),
                                s.apellidos(),
                                s.correo(),
                                ROL_MEDICO,
                                null,
                                null,
                                s.sede(),
                                null)
                        : s;
        return crearValidado(actor, datos, token, accesoMedico);
    }

    private Entrega crearValidado(int actor, Solicitud s, byte[] token, boolean accesoMedico) {
        if (accesoMedico) validarIdentidad(s);
        else validar(s);
        validarHash(token);
        String hash = SecurityUtil.hashPassword(SecretosSesion.nuevo());
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    administrador(c, actor);
                    var repo = new InvitacionPersonalRepository(c);
                    if (accesoMedico) validarSedeDestino(c, s.sede());
                    else validarCatalogos(c, s, 0);
                    if (new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                            .existe(null, s.correo().trim(), true))
                        throw error(
                                409,
                                "CORREO_EXISTENTE",
                                "El correo ya pertenece a una cuenta. No se modificó esa cuenta.");
                    int id = repo.crearUsuario(s, hash);
                    if (!accesoMedico) repo.crearPerfil(id, s);
                    guardarInvitacion(c, id, actor, token, false);
                    if (accesoMedico) asignarDestino(c, id, s.sede());
                    auditar(c, actor, "PERSONAL_INVITADO", id);
                    return new Entrega(id, s.correo().trim().toLowerCase(Locale.ROOT));
                });
    }

    public Entrega reenviar(int actor, int usuario, byte[] token) {
        validarHash(token);
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    administrador(c, actor);
                    var invitacion =
                            new InvitacionPersonalRepository(c).invitacion(usuario, null, true);
                    if (invitacion == null || invitacion.activa() || invitacion.utilizada() != null)
                        throw error(
                                409,
                                "INVITACION_NO_REENVIABLE",
                                "Solo puedes reenviar una invitación pendiente de activación.");
                    if (invitacion.creada().plusSeconds(60).isAfter(ahora()))
                        throw error(429, "ESPERA_REENVIO", "Espera un minuto antes de reenviar.");
                    guardarInvitacion(c, usuario, actor, token, true);
                    auditar(c, actor, "INVITACION_REENVIADA", usuario);
                    return new Entrega(usuario, invitacion.correo());
                });
    }

    public void registrarEnvio(int usuario, byte[] token, boolean enviado) {
        try (var c = conexiones.open()) {
            PersistenciaJpa.transaccion(
                    c,
                    () -> {
                        new InvitacionPersonalRepository(c).registrarEnvio(usuario, token, enviado);
                        return null;
                    });
        } catch (SQLException e) {
            throw error(
                    503,
                    "ENVIO_NO_REGISTRADO",
                    "La cuenta está pendiente; no se pudo registrar el resultado del correo.");
        }
    }

    public boolean vigente(byte[] token) {
        if (token == null || token.length != 32) return false;
        try (var c = conexiones.open()) {
            var i = new InvitacionPersonalRepository(c).invitacion(null, token, false);
            return i != null
                    && !i.activa()
                    && i.utilizada() == null
                    && i.revocada() == null
                    && ahora().isBefore(i.vence());
        } catch (SQLException e) {
            throw error(503, "INVITACION_NO_DISPONIBLE", "No se pudo comprobar la invitación.");
        }
    }

    public java.util.Map<Integer, String> estados(int actor) {
        try (var c = conexiones.open()) {
            administrador(c, actor);
            return new InvitacionPersonalRepository(c).estados(ahora());
        } catch (SQLException e) {
            throw error(
                    503,
                    "INVITACION_NO_DISPONIBLE",
                    "No se pudo consultar el estado de las invitaciones.");
        }
    }

    public void activar(byte[] token, String password) {
        validarHash(token);
        if (!VerificacionJdbc.passwordValida(password))
            throw error(
                    400,
                    "PASSWORD_INVALIDA",
                    "Usa al menos 12 caracteres, mayúscula, minúscula, número y símbolo; máximo 72"
                        + " bytes.");
        String hash = SecurityUtil.hashPassword(password);
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    var repo = new InvitacionPersonalRepository(c);
                    var i = repo.invitacion(null, token, true);
                    if (i == null
                            || i.activa()
                            || i.utilizada() != null
                            || i.revocada() != null
                            || !ahora().isBefore(i.vence())) throw invalida();
                    if (repo.activar(i.usuario(), hash, ahora()) != 1) throw invalida();
                    auditar(c, i.usuario(), "PERSONAL_ACTIVADO", i.usuario());
                    return null;
                });
    }

    public void cambiarEstado(int actor, int usuario, boolean activo) {
        if (actor == usuario)
            throw error(
                    409,
                    "CAMBIO_PROPIO_NO_PERMITIDO",
                    "No puedes cambiar el estado de tu propia cuenta.");
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    administrador(c, actor);
                    var repo = new InvitacionPersonalRepository(c);
                    var persona = repo.persona(usuario);
                    if (persona == null || "PACIENTE".equals(persona.rol()))
                        throw error(
                                404,
                                "PERSONAL_NO_ENCONTRADO",
                                "No se encontró una cuenta de personal.");
                    if (activo) validarActivacionPanel(c, usuario);
                    else {
                        validarBajaMedico(c, usuario);
                        revocar(c, usuario);
                    }
                    repo.actualizarUsuario(usuario, null, null, activo);
                    auditar(
                            c,
                            actor,
                            activo ? "PERSONAL_HABILITADO" : "PERSONAL_DESHABILITADO",
                            usuario);
                    return null;
                });
    }

    public void editar(int actor, int usuario, Solicitud solicitud, boolean activo) {
        validar(solicitud);
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    administrador(c, actor);
                    boolean anterior = validarEdicion(c, actor, usuario, solicitud, activo);
                    if (anterior && !activo) validarBajaMedico(c, usuario);
                    validarCatalogos(c, solicitud, usuario);
                    validarCambioDeAgenda(c, usuario, solicitud);
                    var repo = new InvitacionPersonalRepository(c);
                    repo.actualizarUsuario(
                            usuario, solicitud.nombres(), solicitud.apellidos(), activo);
                    if (repo.actualizarPerfil(usuario, solicitud) != 1)
                        throw error(
                                409, "PERFIL_INVALIDO", "No se encontró el perfil profesional.");
                    if (!activo && anterior) revocar(c, usuario);
                    auditar(c, actor, "PERSONAL_EDITADO", usuario);
                    return null;
                });
    }

    public void editarAcceso(int actor, int usuario, Solicitud s, boolean activo) {
        if (!ROL_MEDICO.equals(s.rol())) {
            editar(actor, usuario, s, activo);
            return;
        }
        validarIdentidad(s);
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    administrador(c, actor);
                    boolean anterior = validarEdicion(c, actor, usuario, s, activo);
                    var repo = new InvitacionPersonalRepository(c);
                    var perfil = repo.medico(usuario, true);
                    if (perfil == null) {
                        validarSedeDestino(c, s.sede());
                        asignarDestino(c, usuario, s.sede());
                    } else if (s.sede() != null && s.sede() != perfil.sede())
                        throw error(
                                409,
                                "MEDICO_CON_PERFIL",
                                "El cambio de sede requiere coordinar el perfil profesional y su"
                                    + " agenda.");
                    if (anterior && !activo) validarBajaMedico(c, usuario);
                    repo.actualizarUsuario(usuario, s.nombres(), s.apellidos(), activo);
                    if (!activo && anterior) revocar(c, usuario);
                    auditar(c, actor, "ACCESO_MEDICO_EDITADO", usuario);
                    return null;
                });
    }

    private static boolean validarEdicion(
            Connection c, int actor, int usuario, Solicitud s, boolean activo) throws SQLException {
        var persona = new InvitacionPersonalRepository(c).persona(usuario);
        if (persona == null || "PACIENTE".equals(persona.rol()))
            throw error(404, "PERSONAL_NO_ENCONTRADO", "No se encontró una cuenta de personal.");
        if (!s.rol().equals(persona.rol()) || !s.correo().trim().equalsIgnoreCase(persona.correo()))
            throw error(
                    400,
                    "IDENTIDAD_NO_MODIFICABLE",
                    "El rol y el correo no se cambian desde esta edición.");
        if (actor == usuario && !activo)
            throw error(
                    409, "CAMBIO_PROPIO_NO_PERMITIDO", "No puedes deshabilitar tu propia cuenta.");
        if (activo) validarActivacionPanel(c, usuario);
        return persona.activa();
    }

    private static void validarActivacionPanel(Connection c, int usuario) throws SQLException {
        if (new InvitacionPersonalRepository(c).activacionPendiente(usuario))
            throw error(
                    409, "ACTIVACION_PENDIENTE", "Debe crear su contraseña desde la invitación.");
    }

    private static void validarSedeDestino(Connection c, Integer sede) throws SQLException {
        if (sede == null || sede < 1)
            throw error(
                    400, "SEDE_REQUERIDA", "Selecciona la sede que completará el perfil médico.");
        if (!new InvitacionPersonalRepository(c).sedeActiva(sede, true))
            throw error(400, "SEDE_INVALIDA", "Selecciona una sede activa.");
    }

    private static void asignarDestino(Connection c, int usuario, int sede) throws SQLException {
        if (new InvitacionPersonalRepository(c).asignarDestino(usuario, sede) != 1)
            throw error(
                    409,
                    "INVITACION_REQUERIDA",
                    "La cuenta no tiene una invitación para asignar la sede.");
    }

    private void validarCambioDeAgenda(Connection c, int usuario, Solicitud s) throws SQLException {
        if (!ROL_MEDICO.equals(s.rol())) return;
        var repo = new InvitacionPersonalRepository(c);
        var perfil = repo.medico(usuario, false);
        if (perfil != null
                && (perfil.sede() != s.sede() || perfil.especialidad() != s.especialidad())
                && repo.agendaImpideCambio(perfil.id(), ahoraClinica(), false))
            throw error(
                    409,
                    "MEDICO_CON_AGENDA",
                    "Coordina la agenda vigente antes de cambiar la sede o especialidad del"
                        + " médico.");
    }

    private void validarBajaMedico(Connection c, int usuario) throws SQLException {
        var repo = new InvitacionPersonalRepository(c);
        var perfil = repo.medico(usuario, false);
        if (perfil != null && repo.agendaImpideCambio(perfil.id(), ahoraClinica(), true))
            throw error(
                    409,
                    "MEDICO_CON_CITAS",
                    "Coordina las citas pendientes antes de deshabilitar al médico.");
    }

    private void revocar(Connection c, int usuario) throws SQLException {
        new InvitacionPersonalRepository(c).revocar(usuario, ahora());
    }

    static void administrador(Connection c, int actor) throws SQLException {
        if (!ROL_ADMIN.equals(
                new pe.edu.utp.clinica.repository.AutorizacionRepository(c).rolActivo(actor)))
            throw error(403, "ADMINISTRADOR_REQUERIDO", "Se requiere un administrador activo.");
    }

    private void guardarInvitacion(
            Connection c, int usuario, int actor, byte[] token, boolean actualizar)
            throws SQLException {
        if (new InvitacionPersonalRepository(c)
                        .guardarInvitacion(usuario, actor, token, actualizar, ahora())
                != 1) throw invalida();
    }

    private static void validarCatalogos(Connection c, Solicitud s, int excluir)
            throws SQLException {
        if (ROL_ADMIN.equals(s.rol())) return;
        var repo = new InvitacionPersonalRepository(c);
        if (!repo.sedeActiva(s.sede(), false))
            throw error(400, "SEDE_INVALIDA", "Selecciona una sede activa.");
        if (ROL_MEDICO.equals(s.rol())) {
            if (!new pe.edu.utp.clinica.repository.ListaEsperaRepository(c)
                    .catalogoActivo(s.especialidad(), false))
                throw error(400, "ESPECIALIDAD_INVALIDA", "Selecciona una especialidad activa.");
            if (repo.cmpOcupado(s.cmp().trim(), excluir))
                throw error(409, "CMP_EXISTENTE", "El CMP ya está registrado.");
        }
    }

    private static void validar(Solicitud s) {
        validarIdentidad(s);
        if (!ROL_ADMIN.equals(s.rol()) && (s.sede() == null || s.sede() <= 0))
            throw error(400, "SEDE_INVALIDA", "Selecciona una sede.");
        if (ROL_MEDICO.equals(s.rol())
                && (s.especialidad() == null
                        || s.especialidad() <= 0
                        || s.cmp() == null
                        || !s.cmp().trim().matches("[A-Za-z0-9-]{4,20}")))
            throw error(400, "MEDICO_INVALIDO", "Completa CMP y especialidad.");
    }

    private static void validarIdentidad(Solicitud s) {
        if (s == null
                || !texto(s.nombres(), 100)
                || !texto(s.apellidos(), 100)
                || s.rol() == null
                || !Set.of(ROL_MEDICO, ROL_ADMISION, ROL_ADMIN).contains(s.rol()))
            throw error(400, "DATOS_INVALIDOS", "Revisa los nombres y el rol del personal.");
        try {
            Correo.validarDireccion(s.correo() == null ? null : s.correo().trim());
        } catch (IllegalArgumentException e) {
            throw error(400, "CORREO_INVALIDO", "Introduce un correo válido.");
        }
        if (s.correo().trim().length() > 150)
            throw error(400, "CORREO_INVALIDO", "El correo supera 150 caracteres.");
        if (s.cargo() != null && s.cargo().length() > 100)
            throw error(400, "CARGO_INVALIDO", "El cargo supera 100 caracteres.");
    }

    private static boolean texto(String s, int max) {
        return s != null
                && !s.isBlank()
                && s.length() <= max
                && s.chars().noneMatch(c -> Character.isISOControl(c) || c == '<' || c == '>');
    }

    private static void validarHash(byte[] hash) {
        if (hash == null || hash.length != 32) throw invalida();
    }

    private static void auditar(Connection c, int actor, String evento, int objetivo)
            throws SQLException {
        new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                .auditar(actor, null, evento, "Cuenta de personal #" + objetivo, false);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
    }

    private LocalDateTime ahoraClinica() {
        return LocalDateTime.ofInstant(reloj.instant(), ZoneId.of("America/Lima"));
    }

    private static AgendaException invalida() {
        return error(
                410,
                "INVITACION_INVALIDA",
                "La invitación no es válida, venció o ya se utilizó. Solicita otra al"
                    + " administrador.");
    }

    private static AgendaException error(int estado, String codigo, String mensaje) {
        return new AgendaException(estado, codigo, mensaje);
    }
}
