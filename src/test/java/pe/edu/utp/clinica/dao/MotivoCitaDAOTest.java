package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MotivoCitaDAOTest {
    @Test void utilizaIdsRealesYExcluyeMotivosOEspecialidadesInactivas() throws Exception {
        String url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var c=DriverManager.getConnection(url);var s=c.createStatement()) {
            s.execute("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,estado VARCHAR(20))");
            s.execute("CREATE TABLE motivo_cita(id_motivo INT PRIMARY KEY,id_especialidad INT,descripcion VARCHAR(200),estado VARCHAR(20))");
            s.execute("INSERT INTO especialidad VALUES(1,'ACTIVO'),(2,'ACTIVO'),(3,'INACTIVO')");
            s.execute("INSERT INTO motivo_cita VALUES(71,1,'Control cardíaco','ACTIVO'),(84,2,'Revisión de piel','ACTIVO'),(99,1,'Motivo retirado','INACTIVO'),(100,3,'Especialidad retirada','ACTIVO')");
        }
        var dao=new MotivoCitaDAO(()->DriverManager.getConnection(url));
        assertEquals(2,dao.listarActivos().size());
        var motivos=dao.listarEspecialidad(2);
        assertEquals(1,motivos.size());assertEquals(84,motivos.get(0).get("idMotivo"));
        assertEquals("Revisión de piel",motivos.get(0).get("descripcion"));
        assertTrue(dao.listarEspecialidad(99).isEmpty());
    }
    @Test void unFalloDeBaseNoSeConvierteEnMotivosInventados() {
        var dao=new MotivoCitaDAO(()->{throw new SQLException("No disponible");});
        assertEquals(503,assertThrows(AgendaException.class,dao::listarActivos).status());
    }
}
