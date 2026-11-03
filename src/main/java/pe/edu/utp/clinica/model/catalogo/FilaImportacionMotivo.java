package pe.edu.utp.clinica.model.catalogo;

public record FilaImportacionMotivo(int numero,int especialidad,String descripcion,String error) implements java.io.Serializable {
    public FilaImportacionMotivo {
        descripcion=descripcion==null?"":descripcion;
        error=error==null?"":error;
    }
}
