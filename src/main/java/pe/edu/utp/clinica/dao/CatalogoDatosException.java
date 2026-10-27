package pe.edu.utp.clinica.dao;

public final class CatalogoDatosException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CatalogoDatosException(Throwable cause) {
        super("No fue posible consultar el catálogo.", cause);
    }
}
