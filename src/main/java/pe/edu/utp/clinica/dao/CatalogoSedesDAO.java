package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CatalogoSedesDAO {
    public record Sede(
            int id, String codigo, String nombre, String direccion, String telefono, String estado)
            implements java.io.Serializable {}

    public record Fila(int numero, Sede sede, String resultado) implements java.io.Serializable {}

    private final AgendaCitaDAO.ConnectionSource conexiones;

    public CatalogoSedesDAO() {
        this(DatabaseConfig::getConnection);
    }

    CatalogoSedesDAO(AgendaCitaDAO.ConnectionSource conexiones) {
        this.conexiones = conexiones;
    }

    public List<Sede> listar(int actor) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    InvitacionPersonalDAO.administrador(c, actor);
                    return List.copyOf(
                            new pe.edu.utp.clinica.repository.CatalogoSedesRepository(c)
                                    .listar(null));
                });
    }

    public void guardar(int actor, Sede sede, String ip) {
        boolean automatico =
                sede != null
                        && sede.id() == 0
                        && (sede.codigo() == null || sede.codigo().isBlank());
        Sede validada = validar(automatico ? conCodigo(sede, "AUTO") : sede);
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    serializarCodigos(c);
                    InvitacionPersonalDAO.administrador(c, actor);
                    Sede definitiva =
                            automatico
                                    ? conCodigo(validada, codigoDisponible(c, validada.nombre()))
                                    : validada;
                    guardar(c, actor, definitiva, ip, false);
                    return null;
                });
    }

    public List<Fila> importar(int actor, List<Fila> filas, String ip) {
        if (filas == null || filas.isEmpty() || filas.size() > 200)
            throw invalida("Importa entre 1 y 200 filas.");
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    serializarCodigos(c);
                    InvitacionPersonalDAO.administrador(c, actor);
                    var resultado = new ArrayList<Fila>();
                    var vistos = new java.util.HashSet<String>();
                    for (var fila : filas) {
                        var punto = c.setSavepoint();
                        try {
                            if (!fila.resultado().isEmpty()) throw invalida(fila.resultado());
                            Sede sede = validar(fila.sede());
                            if (!vistos.add(sede.codigo()))
                                throw invalida("Código repetido en el archivo.");
                            guardar(c, actor, sede, ip, true);
                            resultado.add(new Fila(fila.numero(), sede, "Guardada"));
                        } catch (AgendaException e) {
                            c.rollback(punto);
                            resultado.add(new Fila(fila.numero(), fila.sede(), e.getMessage()));
                        } finally {
                            c.releaseSavepoint(punto);
                        }
                    }
                    return List.copyOf(resultado);
                });
    }

    private static Sede conCodigo(Sede s, String codigo) {
        return new Sede(s.id(), codigo, s.nombre(), s.direccion(), s.telefono(), s.estado());
    }

    private static void serializarCodigos(Connection c) throws SQLException {
        if (!new pe.edu.utp.clinica.repository.AutorizacionRepository(c)
                .serializarAdministradores())
            throw invalida("Se requiere un administrador registrado.");
    }

    private static String codigoDisponible(Connection c, String nombre) throws SQLException {
        final String base;
        try {
            base = pe.edu.utp.clinica.catalogo.CodigoSede.base(nombre);
        } catch (IllegalArgumentException e) {
            throw invalida(e.getMessage());
        }
        var repo = new pe.edu.utp.clinica.repository.CatalogoSedesRepository(c);
        for (int numero = 1; numero <= 9999; numero++) {
            String codigo =
                    numero == 1 ? base : base + "-" + String.format(Locale.ROOT, "%02d", numero);
            if (repo.porCodigo(codigo, false) == null) return codigo;
        }
        throw invalida("No se pudo generar otro código con esas iniciales.");
    }

    private static void guardar(Connection c, int actor, Sede sede, String ip, boolean porCodigo)
            throws SQLException {
        var repo = new pe.edu.utp.clinica.repository.CatalogoSedesRepository(c);
        int id = sede.id();
        Integer existente = repo.porCodigo(sede.codigo(), true);
        if (existente != null) {
            if (!porCodigo && id != existente)
                throw invalida("El código ya pertenece a otra sede.");
            if (porCodigo) id = existente;
        }
        String nombreAnterior = null;
        if (id > 0) {
            var previas = repo.listar(id);
            if (previas.isEmpty())
                throw new AgendaException(404, "SEDE_NO_ENCONTRADA", "La sede ya no existe.");
            var previa = previas.get(0);
            nombreAnterior = previa.nombre();
            if (!previa.codigo().equals(sede.codigo()))
                throw invalida("El código de una sede registrada no puede cambiarse.");
            if (!"ACTIVO".equals(sede.estado())
                    && repo.agendaActiva(
                            id, java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"))))
                throw new AgendaException(
                        409,
                        "SEDE_CON_AGENDA",
                        "Coordina las citas y los horarios publicados antes de deshabilitar la"
                            + " sede.");
        }
        id = repo.guardar(sede, id);
        if (nombreAnterior != null && !nombreAnterior.equals(sede.nombre()))
            repo.renombrarPreferencias(nombreAnterior, sede.nombre());
        new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                .auditar(
                        actor,
                        ip,
                        "SEDE_GUARDADA",
                        "Sede #" + id + "; código " + sede.codigo() + "; estado " + sede.estado(),
                        false);
    }

    public static Sede validar(Sede s) {
        if (s == null) throw invalida("Completa los datos de la sede.");
        String codigo = texto(s.codigo(), 2, 20).toUpperCase(Locale.ROOT);
        if (!codigo.matches("[A-Z0-9][A-Z0-9_-]{1,19}"))
            throw invalida("El código debe contener letras, números, guion o guion bajo.");
        if (!Set.of("ACTIVO", "INACTIVO", "MANTENIMIENTO")
                .contains(s.estado() == null ? "" : s.estado()))
            throw invalida("Selecciona un estado válido.");
        return new Sede(
                s.id(),
                codigo,
                texto(s.nombre(), 3, 100),
                texto(s.direccion(), 5, 255),
                texto(s.telefono(), 6, 20),
                s.estado());
    }

    private static String texto(String s, int min, int max) {
        String t = s == null ? "" : s.strip();
        if (t.length() < min
                || t.length() > max
                || t.chars().anyMatch(Character::isISOControl)
                || t.contains("<")
                || t.contains(">"))
            throw invalida("Revisa la longitud y el contenido de los datos de la sede.");
        return t;
    }

    private static AgendaException invalida(String mensaje) {
        return new AgendaException(422, "SEDE_INVALIDA", mensaje);
    }
}
