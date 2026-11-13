package pe.edu.utp.clinica.dao;

import java.util.UUID;

class OfertasDaoTest extends OfertasContrato {
    @Override protected void configurar() {
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    }
}
