package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class MySqlPruebasTest {
    @Test void rejectsRealDatabasesBeforeOpeningAConnection() {
        for (String url : new String[]{"jdbc:mysql://db/clinica_db", "jdbc:mysql://db/railway",
                "jdbc:mysql://db/clinica_agendas_test_../railway", "jdbc:h2:mem:test", "", null}) {
            assertThrows(SQLException.class, () -> MySqlPruebas.validarUrl(url));
        }
    }
    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
    void pooledConnectionsStillNegotiateTls() throws Exception {
        try (var connection=MySqlPruebas.abrir();var st=connection.createStatement();var rs=st.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
            assertTrue(rs.next()); assertFalse(rs.getString(2).isBlank());
        }
    }
    @Test void acceptsOnlyExplicitTemporarySchemas() {
        assertDoesNotThrow(() -> MySqlPruebas.validarUrl("jdbc:mysql://db:3306/clinica_agendas_test_123?sslMode=REQUIRED"));
    }
}
