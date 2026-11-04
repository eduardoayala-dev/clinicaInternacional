package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.Clock;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ActividadAdmisionTest {
    private String url;private InicioOperativoDAO dao;
    private Connection abrir()throws SQLException{return DriverManager.getConnection(url);}
    private void sql(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(texto);}}
    @BeforeEach void preparar()throws SQLException{
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";dao=new InicioOperativoDAO(this::abrir,Clock.systemUTC());
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,nombres VARCHAR(100),apellidos VARCHAR(100),rol VARCHAR(40),activo BOOLEAN)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,estado VARCHAR(20))");sql("CREATE TABLE personal_administrativo(id_usuario INT PRIMARY KEY,id_sede INT)");
        sql("CREATE TABLE paciente(id_paciente INT PRIMARY KEY,id_usuario INT)");sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_sede INT)");sql("CREATE TABLE disponibilidad(id_disponibilidad INT PRIMARY KEY,id_sede INT)");
        sql("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_paciente INT,id_medico INT,id_disponibilidad INT,fecha_hora DATETIME)");
        sql("CREATE TABLE notificacion(id_notificacion INT PRIMARY KEY,id_cita INT,fecha_envio DATETIME,tipo VARCHAR(100),mensaje VARCHAR(300))");
        sql("INSERT INTO usuario VALUES(1,'Admisión','Uno','PERSONAL_ADMINISTRATIVO',TRUE),(2,'Paciente','Uno','PACIENTE',TRUE)");sql("INSERT INTO sede VALUES(1,'ACTIVO'),(2,'ACTIVO')");sql("INSERT INTO personal_administrativo VALUES(1,1)");
        sql("INSERT INTO paciente VALUES(1,2)");sql("INSERT INTO medico VALUES(1,1),(2,2)");sql("INSERT INTO disponibilidad VALUES(1,1),(2,2)");sql("INSERT INTO cita VALUES(9,1,1,1,'2026-11-05 09:00:00'),(10,1,2,2,'2026-11-05 10:00:00')");
    }
    @Test void mensajesSonDeAdmisionConCitaPacienteYFechaSinTextoDirigidoAlPaciente()throws SQLException{
        sql("INSERT INTO notificacion VALUES(1,9,'2026-11-04 08:19:00','Citas','Tu cita médica ha sido cancelada correctamente.'),(2,10,'2026-11-04 08:20:00','Citas','Otra sede')");
        var actividad=dao.operaciones(1);assertEquals(1,actividad.size());var item=actividad.get(0);
        assertEquals("04/11/2026 08:19",item.get("hora"));assertTrue(item.get("texto").contains("Aviso de cancelación registrado"));assertTrue(item.get("texto").contains("Cita #9"));assertTrue(item.get("texto").contains("Paciente Uno"));assertFalse(item.get("texto").contains("Tu cita"));
        try(var c=abrir();var s=c.createStatement();var r=s.executeQuery("SELECT mensaje FROM notificacion WHERE id_notificacion=1")){r.next();assertTrue(r.getString(1).startsWith("Tu cita"));}
    }
    @Test void sinNotificacionesNoSeInventanOperacionesYSeExigeRol(){assertTrue(dao.operaciones(1).isEmpty());assertEquals(403,assertThrows(AgendaException.class,()->dao.operaciones(2)).status());}
}
