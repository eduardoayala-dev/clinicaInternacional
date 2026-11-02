package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MedicinaGeneralCatalogoTest {
    private String url;
    protected Connection abrir()throws SQLException{return DriverManager.getConnection(url);}
    private void sql(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(texto);}}
    private int cantidad(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement();var r=s.executeQuery(texto)){r.next();return r.getInt(1);}}
    @BeforeEach void preparar()throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var c=abrir()){EsquemaPruebas.limpiar(c);}
        sql("CREATE TABLE especialidad(id_especialidad INT AUTO_INCREMENT PRIMARY KEY,nombre VARCHAR(100) UNIQUE,descripcion VARCHAR(300),estado VARCHAR(20))");
        sql("CREATE TABLE motivo_cita(id_motivo INT AUTO_INCREMENT PRIMARY KEY,id_especialidad INT,descripcion VARCHAR(200),estado VARCHAR(20))");
        sql("CREATE TABLE medico(id_medico INT PRIMARY KEY,id_especialidad INT)");
    }
    private void aplicar()throws Exception {
        try(var c=abrir();var s=c.createStatement()) {
            c.setAutoCommit(false);
            for(String sentencia:Files.readString(Path.of("sql/catalogos/medicina-general.sql")).split(";")) {
                String texto=sentencia.strip();
                if(!texto.isEmpty()&&!texto.equals("START TRANSACTION")&&!texto.equals("COMMIT"))s.execute(texto);
            }
            c.commit();
        }
    }
    @Test void agregaCatalogoSinDuplicarNiCrearMedicosUHorarios()throws Exception {
        sql("INSERT INTO especialidad(id_especialidad,nombre,descripcion,estado) VALUES(99,'Otra especialidad','Otra','ACTIVO')");
        aplicar();aplicar();
        assertEquals(1,cantidad("SELECT COUNT(*) FROM especialidad WHERE nombre='Medicina General'"));
        assertEquals(5,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(0,cantidad("SELECT COUNT(*) FROM medico"));
        var dao=new CatalogoOrientacionDAO(){@Override protected Connection abrir()throws SQLException{return MedicinaGeneralCatalogoTest.this.abrir();}};
        var general=dao.activas().stream().filter(e->e.nombre().equals("Medicina General")).findFirst().orElseThrow();
        assertNotEquals(13,general.id());assertEquals(5,general.motivos().size());
    }
    @Test void respetaEspecialidadDesactivadaPorElAdministrador()throws Exception {
        sql("INSERT INTO especialidad VALUES(13,'Medicina General','Desactivada','INACTIVO')");aplicar();
        assertEquals(1,cantidad("SELECT COUNT(*) FROM especialidad WHERE estado='INACTIVO'"));assertEquals(0,cantidad("SELECT COUNT(*) FROM motivo_cita"));
    }
    @Test void noReactivaMotivosQueYaFueronDesactivados()throws Exception {
        sql("INSERT INTO especialidad VALUES(13,'Medicina General','General','ACTIVO')");
        sql("INSERT INTO motivo_cita VALUES(80,13,'Evaluación general','INACTIVO')");aplicar();
        assertEquals(5,cantidad("SELECT COUNT(*) FROM motivo_cita"));assertEquals(1,cantidad("SELECT COUNT(*) FROM motivo_cita WHERE estado='INACTIVO'"));
    }
}
