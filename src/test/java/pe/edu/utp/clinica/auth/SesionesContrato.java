package pe.edu.utp.clinica.auth;

import org.junit.jupiter.api.*;
import pe.edu.utp.clinica.model.Usuario;
import java.sql.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

abstract class SesionesContrato {
    protected String url;
    private SesionesJdbc dao;
    private final Instant now=Instant.parse("2026-10-29T12:00:00Z");
    private final Usuario usuario=new Usuario(1,"Paciente","Prueba","p@example.test","hash-uno","PACIENTE",null,true);
    private final String sid=UUID.randomUUID().toString(), csrf=SecretosSesion.nuevo();
    private final byte[] refresh=SecretosSesion.hash(SecretosSesion.nuevo());
    @BeforeEach void setup() throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dao=new SesionesJdbc(this::abrir);
        try(var c=abrir();var st=c.createStatement()) {
            pe.edu.utp.clinica.dao.EsquemaPruebas.limpiar(c);
            st.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),email VARCHAR(150),password_hash VARCHAR(100),rol VARCHAR(32),activo BOOLEAN)");
            st.execute("INSERT INTO usuario VALUES(1,'Paciente','Prueba','p@example.test','hash-uno','PACIENTE',TRUE)");
            String ddl=Files.readString(Path.of("sql/migrations/V006__sesiones_autenticacion.sql"));
            st.execute(ddl.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "").replace(" CHARACTER SET ascii COLLATE ascii_bin", ""));
        }
        dao.crear(sid,usuario,refresh,csrf,now,now.plusSeconds(28800));
    }
    protected Connection abrir() throws SQLException {return DriverManager.getConnection(url);}
    private void sql(String sql) throws SQLException {try(var c=abrir();var st=c.createStatement()){st.execute(sql);}}
    @Test void resolvesSameSessionFromAccessOrRefreshAndKeepsCsrf() throws Exception {
        assertEquals(sid,dao.resolver(null,refresh,now,false).id());
        assertEquals(csrf,dao.resolver(sid,null,now.plusSeconds(300),true).csrf());
        assertNotNull(dao.resolver(sid,null,now.plusSeconds(1900),false));
        assertNull(dao.resolver(null,SecretosSesion.hash("wrong"),now,false));
        assertNull(dao.resolver("desconocida",refresh,now,false));
        assertNull(dao.resolver("' OR 1=1 --",null,now,false));
    }
    @Test void logoutIsImmediateAndRepeatedRevocationIsSafe() throws Exception {
        dao.revocar(sid,now); dao.revocar(sid,now);
        assertNull(dao.resolver(sid,null,now,true));
        assertNull(dao.resolver(null,refresh,now,true));
    }
    @Test void pollsDoNotExtendIdleSession() throws Exception {
        assertNotNull(dao.resolver(sid,null,now.plusSeconds(1700),false));
        assertNull(dao.resolver(sid,null,now.plusSeconds(1800),true));
    }
    @Test void absoluteExpiryCannotBeExtendedByActivity() throws Exception {
        for(int i=1;i<=16;i++) assertNotNull(dao.resolver(sid,null,now.plusSeconds(1700L*i),true));
        assertNull(dao.resolver(null,refresh,now.plusSeconds(28800),true));
    }
    @Test void disablingAccountRevokesItsExistingAccess() throws Exception {
        sql("UPDATE usuario SET activo=FALSE");
        assertNull(dao.resolver(sid,null,now,false));
        sql("UPDATE usuario SET activo=TRUE");
        assertNull(dao.resolver(sid,null,now,false));
    }
    @Test void changingRoleRevokesExistingAccess() throws Exception {
        sql("UPDATE usuario SET rol='MEDICO'");
        assertNull(dao.resolver(sid,null,now,false));
    }
    @Test void changingPasswordRevokesExistingAccess() throws Exception {
        sql("UPDATE usuario SET password_hash='hash-dos'");
        assertNull(dao.resolver(sid,null,now,false));
    }
    @Test void concurrentRenewalsKeepTheSameSessionAndCsrf() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->dao.resolver(null,refresh,now.plusSeconds(300),false));
            var second=executor.submit(()->dao.resolver(null,refresh,now.plusSeconds(300),false));
            assertEquals(sid,first.get(10,java.util.concurrent.TimeUnit.SECONDS).id());
            assertEquals(csrf,second.get(10,java.util.concurrent.TimeUnit.SECONDS).csrf());
        } finally { executor.shutdownNow(); }
    }
    @Test void renewalCannotReactivateAConcurrentlyRevokedSession() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var renewal=executor.submit(()->dao.resolver(null,refresh,now,false));
            var logout=executor.submit(()->{dao.revocar(sid,now);return true;});
            renewal.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(logout.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertNull(dao.resolver(null,refresh,now,false));
        } finally { executor.shutdownNow(); }
    }
    @Test void creationDoesNotAcceptAStaleIdentity() throws Exception {
        sql("UPDATE usuario SET activo=FALSE");
        assertThrows(SQLException.class,()->dao.crear(UUID.randomUUID().toString(),usuario,refresh,csrf,now,now.plusSeconds(10)));
    }
}
