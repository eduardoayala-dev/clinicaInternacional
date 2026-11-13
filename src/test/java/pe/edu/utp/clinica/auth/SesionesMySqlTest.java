package pe.edu.utp.clinica.auth;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class SesionesMySqlTest extends SesionesContrato {
    @Override protected Connection abrir() throws SQLException {
        return pe.edu.utp.clinica.dao.MySqlPruebas.abrir();
    }
}
