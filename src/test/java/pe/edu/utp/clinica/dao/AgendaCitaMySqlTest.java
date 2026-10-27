package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "AGENDA_MYSQL_URL", matches = "jdbc:mysql:.*")
class AgendaCitaMySqlTest extends AgendaCitaContrato {
    @Override protected void configurar() {
        url = System.getenv("AGENDA_MYSQL_URL");
        user = System.getenv("AGENDA_MYSQL_USER");
        password = System.getenv("AGENDA_MYSQL_PASSWORD");
    }
}
