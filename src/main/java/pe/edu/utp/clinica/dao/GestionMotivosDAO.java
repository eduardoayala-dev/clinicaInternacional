package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GestionMotivosDAO {
    private static final String ACTIVO = "ACTIVO";

    public record Especialidad(int id, String nombre) {}

    public record Motivo(
            int id,
            int especialidad,
            String nombreEspecialidad,
            String descripcion,
            String estado,
            boolean utilizado) {}

    public record Filtro(int especialidad, String estado, String busqueda, int pagina) {
        public Filtro {
            estado = estado == null ? "" : estado;
            busqueda = busqueda == null ? "" : busqueda.strip();
            if (especialidad < 0
                    || pagina < 1
                    || !Set.of("", ACTIVO, "INACTIVO").contains(estado)
                    || busqueda.length() > 80
                    || busqueda.chars().anyMatch(Character::isISOControl))
                throw new AgendaException(
                        400, "FILTRO_INVALIDO", "Revisa los filtros del catálogo.");
        }
    }

    public record Panel(
            boolean administrador,
            boolean puedeEditar,
            List<Especialidad> especialidades,
            List<Motivo> motivos,
            Filtro filtro,
            long total,
            int paginas) {}

    private final AgendaCitaDAO.ConnectionSource conexiones;

    public GestionMotivosDAO() {
        this(DatabaseConfig::getConnection);
    }

    GestionMotivosDAO(AgendaCitaDAO.ConnectionSource conexiones) {
        this.conexiones = conexiones;
    }

    public Panel consultar(int actor, Filtro filtro) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    boolean administrador = autorizar(c, actor);
                    var repo = new pe.edu.utp.clinica.repository.GestionMotivosRepository(c);
                    var especialidades = repo.especialidades();
                    long total = repo.contar(filtro);
                    int paginas = (int) Math.max(1, (total + 19) / 20);
                    int pagina = Math.min(filtro.pagina(), paginas);
                    var motivos =
                            repo.listar(
                                    new Filtro(
                                            filtro.especialidad(),
                                            filtro.estado(),
                                            filtro.busqueda(),
                                            pagina),
                                    null,
                                    false);
                    return new Panel(
                            administrador,
                            true,
                            List.copyOf(especialidades),
                            List.copyOf(motivos),
                            new Filtro(
                                    filtro.especialidad(),
                                    filtro.estado(),
                                    filtro.busqueda(),
                                    pagina),
                            total,
                            paginas);
                });
    }

    public int guardar(int actor, int id, int especialidad, String descripcion, String ip) {
        String texto = descripcionValida(id, especialidad, descripcion);
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    autorizar(c, actor);
                    especialidad(c, especialidad, true);
                    if (id > 0 && sinCambios(c, id, especialidad, texto)) return id;
                    exigirUnico(c, id, especialidad, texto);
                    int resultado =
                            id == 0 ? insertar(c, especialidad, texto) : actualizar(c, id, texto);
                    auditar(
                            c,
                            actor,
                            ip,
                            id == 0 ? "MOTIVO_CREADO" : "MOTIVO_EDITADO",
                            resultado,
                            especialidad);
                    return resultado;
                });
    }

    public List<Especialidad> especialidadesImportacion(int actor) {
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    autorizar(c, actor);
                    return List.copyOf(
                            new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                                    .especialidades());
                });
    }

    public List<FilaImportacionMotivo> revisarImportacion(
            int actor, List<FilaImportacionMotivo> filas) {
        exigirFilas(filas);
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    autorizar(c, actor);
                    List<FilaImportacionMotivo> revisadas = new ArrayList<>();
                    java.util.Map<Integer, List<String>> vistos = new java.util.HashMap<>();
                    java.util.Set<Integer> activas = new java.util.HashSet<>();
                    new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                            .especialidades()
                            .forEach(e -> activas.add(e.id()));
                    for (var fila : filas) {
                        revisadas.add(revisarFila(c, fila, vistos, activas));
                    }
                    return List.copyOf(revisadas);
                });
    }

    private static FilaImportacionMotivo revisarFila(
            Connection c,
            FilaImportacionMotivo fila,
            java.util.Map<Integer, List<String>> vistos,
            java.util.Set<Integer> activas)
            throws SQLException {
        if (!fila.error().isEmpty()) return fila;
        try {
            String texto = descripcionValida(0, fila.especialidad(), fila.descripcion());
            if (!activas.contains(fila.especialidad()))
                throw new AgendaException(
                        409, "ESPECIALIDAD_NO_DISPONIBLE", "La especialidad no está activa.");
            exigirUnico(c, 0, fila.especialidad(), texto);
            var anteriores = vistos.computeIfAbsent(fila.especialidad(), id -> new ArrayList<>());
            if (repetido(c, fila.especialidad(), anteriores, texto))
                throw new AgendaException(
                        409,
                        "MOTIVO_DUPLICADO",
                        "El motivo se repite en el archivo para la misma especialidad.");
            anteriores.add(texto);
            return new FilaImportacionMotivo(fila.numero(), fila.especialidad(), texto, "");
        } catch (AgendaException e) {
            return new FilaImportacionMotivo(
                    fila.numero(), fila.especialidad(), fila.descripcion(), e.getMessage());
        }
    }

    private static boolean repetido(
            Connection c, int especialidad, List<String> anteriores, String texto)
            throws SQLException {
        return new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                .repetido(especialidad, anteriores, texto);
    }

    public record ResultadoImportacion(int importados, List<FilaImportacionMotivo> filas)
            implements java.io.Serializable {
        public ResultadoImportacion {
            filas = List.copyOf(filas);
        }
    }

    public ResultadoImportacion importar(int actor, List<FilaImportacionMotivo> filas, String ip) {
        exigirFilas(filas);
        return TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    autorizar(c, actor);
                    java.util.Set<Integer> activas = new java.util.HashSet<>();
                    for (int id :
                            filas.stream()
                                    .filter(f -> f.error().isEmpty())
                                    .mapToInt(FilaImportacionMotivo::especialidad)
                                    .distinct()
                                    .sorted()
                                    .toArray()) {
                        try {
                            especialidad(c, id, true);
                            activas.add(id);
                        } catch (AgendaException e) {
                            if (e.status() != 409) throw e;
                        }
                    }
                    List<FilaImportacionMotivo> resultado = new ArrayList<>();
                    int importados = 0;
                    for (var fila : filas) {
                        var revisada = importarFila(c, actor, fila, ip, activas);
                        resultado.add(revisada);
                        if (revisada.error().isEmpty()) importados++;
                    }
                    return new ResultadoImportacion(importados, resultado);
                });
    }

    private static FilaImportacionMotivo importarFila(
            Connection c,
            int actor,
            FilaImportacionMotivo fila,
            String ip,
            java.util.Set<Integer> activas)
            throws SQLException {
        if (!fila.error().isEmpty()) return fila;
        var punto = c.setSavepoint();
        try {
            if (!activas.contains(fila.especialidad()))
                throw new AgendaException(
                        409, "ESPECIALIDAD_NO_DISPONIBLE", "La especialidad ya no está activa.");
            String texto = descripcionValida(0, fila.especialidad(), fila.descripcion());
            exigirUnico(c, 0, fila.especialidad(), texto);
            int id = insertar(c, fila.especialidad(), texto);
            auditar(c, actor, ip, "MOTIVO_IMPORTADO", id, fila.especialidad());
            return new FilaImportacionMotivo(fila.numero(), fila.especialidad(), texto, "");
        } catch (AgendaException e) {
            c.rollback(punto);
            return new FilaImportacionMotivo(
                    fila.numero(), fila.especialidad(), fila.descripcion(), e.getMessage());
        } finally {
            c.releaseSavepoint(punto);
        }
    }

    private static void exigirFilas(List<FilaImportacionMotivo> filas) {
        if (filas == null
                || filas.isEmpty()
                || filas.size() > 200
                || filas.stream().anyMatch(java.util.Objects::isNull))
            throw new AgendaException(
                    422, "IMPORTACION_INVALIDA", "Selecciona entre 1 y 200 filas válidas.");
    }

    private static String descripcionValida(int id, int especialidad, String descripcion) {
        String texto = descripcion == null ? "" : descripcion.strip().replaceAll(" +", " ");
        if (id < 0
                || especialidad <= 0
                || texto.length() < 3
                || texto.length() > 200
                || texto.chars()
                        .anyMatch(ch -> Character.isISOControl(ch) || ch == '<' || ch == '>'))
            throw new AgendaException(
                    400,
                    "MOTIVO_INVALIDO",
                    "Escribe un motivo de 3 a 200 caracteres, sin etiquetas ni caracteres de"
                        + " control.");
        return texto;
    }

    private static boolean sinCambios(Connection c, int id, int especialidad, String texto)
            throws SQLException {
        Motivo previo = motivo(c, id);
        if (previo.especialidad() != especialidad)
            throw new AgendaException(
                    409,
                    "ESPECIALIDAD_INMUTABLE",
                    "Crea otro motivo para cambiar de especialidad.");
        if (previo.utilizado() && !previo.descripcion().equals(texto))
            throw new AgendaException(
                    409,
                    "MOTIVO_CON_HISTORIAL",
                    "Este motivo ya aparece en citas. Desactívalo y crea otro para conservar el"
                        + " historial.");
        return previo.descripcion().equals(texto);
    }

    private static void exigirUnico(Connection c, int id, int especialidad, String texto)
            throws SQLException {
        if (new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                .duplicado(id, especialidad, texto))
            throw new AgendaException(
                    409,
                    "MOTIVO_DUPLICADO",
                    "Ya existe ese motivo en la especialidad, incluso si está inactivo.");
    }

    private static int insertar(Connection c, int especialidad, String texto) throws SQLException {
        return new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                .crear(especialidad, texto);
    }

    private static int actualizar(Connection c, int id, String texto) throws SQLException {
        new pe.edu.utp.clinica.repository.GestionMotivosRepository(c).actualizar(id, texto, false);
        return id;
    }

    public void estado(int actor, int id, String estado, String ip) {
        if (id <= 0 || !Set.of(ACTIVO, "INACTIVO").contains(estado == null ? "" : estado))
            throw new AgendaException(400, "ESTADO_INVALIDO", "Selecciona un estado válido.");
        TransaccionJdbc.ejecutar(
                conexiones,
                c -> {
                    autorizar(c, actor);
                    Integer especialidad =
                            new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                                    .especialidadMotivo(id);
                    if (especialidad == null) throw noEncontrado();
                    especialidad(c, especialidad, ACTIVO.equals(estado));
                    var previo = motivo(c, id);
                    if (estado.equals(previo.estado())) return null;
                    new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                            .actualizar(id, estado, true);
                    auditar(
                            c,
                            actor,
                            ip,
                            ACTIVO.equals(estado) ? "MOTIVO_ACTIVADO" : "MOTIVO_DESACTIVADO",
                            id,
                            especialidad);
                    return null;
                });
    }

    private static boolean autorizar(Connection c, int actor) throws SQLException {
        String rol = new pe.edu.utp.clinica.repository.AutorizacionRepository(c).rolActivo(actor);
        if ("ADMINISTRADOR".equals(rol)) return true;
        if ("PERSONAL_ADMINISTRATIVO".equals(rol)) {
            AmbitoAdmisionDAO.sede(c, actor);
            return false;
        }
        throw denegado();
    }

    private static void especialidad(Connection c, int id, boolean activa) throws SQLException {
        String estado =
                new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                        .bloquearEspecialidad(id);
        if (estado == null || (activa && !ACTIVO.equals(estado)))
            throw new AgendaException(
                    409, "ESPECIALIDAD_NO_DISPONIBLE", "La especialidad no está activa.");
    }

    private static Motivo motivo(Connection c, int id) throws SQLException {
        var motivos =
                new pe.edu.utp.clinica.repository.GestionMotivosRepository(c)
                        .listar(null, id, true);
        if (motivos.isEmpty()) throw noEncontrado();
        return motivos.get(0);
    }

    private static void auditar(
            Connection c, int actor, String ip, String evento, int motivo, int especialidad)
            throws SQLException {
        if (ip == null || ip.length() > 45) throw new SQLException("Origen inválido");
        new pe.edu.utp.clinica.repository.UsuarioRepository(c)
                .auditar(
                        actor,
                        ip,
                        evento,
                        "Motivo #" + motivo + "; especialidad #" + especialidad,
                        false);
    }

    private static AgendaException denegado() {
        return new AgendaException(
                403,
                "CATALOGO_NO_AUTORIZADO",
                "Tu cuenta no puede realizar esta operación del catálogo.");
    }

    private static AgendaException noEncontrado() {
        return new AgendaException(
                404, "MOTIVO_NO_ENCONTRADO", "No se encontró el motivo de consulta.");
    }
}
