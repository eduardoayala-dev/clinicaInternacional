package pe.edu.utp.clinica.service.dni;

@FunctionalInterface
public interface DniProvider {
    DniDatos consultar(String numero) throws DniException;
}
