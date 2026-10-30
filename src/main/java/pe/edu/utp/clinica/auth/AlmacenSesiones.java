package pe.edu.utp.clinica.auth;

import pe.edu.utp.clinica.model.Usuario;
import java.sql.SQLException;
import java.time.Instant;

public interface AlmacenSesiones {
    void crear(String id, Usuario usuario, byte[] refreshHash, String csrf, Instant now, Instant vence) throws SQLException;
    SesionAutenticada resolver(String id, byte[] refreshHash, Instant now, boolean actividad) throws SQLException;
    void revocar(String id, Instant now) throws SQLException;
}
