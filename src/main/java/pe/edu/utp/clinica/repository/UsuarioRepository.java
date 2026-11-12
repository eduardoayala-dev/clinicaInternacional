package pe.edu.utp.clinica.repository;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.JoinType;

import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.PerfilPaciente;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.persistence.PersistenciaJpa;
import pe.edu.utp.clinica.persistence.entity.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UsuarioRepository {
    private final Connection conexion;

    public UsuarioRepository(Connection conexion) {
        this.conexion = conexion;
    }

    public Usuario buscar(String identificador) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var u = q.from(UsuarioEntidad.class);
                    var p = u.join("pacientes", JoinType.LEFT);
                    q.multiselect(
                                    u.get("id"),
                                    u.get("nombres"),
                                    u.get("apellidos"),
                                    u.get("email"),
                                    u.get("passwordHash"),
                                    u.get("rol"),
                                    u.get("registro"),
                                    u.get("activo"))
                            .where(
                                    cb.isTrue(u.get("activo")),
                                    cb.or(
                                            cb.equal(u.get("email"), identificador),
                                            cb.equal(p.get("dni"), identificador)));
                    var filas = sesion.createQuery(q).setMaxResults(1).getResultList();
                    return filas.isEmpty() ? null : usuario(filas.get(0));
                });
    }

    private static Usuario usuario(Tuple f) {
        return new Usuario(
                f.get(0, Integer.class),
                f.get(1, String.class),
                f.get(2, String.class),
                f.get(3, String.class),
                f.get(4, String.class),
                f.get(5, String.class),
                f.get(6, java.time.LocalDateTime.class),
                f.get(7, Boolean.class));
    }

    public int crear(Usuario usuario) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var entidad = new UsuarioEntidad(usuario);
                    sesion.persist(entidad);
                    return entidad.id();
                });
    }

    public int crearPaciente(int usuario, Paciente paciente) throws SQLException {
        return PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var entidad =
                            new PacienteEntidad(
                                    sesion.getReference(UsuarioEntidad.class, usuario), paciente);
                    sesion.persist(entidad);
                    return entidad.id();
                });
    }

    public void auditar(
            Integer usuario, String ip, String evento, String descripcion, boolean sospechoso)
            throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    sesion.persist(
                            new AuditoriaEntidad(usuario, ip, evento, descripcion, sospechoso));
                    return null;
                });
    }

    public boolean existe(String documento, String email, boolean soloCorreo) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createQuery(Integer.class);
                    var u = q.from(UsuarioEntidad.class);
                    var filtro =
                            soloCorreo
                                    ? cb.equal(
                                            cb.lower(u.get("email")),
                                            email.toLowerCase(java.util.Locale.ROOT))
                                    : cb.or(
                                            cb.equal(u.get("email"), email),
                                            cb.equal(
                                                    u.join("pacientes", JoinType.LEFT).get("dni"),
                                                    documento));
                    q.select(u.get("id")).where(filtro);
                    return !sesion.createQuery(q).setMaxResults(1).getResultList().isEmpty();
                });
    }

    public List<Usuario> personal() throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var u = q.from(UsuarioEntidad.class);
                    var m = u.join("medicos", JoinType.LEFT);
                    var i = u.join("invitaciones", JoinType.LEFT);
                    var si = i.join("sede", JoinType.LEFT);
                    var sm = m.join("sede", JoinType.LEFT);
                    var e = m.join("especialidad", JoinType.LEFT);
                    var p = u.join("personal", JoinType.LEFT);
                    var sp = p.join("sede", JoinType.LEFT);
                    var a = u.join("administradores", JoinType.LEFT);
                    q.multiselect(
                                    u.get("id"),
                                    u.get("nombres"),
                                    u.get("apellidos"),
                                    u.get("email"),
                                    cb.literal(""),
                                    u.get("rol"),
                                    u.get("registro"),
                                    u.get("activo"),
                                    m.get("cmp"),
                                    m.get("especialidad").get("id"),
                                    cb.coalesce(sm.get("id"), si.get("id")),
                                    m.get("estado"),
                                    e.get("nombre"),
                                    cb.coalesce(sm.get("nombre"), si.get("nombre")),
                                    p.get("codigo"),
                                    p.get("cargo"),
                                    sp.get("id"),
                                    sp.get("nombre"),
                                    a.get("estado"))
                            .orderBy(cb.asc(u.get("id")));
                    var resultado = new ArrayList<Usuario>();
                    for (var f : sesion.createQuery(q).getResultList()) {
                        var persona = usuario(f);
                        persona.setCmp(f.get(8, String.class));
                        persona.setEspecialidad(f.get(12, String.class));
                        persona.setCodigoEmpleado(f.get(14, String.class));
                        persona.setCargo(f.get(15, String.class));
                        switch (persona.getRol()) {
                            case "MEDICO" -> {
                                persona.setIdEspecialidad(cero(f.get(9, Integer.class)));
                                persona.setIdSede(cero(f.get(10, Integer.class)));
                                persona.setNombreSede(nombreSede(f.get(13, String.class)));
                                var estado = f.get(11, String.class);
                                persona.setEstadoDetallado(
                                        !persona.isActivo() || "INACTIVO".equalsIgnoreCase(estado)
                                                ? "Inactivo"
                                                : "LICENCIA".equalsIgnoreCase(estado)
                                                        ? "Suspendido"
                                                        : "Activo");
                            }
                            case "PERSONAL_ADMINISTRATIVO" -> {
                                persona.setIdSede(cero(f.get(16, Integer.class)));
                                persona.setNombreSede(nombreSede(f.get(17, String.class)));
                                persona.setEstadoDetallado(
                                        persona.isActivo() ? "Activo" : "Inactivo");
                            }
                            case "ADMINISTRADOR" -> {
                                persona.setNombreSede("Multisede");
                                persona.setEstadoDetallado(
                                        !persona.isActivo()
                                                        || "INACTIVO"
                                                                .equalsIgnoreCase(
                                                                        f.get(18, String.class))
                                                ? "Inactivo"
                                                : "Activo");
                            }
                            default ->
                                    persona.setEstadoDetallado(
                                            persona.isActivo() ? "Activo" : "Inactivo");
                        }
                        resultado.add(persona);
                    }
                    return resultado;
                });
    }

    private static int cero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static String nombreSede(String nombre) {
        return nombre == null || nombre.isBlank() ? "Sin sede asignada" : nombre;
    }

    public Map<String, Object> metricas() throws SQLException {
        var resultado = new HashMap<String, Object>();
        var medicos = contarPerfiles(MedicoEntidad.class, true);
        var personal = contarPerfiles(PersonalEntidad.class, false);
        var admins = contarPerfiles(AdministradorEntidad.class, true);
        resultado.put("medicosTotal", medicos[0]);
        resultado.put("medicosActivos", medicos[1]);
        resultado.put("personalTotal", personal[0]);
        resultado.put("personalActivos", personal[1]);
        resultado.put("adminsTotal", admins[0]);
        resultado.put("adminsActivos", admins[1]);
        resultado.put(
                "totalUsuarios",
                PersistenciaJpa.consultar(
                        conexion,
                        sesion -> {
                            var cb = sesion.getCriteriaBuilder();
                            var q = cb.createQuery(Long.class);
                            var u = q.from(UsuarioEntidad.class);
                            q.select(cb.count(u)).where(cb.notEqual(u.get("rol"), "PACIENTE"));
                            return sesion.createQuery(q).getSingleResult().intValue();
                        }));
        return resultado;
    }

    private <T> int[] contarPerfiles(Class<T> tipo, boolean estado) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var p = q.from(tipo);
                    var u = p.join("usuario");
                    var activo = cb.isTrue(u.get("activo"));
                    if (estado)
                        activo =
                                cb.and(
                                        activo,
                                        cb.or(
                                                cb.equal(p.get("estado"), "ACTIVO"),
                                                cb.isNull(p.get("estado"))));
                    q.multiselect(
                            cb.count(p),
                            cb.sum(cb.<Integer>selectCase().when(activo, 1).otherwise(0)));
                    var f = sesion.createQuery(q).getSingleResult();
                    var suma = (Number) f.get(1);
                    return new int[] {
                        ((Number) f.get(0)).intValue(), suma == null ? 0 : suma.intValue()
                    };
                });
    }

    public Paciente paciente(int usuario) throws SQLException {
        return PersistenciaJpa.consultar(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createTupleQuery();
                    var p = q.from(PacienteEntidad.class);
                    q.multiselect(
                                    p.get("id"),
                                    p.get("usuario").get("id"),
                                    p.get("dni"),
                                    p.get("nacimiento"),
                                    p.get("telefono"),
                                    p.get("direccion"),
                                    p.get("canal"),
                                    p.get("sedePreferida"),
                                    p.get("alergias"),
                                    p.get("cobertura"))
                            .where(cb.equal(p.get("usuario").get("id"), usuario));
                    var filas = sesion.createQuery(q).setMaxResults(1).getResultList();
                    if (filas.isEmpty()) return null;
                    var f = filas.get(0);
                    return new Paciente(
                            f.get(0, Integer.class),
                            f.get(1, Integer.class),
                            f.get(2, String.class),
                            f.get(3, java.time.LocalDate.class),
                            f.get(4, String.class),
                            f.get(5, String.class),
                            f.get(6, String.class),
                            f.get(7, String.class),
                            f.get(8, String.class),
                            f.get(9, String.class));
                });
    }

    public void guardarPerfil(PerfilPaciente perfil) throws SQLException {
        PersistenciaJpa.escribir(
                conexion,
                sesion -> {
                    var cb = sesion.getCriteriaBuilder();
                    var q = cb.createCriteriaUpdate(PacienteEntidad.class);
                    var p = q.from(PacienteEntidad.class);
                    q.set("telefono", perfil.telefono())
                            .set("direccion", perfil.direccion())
                            .set("canal", "Correo")
                            .set(
                                    "sedePreferida",
                                    perfil.sedePreferida() != null
                                            ? perfil.sedePreferida()
                                            : "Sede San Borja")
                            .set("alergias", perfil.alergias() != null ? perfil.alergias() : "")
                            .set(
                                    "cobertura",
                                    perfil.tipoCobertura() != null ? perfil.tipoCobertura() : "EPS")
                            .where(cb.equal(p.get("usuario").get("id"), perfil.idUsuario()));
                    sesion.createMutationQuery(q).executeUpdate();
                    return null;
                });
    }
}
