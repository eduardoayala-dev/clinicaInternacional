package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches=".+")
class AdmisionCitaMySqlTest extends AdmisionCitaContrato {
    @Override protected Connection abrir() throws SQLException {
        String url=System.getenv("AGENDA_MYSQL_URL");
        if(!url.contains("/clinica_agendas_test_")) throw new SQLException("Las pruebas requieren una base temporal aislada.");
        return MySqlPruebas.abrir();
    }
}
