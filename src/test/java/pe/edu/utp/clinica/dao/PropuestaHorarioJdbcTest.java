package pe.edu.utp.clinica.dao;

class PropuestaHorarioJdbcTest extends PropuestaHorarioContrato {
    @Override protected String url() { return "jdbc:h2:mem:propuestas;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"; }
}
