package pe.edu.utp.clinica.dao;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class AvisosCambiosMySqlTest extends AvisosCambiosJdbcTest {
    @Override protected void configurar(){url=System.getenv("AGENDA_MYSQL_URL");}
}
