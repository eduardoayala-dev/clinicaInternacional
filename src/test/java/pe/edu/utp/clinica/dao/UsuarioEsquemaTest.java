package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Paciente;
import pe.edu.utp.clinica.model.Usuario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioEsquemaTest {
    private String url;
    private UsuarioDAO dao;

    @BeforeEach void setUp() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID();
        try (Connection conn = DriverManager.getConnection(url + ";DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", ""); var st = conn.createStatement()) {
            st.execute("CREATE TABLE usuario(id_usuario INT AUTO_INCREMENT PRIMARY KEY,nombres VARCHAR,apellidos VARCHAR,email VARCHAR UNIQUE,password_hash VARCHAR,rol VARCHAR,activo BOOLEAN)");
            st.execute("CREATE TABLE paciente(id_paciente INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT REFERENCES usuario(id_usuario),dni VARCHAR,fecha_nacimiento DATE,telefono VARCHAR,direccion VARCHAR,canal_aviso VARCHAR,sede_preferida VARCHAR,alergias VARCHAR,tipo_cobertura VARCHAR)");
            st.execute("CREATE USER clinica_app PASSWORD 'test-only'");
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON usuario,paciente TO clinica_app");
        }
        dao = new UsuarioDAO() {
            @Override protected Connection abrirConexion() throws SQLException {
                return DriverManager.getConnection(url, "clinica_app", "test-only");
            }
        };
    }

    private Usuario user() {
        Usuario user = new Usuario(); user.setNombres("Prueba"); user.setApellidos("Paciente");
        user.setEmail("prueba@test.local"); user.setPasswordHash("hash-de-prueba"); user.setRol("PACIENTE"); user.setActivo(true);
        return user;
    }

    private Paciente patient() {
        Paciente p = new Paciente(); p.setDni("12345678"); p.setFechaNacimiento(LocalDate.of(2000,1,1));
        p.setTelefono("999999999"); p.setDireccion("Dirección sintética"); p.setAlergias("Ninguna"); return p;
    }

    private int count(String table) throws Exception {
        try (Connection conn=DriverManager.getConnection(url,"sa",""); var st=conn.createStatement(); var rs=st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next(); return rs.getInt(1);
        }
    }

    @Test void registrationWorksWithCrudPermissionsAndNoAlterPrivilege() throws Exception {
        try (Connection conn=DriverManager.getConnection(url,"clinica_app","test-only"); var st=conn.createStatement()) {
            assertThrows(SQLException.class, () -> st.execute("ALTER TABLE paciente ADD COLUMN prohibida INT"));
        }
        assertTrue(dao.crearCuentaPaciente(user(),patient()));
        assertEquals(1,count("usuario")); assertEquals(1,count("paciente"));
    }

    @Test void missingColumnRollsBackInsteadOfDiscardingProfileFields() throws Exception {
        try (Connection conn=DriverManager.getConnection(url,"sa",""); var st=conn.createStatement()) {
            st.execute("ALTER TABLE paciente DROP COLUMN alergias");
        }
        assertFalse(dao.crearCuentaPaciente(user(),patient()));
        assertEquals(0,count("usuario")); assertEquals(0,count("paciente"));
    }

    @Test void profileSchemaFailureRollsBackBasicChanges() throws Exception {
        Usuario u=user(); assertTrue(dao.crearCuentaPaciente(u,patient()));
        try (Connection conn=DriverManager.getConnection(url,"sa",""); var st=conn.createStatement()) {
            st.execute("ALTER TABLE paciente DROP COLUMN alergias");
        }
        assertFalse(dao.actualizarPerfilCompletoPaciente(new pe.edu.utp.clinica.model.PerfilPaciente(u.getIdUsuario(),"Cambio","Paciente",
                "2000-01-01","888888888","Nueva dirección","Correo","Central","Ninguna","EPS")));
        try (Connection conn=DriverManager.getConnection(url,"sa",""); var st=conn.createStatement(); var rs=st.executeQuery("SELECT nombres,email FROM usuario")) {
            assertTrue(rs.next()); assertEquals("Prueba",rs.getString(1)); assertEquals("prueba@test.local",rs.getString(2));
        }
    }
    @Test void profileUpdatePreservesIdentityAndVerifiedEmail() throws Exception {
        Usuario u=user();assertTrue(dao.crearCuentaPaciente(u,patient()));
        var perfil=new pe.edu.utp.clinica.model.PerfilPaciente(u.getIdUsuario(),"Cambio","Alterado",
                "02/03/2000","888888888","Nueva dirección","Correo","Central","Ninguna","EPS");
        assertTrue(dao.actualizarPerfilCompletoPaciente(perfil));
        try(var conn=DriverManager.getConnection(url,"sa","");var st=conn.createStatement();
            var rs=st.executeQuery("SELECT u.nombres,u.email,p.fecha_nacimiento,p.direccion,p.canal_aviso,u.apellidos,p.telefono FROM usuario u JOIN paciente p ON p.id_usuario=u.id_usuario")) {
            assertTrue(rs.next());assertEquals("Prueba",rs.getString(1));assertEquals("prueba@test.local",rs.getString(2));
            assertEquals(java.sql.Date.valueOf("2000-01-01"),rs.getDate(3));assertEquals("Nueva dirección",rs.getString(4));assertEquals("Correo",rs.getString(5));assertEquals("Paciente",rs.getString(6));assertEquals("888888888",rs.getString(7));
        }
    }
}
