package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.*;
import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.util.SecurityUtil;
import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

abstract class InvitacionPersonalContrato {
    @Test void accesoMedicoSeActivaAntesDeCompletarSuPerfilEnAdmision()throws Exception {
        sql("ALTER TABLE auditoria_seguridad ADD COLUMN ip_origen VARCHAR(45) NULL");
        byte[] invitacion=token();int id=dao().crearAcceso(90,solicitud(),invitacion).usuario();
        assertEquals(0,cuenta("SELECT COUNT(*) FROM medico WHERE id_usuario="+id));
        assertEquals(409,assertThrows(AgendaException.class,()->dao().editarAcceso(90,id,solicitud(),true)).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM usuario WHERE id_usuario="+id+" AND activo=FALSE"));
        dao().activar(invitacion,"ClaveNuevaSegura#2026");
        var perfiles=new PerfilProfesionalMedicoDAO(this::abrir);
        assertTrue(perfiles.pendientes(91).stream().anyMatch(c->c.id()==id&&c.activa()));
        assertEquals(403,assertThrows(AgendaException.class,()->perfiles.completar(90,id,"123456",1,null)).status());
        assertTrue(perfiles.pendientes(92).stream().noneMatch(c->c.id()==id));
        assertEquals(403,assertThrows(AgendaException.class,()->perfiles.completar(92,id,"123456",1,null)).status());
        perfiles.completar(91,id,"123456",1,null);
        assertEquals(1,cuenta("SELECT COUNT(*) FROM medico WHERE id_usuario="+id+" AND id_sede=1 AND cmp='123456'"));
        assertFalse(perfiles.pendientes(91).stream().anyMatch(c->c.id()==id));
        assertEquals(403,assertThrows(AgendaException.class,()->perfiles.completar(92,id,"654321",1,null)).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='PERFIL_MEDICO_COMPLETADO'"));
    }
    @Test void cancelacionDeAdmisionRequiereMotivoYLoAudita()throws Exception {
        var admision=new AdmisionCitaDAO(this::abrir,reloj,10);
        assertEquals(400,assertThrows(AgendaException.class,()->admision.cancelar(91,1,"")).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM cita WHERE id_cita=1 AND estado='CONFIRMADA'"));
        admision.cancelar(91,1,"Cambio solicitado por paciente");
        admision.cancelar(91,1,"Intento duplicado");
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE id_usuario=91 AND evento='CITA_CANCELADA_ADMISION' AND descripcion LIKE '%Cambio solicitado por paciente%'"));
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='CITA_CANCELADA_ADMISION'"));
    }
    protected String url;
    protected abstract void configurar();
    private final Clock reloj=Clock.fixed(Instant.parse("2026-10-31T17:00:00Z"),ZoneOffset.UTC);
    protected Connection abrir()throws SQLException {return url.startsWith("jdbc:mysql:")?MySqlPruebas.abrir():DriverManager.getConnection(url,"sa","");}
    private InvitacionPersonalDAO dao(){return new InvitacionPersonalDAO(this::abrir,reloj);}
    private InvitacionPersonalDAO despues(long segundos){return new InvitacionPersonalDAO(this::abrir,Clock.offset(reloj,Duration.ofSeconds(segundos)));}
    private byte[] token(){return SecretosSesion.hash(SecretosSesion.nuevo());}
    private InvitacionPersonalDAO.Solicitud solicitud(){return new InvitacionPersonalDAO.Solicitud("Médica","Peña Ñúñez","nueva@example.test","MEDICO","123456",1,1,null);}
    private int crear(byte[] token){return dao().crear(90,solicitud(),token).usuario();}
    private void sql(String sql)throws SQLException {try(var c=abrir();var st=c.createStatement()){st.execute(sql);}}
    private int cuenta(String sql)throws SQLException {try(var c=abrir();var st=c.createStatement();var rs=st.executeQuery(sql)){rs.next();return rs.getInt(1);}}

    @BeforeEach void preparar()throws Exception {
        configurar();try(var c=abrir()){EsquemaPruebas.limpiar(c);}
        sql("CREATE TABLE usuario(id_usuario INT AUTO_INCREMENT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),email VARCHAR(150) UNIQUE,password_hash VARCHAR(255),rol VARCHAR(30),activo BOOLEAN)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20))");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,estado VARCHAR(20))");
        sql("CREATE TABLE medico(id_medico INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT UNIQUE,cmp VARCHAR(20) UNIQUE,id_sede INT,id_especialidad INT,estado VARCHAR(20))");
        sql("CREATE TABLE administrador(id_usuario INT PRIMARY KEY,estado VARCHAR(20))");
        sql("CREATE TABLE personal_administrativo(id_personal INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT UNIQUE,id_sede INT,codigo_empleado VARCHAR(30),cargo VARCHAR(100))");
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT,sede_preferida VARCHAR(100),telefono VARCHAR(20),direccion VARCHAR(255),tipo_cobertura VARCHAR(50),canal_aviso VARCHAR(30))");
        sql("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_medico INT,id_sede INT,fecha DATE,estado VARCHAR(20))");
        sql("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_medico INT,id_paciente INT,id_disponibilidad INT,fecha_hora DATETIME,estado VARCHAR(20))");
        sql("CREATE TABLE admision_cita(id_cita INT PRIMARY KEY)");
        sql("CREATE TABLE notificacion(id_notificacion INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT,id_cita INT,tipo VARCHAR(40),canal VARCHAR(20),mensaje VARCHAR(300),estado_envio VARCHAR(20),leida BOOLEAN,fecha_envio DATETIME)");
        sql("CREATE TABLE auditoria_seguridad(id_auditoria INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT,evento VARCHAR(70),descripcion VARCHAR(300),sospechoso BOOLEAN)");
        sql("INSERT INTO sede VALUES(1,'San Borja','ACTIVO'),(2,'Otra sede','ACTIVO'),(3,'Inactiva','INACTIVO')");
        sql("INSERT INTO especialidad VALUES(1,'ACTIVO'),(2,'INACTIVO')");
        sql("INSERT INTO usuario VALUES(1,'Paciente','Uno','p1@test.test','no','PACIENTE',TRUE),(2,'Paciente','Dos','p2@test.test','no','PACIENTE',TRUE),(30,'Médico','Uno','m1@test.test','no','MEDICO',TRUE),(31,'Médico','Dos','m2@test.test','no','MEDICO',TRUE),(90,'Admin','Prueba','a@test.test','no','ADMINISTRADOR',TRUE),(91,'Admisión','Uno','pa1@test.test','no','PERSONAL_ADMINISTRATIVO',TRUE),(92,'Admisión','Dos','pa2@test.test','no','PERSONAL_ADMINISTRATIVO',TRUE)");
        sql("INSERT INTO medico(id_usuario,cmp,id_sede,id_especialidad,estado) VALUES(30,'000001',1,1,'ACTIVO'),(31,'000002',2,1,'ACTIVO')");
        sql("INSERT INTO administrador VALUES(90,'ACTIVO')");
        sql("INSERT INTO personal_administrativo(id_usuario,id_sede) VALUES(91,1),(92,2)");
        sql("INSERT INTO paciente(id_paciente,id_usuario,sede_preferida) VALUES(1,1,'San Borja'),(2,2,'Otra sede')");
        sql("INSERT INTO disponibilidad VALUES(1,1,1,'2026-11-03','PUBLICADO'),(2,2,2,'2026-11-03','PUBLICADO')");
        sql("INSERT INTO cita VALUES(1,1,1,1,'2026-11-03 09:00:00','CONFIRMADA'),(2,2,2,2,'2026-11-03 09:00:00','CONFIRMADA')");
        String ddl=Files.readString(Path.of("sql/migrations/V012__invitacion_personal.sql"));
        if(url.startsWith("jdbc:h2:"))ddl=ddl.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","");sql(ddl);
        sql("ALTER TABLE invitacion_personal ADD COLUMN id_sede_destino INT NULL");
    }
    @Test void creaCuentaInactivaConPerfilYAuditoriaSinPasswordCompartido()throws Exception {
        byte[] token=token();int id=crear(token);
        assertEquals(1,cuenta("SELECT COUNT(*) FROM usuario WHERE id_usuario="+id+" AND activo=FALSE"));
        assertEquals(1,cuenta("SELECT COUNT(*) FROM medico WHERE id_usuario="+id+" AND cmp='123456'"));
        assertTrue(dao().vigente(token));assertEquals("Pendiente de activación",dao().estados(90).get(id));
        try(var c=abrir();var st=c.createStatement();var rs=st.executeQuery("SELECT password_hash FROM usuario WHERE id_usuario="+id)){rs.next();assertFalse(SecurityUtil.checkPassword("Password123!",rs.getString(1)));}
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='PERSONAL_INVITADO'"));
    }
    @Test void soloUnAdministradorActivoPuedeInvitar()throws Exception {
        var solicitud=solicitud();byte[] token=token();var dao=dao();
        assertEquals(403,assertThrows(AgendaException.class,()->dao.crear(91,solicitud,token)).status());
        sql("UPDATE usuario SET activo=FALSE WHERE id_usuario=90");
        assertEquals(403,assertThrows(AgendaException.class,()->dao.crear(90,solicitud,token)).status());
        assertEquals(0,cuenta("SELECT COUNT(*) FROM invitacion_personal"));
    }
    @Test void noCreaPacientesNiPerfilesConCatalogosInvalidos()throws Exception {
        var dao=dao();byte[] token=token();
        var paciente=new InvitacionPersonalDAO.Solicitud("A","B","a@example.test","PACIENTE",null,null,null,null);
        var sede=new InvitacionPersonalDAO.Solicitud("A","B","a@example.test","MEDICO","123456",1,3,null);
        var esp=new InvitacionPersonalDAO.Solicitud("A","B","a@example.test","MEDICO","123456",2,1,null);
        assertEquals(400,assertThrows(AgendaException.class,()->dao.crear(90,paciente,token)).status());
        assertEquals(400,assertThrows(AgendaException.class,()->dao.crear(90,sede,token)).status());
        assertEquals(400,assertThrows(AgendaException.class,()->dao.crear(90,esp,token)).status());
        assertEquals(7,cuenta("SELECT COUNT(*) FROM usuario"));
    }
    @Test void correoExistenteNoReemplazaCuentaNiPassword()throws Exception {
        byte[] token=token();crear(token);var s=solicitud();var dao=dao();
        assertEquals(409,assertThrows(AgendaException.class,()->dao.crear(90,s,token)).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM invitacion_personal"));
    }
    @Test void activacionEsDeUnSoloUsoYNoAceptaClavesDebiles()throws Exception {
        byte[] token=token();int id=crear(token);var dao=dao();
        assertEquals(400,assertThrows(AgendaException.class,()->dao.activar(token,"corta")).status());
        dao.activar(token,"ClaveNuevaSegura#2026");
        assertFalse(dao.vigente(token));assertEquals(410,assertThrows(AgendaException.class,()->dao.activar(token,"OtraClaveSegura#2026")).status());
        try(var c=abrir();var st=c.createStatement();var rs=st.executeQuery("SELECT password_hash,activo,rol FROM usuario WHERE id_usuario="+id)){rs.next();assertTrue(rs.getBoolean(2));assertEquals("MEDICO",rs.getString(3));assertTrue(SecurityUtil.checkPassword("ClaveNuevaSegura#2026",rs.getString(1)));}
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='PERSONAL_ACTIVADO'"));
    }
    @Test void venceExactamenteALosTreintaMinutos() {
        byte[] token=token();crear(token);var dao=despues(1800);
        assertFalse(dao.vigente(token));assertEquals(410,assertThrows(AgendaException.class,()->dao.activar(token,"ClaveNuevaSegura#2026")).status());
    }
    @Test void reenviarInvalidaElAnteriorSinActivarCuenta()throws Exception {
        byte[] anterior=token();int id=crear(anterior);byte[] nuevo=token();var dao=dao();
        assertEquals(429,assertThrows(AgendaException.class,()->dao.reenviar(90,id,nuevo)).status());
        despues(61).reenviar(90,id,nuevo);assertFalse(despues(61).vigente(anterior));assertTrue(despues(61).vigente(nuevo));
        assertEquals(1,cuenta("SELECT COUNT(*) FROM usuario WHERE id_usuario="+id+" AND activo=FALSE"));
    }
    @Test void noSePuedeActivarDesdeElPanelSinUsarLaInvitacion() {
        byte[] token=token();int id=crear(token);var dao=dao();
        assertEquals(409,assertThrows(AgendaException.class,()->dao.cambiarEstado(90,id,true)).status());
        dao.cambiarEstado(90,id,false);assertFalse(dao.vigente(token));
    }
    @Test void adminNoPuedeDeshabilitarseNiCambiarRolOCorreoDesdeEdicion() {
        var dao=dao();assertEquals(409,assertThrows(AgendaException.class,()->dao.cambiarEstado(90,90,false)).status());
        int id=crear(token());var original=solicitud();
        var otro=new InvitacionPersonalDAO.Solicitud(original.nombres(),original.apellidos(),original.correo(),"ADMINISTRADOR",null,null,null,null);
        assertEquals(400,assertThrows(AgendaException.class,()->dao.editar(90,id,otro,false)).status());
    }
    @Test void fallaDeAuditoriaRevierteActivacionCompleta()throws Exception {
        byte[] token=token();int id=crear(token);sql("DROP TABLE auditoria_seguridad");var dao=dao();
        assertEquals(503,assertThrows(AgendaException.class,()->dao.activar(token,"ClaveNuevaSegura#2026")).status());
        assertTrue(dao.vigente(token));assertEquals(1,cuenta("SELECT COUNT(*) FROM usuario WHERE id_usuario="+id+" AND activo=FALSE"));
    }
    @Test void dosActivacionesSimultaneasSoloPuedenConsumirUnEnlace()throws Exception {
        byte[] token=token();crear(token);var dao=dao();var inicio=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        Callable<Integer> tarea=()->{assertTrue(inicio.await(5,TimeUnit.SECONDS));try{dao.activar(token,"ClaveNuevaSegura#2026");return 1;}catch(AgendaException e){assertEquals(410,e.status());return 0;}};
        try{var a=executor.submit(tarea);var b=executor.submit(tarea);inicio.countDown();assertEquals(1,a.get(15,TimeUnit.SECONDS)+b.get(15,TimeUnit.SECONDS));}finally{executor.shutdownNow();}
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='PERSONAL_ACTIVADO'"));
    }
    @Test void ambitoSeObtieneDeLaIdentidadYRechazaRecursosDeOtraSede() {
        var ambito=new AmbitoAdmisionDAO(this::abrir);
        assertEquals(1,ambito.sede(91));ambito.exigir(91,AmbitoAdmisionDAO.Recurso.CITA,1);ambito.exigir(91,AmbitoAdmisionDAO.Recurso.PACIENTE,1);
        for(var recurso:AmbitoAdmisionDAO.Recurso.values())assertEquals(404,assertThrows(AgendaException.class,()->ambito.exigir(91,recurso,2)).status());
        assertEquals(403,assertThrows(AgendaException.class,()->ambito.sede(1)).status());
    }
    @Test void edicionDePacienteAjenoNoEscribeNombresNiContacto()throws Exception {
        var ambito=new AmbitoAdmisionDAO(this::abrir);var datos=new AmbitoAdmisionDAO.EdicionPaciente("Cambio","No autorizado","999999999","Dirección","Particular");
        assertEquals(404,assertThrows(AgendaException.class,()->ambito.editarPaciente(91,2,datos)).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM usuario WHERE id_usuario=2 AND nombres='Paciente'"));
        ambito.editarPaciente(91,1,datos);assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='PACIENTE_EDITADO'"));
    }
    @Test void cancelacionValidaSedeYRegistraAuditadaSinAfectarLaAjena()throws Exception {
        var admision=new AdmisionCitaDAO(this::abrir,reloj,10);
        assertEquals(404,assertThrows(AgendaException.class,()->admision.cancelar(91,2,"Imprevisto personal")).status());
        admision.cancelar(91,1,"Imprevisto personal");admision.cancelar(91,1,"Imprevisto personal");
        assertEquals(1,cuenta("SELECT COUNT(*) FROM cita WHERE id_cita=1 AND estado='CANCELADA'"));
        assertEquals(1,cuenta("SELECT COUNT(*) FROM cita WHERE id_cita=2 AND estado='CONFIRMADA'"));
        assertEquals(1,cuenta("SELECT COUNT(*) FROM auditoria_seguridad WHERE evento='CITA_CANCELADA_ADMISION'"));
        assertEquals(1,cuenta("SELECT COUNT(*) FROM notificacion WHERE id_usuario=1"));
    }
    @Test void editarPendienteConservaElEnlaceYNoPermiteHabilitarSinPassword() {
        byte[] token=token();int id=crear(token);var dao=dao();var s=solicitud();
        dao.editar(90,id,s,false);assertTrue(dao.vigente(token));
        assertEquals(409,assertThrows(AgendaException.class,()->dao.editar(90,id,s,true)).status());
    }
    @Test void laCreacionRevierteCuentaPerfilEInvitacionSiFallaAuditoria()throws Exception {
        sql("DROP TABLE auditoria_seguridad");var dao=dao();var solicitud=solicitud();byte[] token=token();
        assertEquals(503,assertThrows(AgendaException.class,()->dao.crear(90,solicitud,token)).status());
        assertEquals(7,cuenta("SELECT COUNT(*) FROM usuario"));assertEquals(2,cuenta("SELECT COUNT(*) FROM medico"));assertEquals(0,cuenta("SELECT COUNT(*) FROM invitacion_personal"));
    }
    @Test void unaCancelacionFallidaNoPublicaElAvisoNiModificaLaCita()throws Exception {
        sql("DROP TABLE auditoria_seguridad");var dao=new AdmisionCitaDAO(this::abrir,reloj,10);
        assertEquals(503,assertThrows(AgendaException.class,()->dao.cancelar(91,1,"Imprevisto personal")).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM cita WHERE id_cita=1 AND estado='CONFIRMADA'"));assertEquals(0,cuenta("SELECT COUNT(*) FROM notificacion"));
    }
    @Test void noSeDeshabilitaNiMueveUnMedicoConCitasVigentes()throws Exception {
        var dao=dao();
        assertEquals(409,assertThrows(AgendaException.class,()->dao.cambiarEstado(90,30,false)).status());
        var solicitud=new InvitacionPersonalDAO.Solicitud("Médico","Uno","m1@test.test","MEDICO","000001",1,2,null);
        assertEquals(409,assertThrows(AgendaException.class,()->dao.editar(90,30,solicitud,true)).status());
        assertEquals(1,cuenta("SELECT COUNT(*) FROM medico WHERE id_usuario=30 AND id_sede=1"));assertEquals(1,cuenta("SELECT COUNT(*) FROM usuario WHERE id_usuario=30 AND activo=TRUE"));
    }
}
