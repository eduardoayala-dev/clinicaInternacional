package pe.edu.utp.clinica.dao;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class PropuestaHorarioMySqlTest extends PropuestaHorarioContrato {
    @Override protected String url() { return System.getenv("AGENDA_MYSQL_URL"); }
}
