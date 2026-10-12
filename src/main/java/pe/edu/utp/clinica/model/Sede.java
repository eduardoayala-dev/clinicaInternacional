package pe.edu.utp.clinica.model;

public class Sede {
    private int idSede;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String imagenUrl;
    private String estado; // ACTIVO, MANTENIMIENTO, INACTIVO

    public Sede() {
        this.estado = "ACTIVO";
    }

    public Sede(int idSede, String codigo, String nombre, String direccion, String telefono, String imagenUrl, String estado) {
        this.idSede = idSede;
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.imagenUrl = imagenUrl;
        this.estado = (estado != null && !estado.trim().isEmpty()) ? estado.toUpperCase() : "ACTIVO";
    }

    public int getIdSede() {
        return idSede;
    }

    public void setIdSede(int idSede) {
        this.idSede = idSede;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getEstado() {
        return estado != null ? estado : "ACTIVO";
    }

    public void setEstado(String estado) {
        this.estado = (estado != null && !estado.trim().isEmpty()) ? estado.toUpperCase() : "ACTIVO";
    }

    public boolean isActivo() {
        return "ACTIVO".equalsIgnoreCase(this.estado);
    }

    public int getConsultorios() {
        if (codigo != null) {
            String c = codigo.toUpperCase();
            if (c.contains("SB") || c.contains("SUR-01") || c.contains("NORTE")) return 24;
            if (c.contains("LI") || c.contains("CENTRO")) return 18;
            if (c.contains("LM") || c.contains("MOLINA")) return 12;
            if (c.contains("SU") || c.contains("SURCO")) return 10;
        }
        if (nombre != null) {
            String n = nombre.toLowerCase();
            if (n.contains("borja")) return 24;
            if (n.contains("lima") || n.contains("garcilaso") || n.contains("central")) return 18;
            if (n.contains("molina")) return 12;
            if (n.contains("surco")) return 10;
        }
        return 15;
    }

    public int getEspecialidadesCount() {
        if (codigo != null) {
            String c = codigo.toUpperCase();
            if (c.contains("SB") || c.contains("NORTE")) return 16;
            if (c.contains("LI") || c.contains("CENTRO")) return 14;
            if (c.contains("LM") || c.contains("MOLINA")) return 9;
            if (c.contains("SU") || c.contains("SURCO")) return 8;
        }
        if (nombre != null) {
            String n = nombre.toLowerCase();
            if (n.contains("borja")) return 16;
            if (n.contains("lima") || n.contains("garcilaso") || n.contains("central")) return 14;
            if (n.contains("molina")) return 9;
            if (n.contains("surco")) return 8;
        }
        return 10;
    }

    public String getDireccionPrincipal() {
        if (direccion == null || direccion.trim().isEmpty()) return "Sin dirección";
        if (direccion.contains(",")) {
            return direccion.substring(0, direccion.indexOf(",")).trim();
        }
        return direccion;
    }

    public String getZonaDistrito() {
        if (direccion != null && direccion.contains(",")) {
            String[] parts = direccion.split(",");
            return parts[parts.length - 1].trim();
        }
        if (nombre != null) {
            String n = nombre.toLowerCase();
            if (n.contains("san borja") || n.contains("borja")) return "San Borja";
            if (n.contains("surco")) return "Santiago de Surco";
            if (n.contains("molina")) return "La Molina";
            if (n.contains("lima") || n.contains("central") || n.contains("garcilaso")) return "Cercado de Lima";
        }
        return "Lima";
    }

    public String getEstadoEtiqueta() {
        if ("ACTIVO".equalsIgnoreCase(this.estado)) return "Activa";
        if ("MANTENIMIENTO".equalsIgnoreCase(this.estado)) return "En mantenimiento";
        return "Inactiva";
    }
}
