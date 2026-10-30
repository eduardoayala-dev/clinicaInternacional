package pe.edu.utp.clinica.recuperacion;

import org.junit.jupiter.api.*;
import pe.edu.utp.clinica.auth.*;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.utp.clinica.recuperacion.VerificacionJdbc.Proposito.*;

abstract class RecuperacionContrato {
    protected String url;
    private VerificacionJdbc dao;
    private final CodigosVerificacion codigos=new CodigosVerificacion(Base64.getEncoder().encodeToString(new byte[32]));
    private final Instant ahora=Instant.parse("2026-10-30T15:00:00Z");
    private static final String ANTERIOR="Anterior!2026Segura";
    private static final String HASH=SecurityUtil.hashPassword(ANTERIOR);
    private static final String NUEVA="NuevaClave!2026Segura";
    private static final String VINCULO="navegador-local";
    private record Solicitud(String id,VerificacionJdbc.Entrega entrega) {}
    protected Connection abrir()throws SQLException{return DriverManager.getConnection(url);}

    @BeforeEach void preparar()throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        dao=new VerificacionJdbc(this::abrir,codigos);
        try(var c=abrir();var st=c.createStatement()){
            pe.edu.utp.clinica.dao.EsquemaPruebas.limpiar(c);
            st.execute("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),email VARCHAR(254) UNIQUE,password_hash VARCHAR(100),rol VARCHAR(32),activo BOOLEAN)");
            st.execute("CREATE TABLE paciente(id_usuario INT PRIMARY KEY,dni VARCHAR(15))");
            st.execute("CREATE TABLE auditoria_seguridad(id_auditoria INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT,evento VARCHAR(50),descripcion VARCHAR(255),sospechoso BOOLEAN)");
            try(var insertar=c.prepareStatement("INSERT INTO usuario VALUES(1,'Paciente','Prueba','paciente@example.test',?,'PACIENTE',TRUE),(2,'Personal','Prueba','personal@example.test',?,'MEDICO',TRUE)")){
                insertar.setString(1,HASH);insertar.setString(2,HASH);insertar.executeUpdate();
            }
            st.execute("INSERT INTO paciente VALUES(1,'12345678')");
            for(String archivo:List.of("V006__sesiones_autenticacion.sql","V007__verificacion_cuenta.sql")){
                String ddl=Files.readString(Path.of("sql/migrations",archivo));
                if(url.startsWith("jdbc:h2:"))ddl=ddl.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","").replace(" CHARACTER SET ascii COLLATE ascii_bin","");
                for(String parte:ddl.split(";"))if(!parte.isBlank())st.execute(parte);
            }
        }
    }
    private Solicitud recuperar()throws SQLException {
        String id=UUID.randomUUID().toString();
        return new Solicitud(id,dao.iniciarRecuperacion(id,"12345678",false,VINCULO,"ip",ahora).orElseThrow());
    }
    private String sesion(int usuario)throws SQLException {
        String id=UUID.randomUUID().toString();
        var u=new Usuario(usuario,"Prueba","Local",usuario==1?"paciente@example.test":"personal@example.test",HASH,usuario==1?"PACIENTE":"MEDICO",null,true);
        new SesionesJdbc(this::abrir).crear(id,u,SecretosSesion.hash(SecretosSesion.nuevo()),SecretosSesion.nuevo(),ahora,ahora.plusSeconds(28800));
        return id;
    }
    private Solicitud cambio(String sid,VerificacionJdbc.Proposito proposito)throws SQLException {
        String id=UUID.randomUUID().toString();
        return new Solicitud(id,dao.iniciarCambio(id,new VerificacionJdbc.InicioCambio(1,sid,VINCULO,proposito,ANTERIOR,"ip"),ahora).orElseThrow());
    }
    private boolean verificar(Solicitud s)throws SQLException{return dao.verificar(s.id(),VINCULO,RECUPERAR,null,s.entrega().codigo(),ahora);}
    private String incorrecto(String codigo){return "000000".equals(codigo)?"111111":"000000";}
    private void sql(String sql)throws SQLException{try(var c=abrir();var st=c.createStatement()){st.execute(sql);}}
    private String valor(String columna)throws SQLException{
        String query=switch(columna){case "email"->"SELECT email FROM usuario WHERE id_usuario=1";case "password_hash"->"SELECT password_hash FROM usuario WHERE id_usuario=1";default->throw new IllegalArgumentException();};
        try(var c=abrir();var st=c.createStatement();var rs=st.executeQuery(query)){rs.next();return rs.getString(1);}
    }
    @Test void codigoSeProtegeConHmacYNoEsContrasena()throws Exception {
        var s=recuperar();assertTrue(s.entrega().codigo().matches("[0-9]{6}"));
        try(var c=abrir();var st=c.createStatement();var rs=st.executeQuery("SELECT codigo_hash FROM verificacion_cuenta")){
            assertTrue(rs.next());assertArrayEquals(codigos.hash(s.id(),RECUPERAR.name(),"CODIGO_ACTUAL",s.entrega().codigo()),rs.getBytes(1));
            assertFalse(Arrays.equals(SecretosSesion.hash(s.entrega().codigo()),rs.getBytes(1)));
        }
        assertFalse(s.entrega().toString().contains(s.entrega().codigo()));
    }
    @Test void noPermiteCambiarAntesDeVerificar()throws Exception {
        var s=recuperar();assertFalse(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));assertEquals(HASH,valor("password_hash"));
    }
    @Test void codigoPermiteUnCambioYNoPuedeReutilizarse()throws Exception {
        var s=recuperar();assertTrue(verificar(s));assertFalse(verificar(s));
        assertTrue(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));
        assertTrue(SecurityUtil.checkPassword(NUEVA,valor("password_hash")));
        assertFalse(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,"OtraClave!2026Segura",ahora));
    }
    @Test void cincoIntentosErroneosBloqueanInclusoElCodigoCorrecto()throws Exception {
        var s=recuperar();for(int i=0;i<5;i++)assertFalse(dao.verificar(s.id(),VINCULO,RECUPERAR,null,incorrecto(s.entrega().codigo()),ahora));
        assertFalse(verificar(s));assertFalse(dao.autorizada(s.id(),VINCULO,RECUPERAR,null,ahora));
    }
    @Test void aciertoEnQuintoIntentoTodaviaAutoriza()throws Exception {
        var s=recuperar();for(int i=0;i<4;i++)assertFalse(dao.verificar(s.id(),VINCULO,RECUPERAR,null,incorrecto(s.entrega().codigo()),ahora));
        assertTrue(verificar(s));assertTrue(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));
    }
    @Test void venceCodigoALosDiezMinutosYPermisoALosCinco()throws Exception {
        var s=recuperar();assertFalse(dao.verificar(s.id(),VINCULO,RECUPERAR,null,s.entrega().codigo(),ahora.plusSeconds(600)));
        var nuevo=recuperar();assertTrue(verificar(nuevo));assertFalse(dao.cambiarPassword(nuevo.id(),VINCULO,RECUPERAR,null,NUEVA,ahora.plusSeconds(300)));
    }
    @Test void solicitudNuevaInvalidaCodigoAnterior()throws Exception {
        var uno=recuperar();var dos=recuperar();assertFalse(verificar(uno));assertTrue(verificar(dos));
    }
    @Test void otroNavegadorOtraSesionYOtroPropositoNoFuncionan()throws Exception {
        var s=recuperar();assertFalse(dao.verificar(s.id(),"otro-navegador",RECUPERAR,null,s.entrega().codigo(),ahora));
        assertFalse(dao.verificar(s.id(),VINCULO,CAMBIAR_PASSWORD,null,s.entrega().codigo(),ahora));assertTrue(verificar(s));
        assertFalse(dao.cambiarPassword(s.id(),"otro-navegador",RECUPERAR,null,NUEVA,ahora));
    }
    @Test void correoYPasswordModificadosODesactivacionInvalidanSolicitud()throws Exception {
        var s=recuperar();sql("UPDATE usuario SET email='nuevo@example.test' WHERE id_usuario=1");assertFalse(verificar(s));
        sql("UPDATE usuario SET email='paciente@example.test',password_hash='cambio' WHERE id_usuario=1");assertFalse(verificar(s));
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=1");assertFalse(verificar(s));
    }
    @Test void pacienteUsaDocumentoYPersonalCorreoSinCruzarPortales()throws Exception {
        String id=UUID.randomUUID().toString();
        assertTrue(dao.iniciarRecuperacion(id,"paciente@example.test",true,VINCULO,"ip",ahora).isEmpty());
        assertTrue(dao.iniciarRecuperacion(id,"personal@example.test",true,VINCULO,"ip",ahora).isPresent());
    }
    @Test void cambiarPasswordExigeActualYSesionDeLaCuenta()throws Exception {
        String sid=sesion(1);String otra=sesion(2);String id=UUID.randomUUID().toString();
        assertTrue(dao.iniciarCambio(id,new VerificacionJdbc.InicioCambio(1,sid,VINCULO,CAMBIAR_PASSWORD,"errónea","ip"),ahora).isEmpty());
        assertTrue(dao.iniciarCambio(id,new VerificacionJdbc.InicioCambio(1,otra,VINCULO,CAMBIAR_PASSWORD,ANTERIOR,"ip"),ahora).isEmpty());
        assertTrue(dao.iniciarCambio(id,new VerificacionJdbc.InicioCambio(1,sid,VINCULO,CAMBIAR_PASSWORD,ANTERIOR,"ip"),ahora).isPresent());
    }
    @Test void cambioAutenticadoConservaSesionActualYRevocaLasOtras()throws Exception {
        String actual=sesion(1);String otra=sesion(1);String ajena=sesion(2);var s=cambio(actual,CAMBIAR_PASSWORD);
        assertTrue(dao.verificar(s.id(),VINCULO,CAMBIAR_PASSWORD,actual,s.entrega().codigo(),ahora));
        assertTrue(dao.cambiarPassword(s.id(),VINCULO,CAMBIAR_PASSWORD,actual,NUEVA,ahora));
        var sesiones=new SesionesJdbc(this::abrir);assertNotNull(sesiones.resolver(actual,null,ahora,false));assertNull(sesiones.resolver(otra,null,ahora,false));assertNotNull(sesiones.resolver(ajena,null,ahora,false));
    }
    @Test void recuperarRevocaTodasLasSesionesPropias()throws Exception {
        String una=sesion(1);String otra=sesion(1);String ajena=sesion(2);var s=recuperar();assertTrue(verificar(s));
        assertTrue(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));
        var sesiones=new SesionesJdbc(this::abrir);assertNull(sesiones.resolver(una,null,ahora,false));assertNull(sesiones.resolver(otra,null,ahora,false));assertNotNull(sesiones.resolver(ajena,null,ahora,false));
    }
    @Test void emailNoCambiaSinVerificarAmbasDirecciones()throws Exception {
        String sid=sesion(1);var s=cambio(sid,CAMBIAR_CORREO);
        assertTrue(dao.prepararCorreo(s.id(),VINCULO,sid,"nuevo@example.test",ahora).isEmpty());
        assertTrue(dao.verificar(s.id(),VINCULO,CAMBIAR_CORREO,sid,s.entrega().codigo(),ahora));
        var nuevo=dao.prepararCorreo(s.id(),VINCULO,sid,"nuevo@example.test",ahora).orElseThrow();assertEquals("paciente@example.test",valor("email"));
        assertFalse(dao.confirmarCorreo(s.id(),VINCULO,sid,incorrecto(nuevo.codigo()),ahora).cambiado());
        assertTrue(dao.confirmarCorreo(s.id(),VINCULO,sid,nuevo.codigo(),ahora).cambiado());assertEquals("nuevo@example.test",valor("email"));
        assertFalse(dao.confirmarCorreo(s.id(),VINCULO,sid,nuevo.codigo(),ahora).cambiado());
    }
    @Test void otroUsuarioNoPuedeUsarLaVerificacionDelCorreo()throws Exception {
        String sid=sesion(1);String ajena=sesion(2);var s=cambio(sid,CAMBIAR_CORREO);
        assertFalse(dao.verificar(s.id(),VINCULO,CAMBIAR_CORREO,ajena,s.entrega().codigo(),ahora));
        assertTrue(dao.verificar(s.id(),VINCULO,CAMBIAR_CORREO,sid,s.entrega().codigo(),ahora));
        assertTrue(dao.prepararCorreo(s.id(),"otro-navegador",sid,"nuevo@example.test",ahora).isEmpty());
    }
    @Test void correoYaOcupadoNoPuedeAsignarse()throws Exception {
        String sid=sesion(1);var s=cambio(sid,CAMBIAR_CORREO);assertTrue(dao.verificar(s.id(),VINCULO,CAMBIAR_CORREO,sid,s.entrega().codigo(),ahora));
        String largo="a".repeat(140)+"@example.test";
        String retoId=s.id();
        assertThrows(IllegalArgumentException.class,()->dao.prepararCorreo(retoId,VINCULO,sid,largo,ahora));
        assertTrue(dao.prepararCorreo(s.id(),VINCULO,sid,"personal@example.test",ahora).isEmpty());
        var nuevo=dao.prepararCorreo(s.id(),VINCULO,sid,"reservado@example.test",ahora).orElseThrow();
        sql("UPDATE usuario SET email='reservado@example.test' WHERE id_usuario=2");assertFalse(dao.confirmarCorreo(s.id(),VINCULO,sid,nuevo.codigo(),ahora).cambiado());
        assertEquals("paciente@example.test",valor("email"));
    }
    @Test void revocarSesionInvalidaCodigoDeCambioAutenticado()throws Exception {
        String sid=sesion(1);var s=cambio(sid,CAMBIAR_PASSWORD);new SesionesJdbc(this::abrir).revocar(sid,ahora);
        assertFalse(dao.verificar(s.id(),VINCULO,CAMBIAR_PASSWORD,sid,s.entrega().codigo(),ahora));
    }
    @Test void cambioDeRolInvalidaSesionAunqueNadieLaHayaConsultado()throws Exception {
        String sid=sesion(1);var s=cambio(sid,CAMBIAR_PASSWORD);sql("UPDATE usuario SET rol='ADMINISTRADOR' WHERE id_usuario=1");
        assertFalse(dao.verificar(s.id(),VINCULO,CAMBIAR_PASSWORD,sid,s.entrega().codigo(),ahora));
    }
    @Test void segundoCodigoRespetaVencimientoAbsolutoDelFlujo()throws Exception {
        String sid=sesion(1);var s=cambio(sid,CAMBIAR_CORREO);
        assertTrue(dao.verificar(s.id(),VINCULO,CAMBIAR_CORREO,sid,s.entrega().codigo(),ahora.plusSeconds(590)));
        var nuevo=dao.prepararCorreo(s.id(),VINCULO,sid,"nuevo@example.test",ahora.plusSeconds(880)).orElseThrow();
        assertFalse(dao.confirmarCorreo(s.id(),VINCULO,sid,nuevo.codigo(),ahora.plusSeconds(1200)).cambiado());
        assertEquals("paciente@example.test",valor("email"));
    }
    @Test void auditoriaEsParteDeLaTransaccionYNoGuardaCodigos()throws Exception {
        var s=recuperar();assertTrue(verificar(s));assertTrue(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));
        try(var c=abrir();var st=c.createStatement();var rs=st.executeQuery("SELECT evento,descripcion FROM auditoria_seguridad")){
            assertTrue(rs.next());assertEquals("RECUPERAR",rs.getString(1));assertFalse(rs.getString(2).contains(s.entrega().codigo()));
        }
    }
    @Test void falloDeRevocacionReviertePasswordYConservaAutorizacion()throws Exception {
        var s=recuperar();assertTrue(verificar(s));sql("DROP TABLE sesion_autenticacion");
        assertThrows(SQLException.class,()->dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));assertEquals(HASH,valor("password_hash"));
        assertTrue(dao.autorizada(s.id(),VINCULO,RECUPERAR,null,ahora));
    }
    @Test void dosCanjesConcurrentesSoloHacenUnCambio()throws Exception {
        var s=recuperar();assertTrue(verificar(s));var ejecutor=Executors.newFixedThreadPool(2);
        try{var uno=ejecutor.submit(()->dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,NUEVA,ahora));var dos=ejecutor.submit(()->dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,"OtraClave!2026Segura",ahora));assertNotEquals(uno.get(15,TimeUnit.SECONDS),dos.get(15,TimeUnit.SECONDS));}finally{ejecutor.shutdownNow();}
    }
    @Test void limitesPersistenEntreInstanciasYNoRevelanCuentasAusentes()throws Exception {
        recuperar();recuperar();recuperar();var otra=new VerificacionJdbc(this::abrir,codigos);
        assertTrue(otra.iniciarRecuperacion(UUID.randomUUID().toString(),"12345678",false,VINCULO,"otra-ip",ahora).isEmpty());
        assertTrue(otra.iniciarRecuperacion(UUID.randomUUID().toString(),"12345678",false,VINCULO,"otra-ip",ahora.plusSeconds(3600)).isPresent());
    }
    @Test void limiteIpCuentaSolicitudesInexistentes()throws Exception {
        for(int i=0;i<10;i++)dao.iniciarRecuperacion(UUID.randomUUID().toString(),"9000000"+i,false,VINCULO,"misma-ip",ahora);
        assertTrue(dao.iniciarRecuperacion(UUID.randomUUID().toString(),"12345678",false,VINCULO,"misma-ip",ahora).isEmpty());
    }
    @Test void dniYLimiteUtf8SeCompruebanAlGuardar()throws Exception {
        var s=recuperar();assertTrue(verificar(s));assertFalse(dao.cambiarPassword(s.id(),VINCULO,RECUPERAR,null,"NuevaClave!12345678",ahora));
        assertFalse(VerificacionJdbc.passwordValida("A!1á".repeat(20)));assertFalse(VerificacionJdbc.passwordValida("Corta1!"));assertTrue(VerificacionJdbc.passwordValida(NUEVA));
    }
    @Test void servicioEnviaCodigoSinEnlacesYNoEnviaParaDocumentoAusente() {
        var enviados=new ArrayList<pe.edu.utp.clinica.correo.Correo>();var servicio=new RecuperacionServicio(dao,c->{enviados.add(c);return "id";},Clock.fixed(ahora,ZoneOffset.UTC),Runnable::run);
        String id=servicio.solicitar("99999999",false,VINCULO,"ip");assertNotNull(UUID.fromString(id));assertTrue(enviados.isEmpty());
        servicio.solicitar("12345678",false,VINCULO,"ip");assertEquals(1,enviados.size());assertFalse(enviados.get(0).texto().contains("#token="));assertFalse(enviados.get(0).texto().contains(ANTERIOR));
    }
}
