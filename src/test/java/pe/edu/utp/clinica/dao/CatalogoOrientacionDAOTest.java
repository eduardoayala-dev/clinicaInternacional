package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoOrientacionDAOTest {
    @Test void consultaSoloActivasSinIdsFijosYPreservaAcentos() throws Exception {
        String url="jdbc:h2:mem:catalogo_orientacion;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var conn=DriverManager.getConnection(url);var st=conn.createStatement()) {
            st.execute("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20))");
            st.execute("CREATE TABLE motivo_cita(id_motivo INT PRIMARY KEY,id_especialidad INT,descripcion VARCHAR(200),estado VARCHAR(20))");
            st.execute("INSERT INTO especialidad VALUES(72,'Dermatología','ACTIVO'),(83,'Neurología','INACTIVO')");
            st.execute("INSERT INTO motivo_cita VALUES(8,72,'Revisión de lunares','ACTIVO'),(9,72,'Motivo inactivo','INACTIVO'),(10,83,'Especialidad inactiva','ACTIVO')");
        }
        var dao=new CatalogoOrientacionDAO(){@Override protected Connection abrir()throws java.sql.SQLException{return DriverManager.getConnection(url);}};
        var lista=dao.activas();assertEquals(1,lista.size());assertEquals(72,lista.get(0).id());assertEquals("Dermatología",lista.get(0).nombre());
        assertEquals(1,lista.get(0).motivos().size());assertEquals(8,lista.get(0).motivos().get(0).id());
    }
}
