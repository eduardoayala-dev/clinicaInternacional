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
            st.execute("INSERT INTO especialidad VALUES(72,'Dermatología','ACTIVO'),(83,'Neurología','INACTIVO')");
        }
        var dao=new CatalogoOrientacionDAO(){@Override protected Connection abrir()throws java.sql.SQLException{return DriverManager.getConnection(url);}};
        var lista=dao.activas();assertEquals(1,lista.size());assertEquals(72,lista.get(0).id());assertEquals("Dermatología",lista.get(0).nombre());
    }
}
