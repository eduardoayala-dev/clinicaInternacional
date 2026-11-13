package pe.edu.utp.clinica.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.*;

class CatalogoSedesDAOTest {
    @Test
    void generaInicialesResuelveColisionesYConservaCodigoAlRenombrar() throws SQLException {
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,sede_preferida VARCHAR(100))");
        dao.guardar(
                1,
                new CatalogoSedesDAO.Sede(
                        0, null, "San Borja", "Avenida de prueba 123", "016102222", "ACTIVO"),
                null);
        dao.guardar(
                1,
                new CatalogoSedesDAO.Sede(
                        0, null, "San Bartolo", "Avenida de prueba 123", "016102222", "ACTIVO"),
                null);
        var creada =
                dao.listar(1).stream()
                        .filter(s -> s.nombre().equals("San Borja"))
                        .findFirst()
                        .orElseThrow();
        assertEquals("SED-SB", creada.codigo());
        assertTrue(dao.listar(1).stream().anyMatch(s -> s.codigo().equals("SED-SB-02")));
        dao.guardar(
                1,
                new CatalogoSedesDAO.Sede(
                        creada.id(),
                        creada.codigo(),
                        "Surco nuevo",
                        "Avenida de prueba 123",
                        "016102222",
                        "ACTIVO"),
                null);
        assertTrue(
                dao.listar(1).stream()
                        .anyMatch(s -> s.id() == creada.id() && s.codigo().equals("SED-SB")));
        assertEquals(
                422,
                assertThrows(
                                AgendaException.class,
                                () ->
                                        dao.guardar(
                                                1,
                                                new CatalogoSedesDAO.Sede(
                                                        creada.id(),
                                                        "SED-OTRO",
                                                        "Surco nuevo",
                                                        "Avenida de prueba 123",
                                                        "016102222",
                                                        "ACTIVO"),
                                                null))
                        .status());
    }

    @Test
    void dosAdministradoresSimultaneosNoGeneranElMismoCodigo() throws Exception {
        sql("INSERT INTO usuario VALUES(3,'ADMINISTRADOR',TRUE)");
        var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var inicio = new java.util.concurrent.CountDownLatch(1);
        try {
            var uno =
                    ejecutor.submit(
                            () -> {
                                inicio.await();
                                dao.guardar(
                                        1,
                                        new CatalogoSedesDAO.Sede(
                                                0,
                                                null,
                                                "San Borja",
                                                "Avenida de prueba 123",
                                                "016102222",
                                                "ACTIVO"),
                                        null);
                                return true;
                            });
            var dos =
                    ejecutor.submit(
                            () -> {
                                inicio.await();
                                dao.guardar(
                                        3,
                                        new CatalogoSedesDAO.Sede(
                                                0,
                                                null,
                                                "San Bartolo",
                                                "Avenida de prueba 123",
                                                "016102222",
                                                "ACTIVO"),
                                        null);
                                return true;
                            });
            inicio.countDown();
            assertTrue(uno.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(dos.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(
                    Set.of("SED-SB", "SED-SB-02"),
                    dao.listar(1).stream()
                            .map(CatalogoSedesDAO.Sede::codigo)
                            .collect(java.util.stream.Collectors.toSet()));
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    void renombrarConservaLaPreferenciaDelPaciente() throws SQLException {
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,sede_preferida VARCHAR(100))");
        dao.guardar(1, sede("SB-01", "ACTIVO"), null);
        int id = dao.listar(1).get(0).id();
        sql("INSERT INTO paciente VALUES(1,'Sede prueba')");
        dao.guardar(
                1,
                new CatalogoSedesDAO.Sede(
                        id,
                        "SB-01",
                        "Sede renovada",
                        "Avenida de prueba 123",
                        "016102222",
                        "ACTIVO"),
                null);
        try (var c = abrir();
                var s = c.createStatement();
                var r = s.executeQuery("SELECT sede_preferida FROM paciente WHERE id_paciente=1")) {
            assertTrue(r.next());
            assertEquals("Sede renovada", r.getString(1));
        }
    }

    private String url;
    private CatalogoSedesDAO dao;

    private Connection abrir() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void sql(String s) throws SQLException {
        try (var c = abrir();
                var st = c.createStatement()) {
            st.execute(s);
        }
    }

    private CatalogoSedesDAO.Sede sede(String codigo, String estado) {
        return new CatalogoSedesDAO.Sede(
                0, codigo, "Sede prueba", "Avenida de prueba 123", "016102222", estado);
    }

    @BeforeEach
    void preparar() throws SQLException {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,rol VARCHAR(40),activo BOOLEAN)");
        sql("INSERT INTO usuario VALUES(1,'ADMINISTRADOR',TRUE),(2,'PACIENTE',TRUE)");
        sql(
                "CREATE TABLE sede(id_sede INT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(20)"
                    + " UNIQUE,nombre VARCHAR(100),direccion VARCHAR(255),telefono"
                    + " VARCHAR(20),estado VARCHAR(20))");
        sql("CREATE TABLE disponibilidad(id_sede INT,estado VARCHAR(20),fecha DATE)");
        sql("CREATE TABLE medico(id_medico INT,id_sede INT)");
        sql("CREATE TABLE cita(id_medico INT,fecha_hora DATETIME,estado VARCHAR(20))");
        sql(
                "CREATE TABLE auditoria_seguridad(id_auditoria INT AUTO_INCREMENT PRIMARY"
                    + " KEY,id_usuario INT,ip_origen VARCHAR(45),evento VARCHAR(100),descripcion"
                    + " VARCHAR(255),sospechoso BOOLEAN)");
        dao = new CatalogoSedesDAO(this::abrir);
    }

    @Test
    void creaActualizaPorCodigoYConservaId() {
        dao.guardar(1, sede("SB-01", "ACTIVO"), "127.0.0.1");
        int id = dao.listar(1).get(0).id();
        var filas =
                dao.importar(
                        1,
                        List.of(
                                new CatalogoSedesDAO.Fila(2, sede("SB-01", "MANTENIMIENTO"), ""),
                                new CatalogoSedesDAO.Fila(3, sede("NUEVA-02", "ACTIVO"), "")),
                        "127.0.0.1");
        assertEquals(2, filas.stream().filter(f -> "Guardada".equals(f.resultado())).count());
        assertEquals(2, dao.listar(1).size());
        assertTrue(
                dao.listar(1).stream()
                        .anyMatch(s -> s.id() == id && s.estado().equals("MANTENIMIENTO")));
    }

    @Test
    void filaIncorrectaNoBloqueaLasCorrectas() {
        var filas =
                dao.importar(
                        1,
                        List.of(
                                new CatalogoSedesDAO.Fila(2, null, "Código inválido"),
                                new CatalogoSedesDAO.Fila(3, sede("SB-01", "ACTIVO"), "")),
                        "127.0.0.1");
        assertEquals(1, dao.listar(1).size());
        assertEquals("Código inválido", filas.get(0).resultado());
        assertEquals("Guardada", filas.get(1).resultado());
    }

    @Test
    void permisosYDuplicadosSeValidan() {
        var sede = sede("SB-01", "ACTIVO");
        assertEquals(
                403,
                assertThrows(AgendaException.class, () -> dao.guardar(2, sede, "127.0.0.1"))
                        .status());
        dao.guardar(1, sede, "127.0.0.1");
        assertThrows(AgendaException.class, () -> dao.guardar(1, sede, "127.0.0.1"));
    }

    @Test
    void noInactivaUnaSedeConDisponibilidadPublicada() throws SQLException {
        dao.guardar(1, sede("SB-01", "ACTIVO"), "127.0.0.1");
        int id = dao.listar(1).get(0).id();
        sql("INSERT INTO disponibilidad VALUES(" + id + ",'PUBLICADO',DATE '2099-01-01')");
        var editada =
                new CatalogoSedesDAO.Sede(
                        id,
                        "SB-01",
                        "Sede prueba",
                        "Avenida de prueba 123",
                        "016102222",
                        "INACTIVO");
        assertEquals(
                409,
                assertThrows(AgendaException.class, () -> dao.guardar(1, editada, "127.0.0.1"))
                        .status());
        assertEquals("ACTIVO", dao.listar(1).get(0).estado());
    }

    @Test
    void falloDeAuditoriaRevierteAlta() throws SQLException {
        sql("DROP TABLE auditoria_seguridad");
        var sede = sede("SB-01", "ACTIVO");
        assertEquals(
                503,
                assertThrows(AgendaException.class, () -> dao.guardar(1, sede, "127.0.0.1"))
                        .status());
        assertTrue(dao.listar(1).isEmpty());
    }
}
