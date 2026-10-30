package pe.edu.utp.clinica.recuperacion;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class RecuperacionMySqlTest extends RecuperacionContrato {
    @Override protected Connection abrir() throws SQLException {
        url=System.getenv("AGENDA_MYSQL_URL");
        return pe.edu.utp.clinica.dao.MySqlPruebas.abrir();
    }
}
