package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.nio.file.*;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionOfertasTest {
    private String url;
    private ConfiguracionOfertasDAO dao;
    protected Connection abrir()throws SQLException{return DriverManager.getConnection(url);}
    private void ejecutar(String sql)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(sql);}}
    @BeforeEach void preparar()throws Exception {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var conexion=abrir()){EsquemaPruebas.limpiar(conexion);}
        ejecutar("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,rol VARCHAR(40),activo BOOLEAN)");
        ejecutar("INSERT INTO usuario VALUES(1,'ADMINISTRADOR',TRUE),(2,'PERSONAL_ADMINISTRATIVO',TRUE),(3,'ADMINISTRADOR',FALSE)");
        ejecutar("CREATE TABLE auditoria_seguridad(id_usuario INT,evento VARCHAR(80),descripcion VARCHAR(255),sospechoso BOOLEAN)");
        String migracion=Files.readString(Path.of("sql/migrations/V013__configuracion_ofertas.sql")).replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","");
        for(String sentencia:migracion.split(";")){if(!sentencia.isBlank())ejecutar(sentencia);}
        dao=new ConfiguracionOfertasDAO(this::abrir,Clock.fixed(Instant.parse("2026-11-01T18:00:00Z"),ZoneOffset.UTC));
    }
    @Test void persisteYUnaNuevaInstanciaLeeLaMismaVersion()throws Exception {
        var guardado=dao.guardar(1,7,90,0);
        assertEquals(guardado,new ConfiguracionOfertasDAO(this::abrir,Clock.systemUTC()).leer());
        try(var c=abrir();var s=c.createStatement();var filas=s.executeQuery("SELECT actualizado_por,version FROM configuracion_ofertas")){assertTrue(filas.next());assertEquals(1,filas.getInt(1));assertEquals(1,filas.getLong(2));}
        try(var c=abrir();var s=c.createStatement();var filas=s.executeQuery("SELECT evento,descripcion FROM auditoria_seguridad")){assertTrue(filas.next());assertEquals("CONFIGURACION_OFERTAS",filas.getString(1));assertTrue(filas.getString(2).contains("15→7"));}
    }
    @Test void soloUnAdministradorActivoPuedeModificar() {
        for(int actor:new int[]{2,3,99})assertEquals(403,assertThrows(AgendaException.class,()->dao.guardar(actor,7,90,0)).status());
        assertEquals(15,dao.leer().minutosRespuesta());assertEquals(0,dao.leer().version());
    }
    @Test void rechazaParametrosFueraDeRangoSinGuardar() {
        for(int minutos:new int[]{-1,0,4,61})assertEquals(400,assertThrows(AgendaException.class,()->dao.guardar(1,minutos,0,0)).status());
        for(int anticipacion:new int[]{-1,10081})assertEquals(400,assertThrows(AgendaException.class,()->dao.guardar(1,15,anticipacion,0)).status());
        assertEquals(0,dao.leer().version());
    }
    @Test void unaPestanaAntiguaNoSobrescribeLaConfiguracion() {
        dao.guardar(1,7,90,0);
        assertEquals(409,assertThrows(AgendaException.class,()->dao.guardar(1,30,0,0)).status());
        assertEquals(7,dao.leer().minutosRespuesta());
    }
    @Test void siFallaLaAuditoriaSeRevierteElCambio()throws Exception {
        ejecutar("DROP TABLE auditoria_seguridad");
        assertEquals(503,assertThrows(AgendaException.class,()->dao.guardar(1,7,90,0)).status());
        assertEquals(15,dao.leer().minutosRespuesta());assertEquals(0,dao.leer().version());
    }
    @Test void noInventaConfiguracionSiFaltaLaMigracion()throws Exception {
        ejecutar("DROP TABLE configuracion_ofertas");
        assertEquals(503,assertThrows(AgendaException.class,dao::leer).status());
    }
}
