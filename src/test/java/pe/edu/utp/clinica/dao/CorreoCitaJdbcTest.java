package pe.edu.utp.clinica.dao;

class CorreoCitaJdbcTest extends CorreoCitaContrato {
    @Override protected void configurar(){url="jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";}
}
