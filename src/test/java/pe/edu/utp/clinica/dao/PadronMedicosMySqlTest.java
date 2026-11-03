package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL", matches=".+")
class PadronMedicosMySqlTest extends PadronMedicosDAOTest {
    @Override protected String urlPruebas() { return System.getenv("AGENDA_MYSQL_URL"); }
    @Override protected Connection abrir()throws SQLException {
        return DriverManager.getConnection(urlPruebas(),System.getenv("AGENDA_MYSQL_USER"),System.getenv("AGENDA_MYSQL_PASSWORD"));
    }
}
