package pe.edu.utp.clinica.dao;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches=".+")
class CorreoCitaMySqlTest extends CorreoCitaContrato {
    @Override protected void configurar(){url=System.getenv("AGENDA_MYSQL_URL");}
}
