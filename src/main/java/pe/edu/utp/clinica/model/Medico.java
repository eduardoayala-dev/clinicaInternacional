package pe.edu.utp.clinica.model;

public class Medico {
    private int idMedico;
    private int usuarioId;
    private String cmp;
    private int especialidadId;
    private int sedeId;
    private String estado;

    public Medico() {}

    public Medico(int idMedico, int usuarioId, String cmp, int especialidadId, int sedeId, String estado) {
        this.idMedico = idMedico;
        this.usuarioId = usuarioId;
        this.cmp = cmp;
        this.especialidadId = especialidadId;
        this.sedeId = sedeId;
        this.estado = estado;
    }

    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int idMedico) { this.idMedico = idMedico; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getCmp() { return cmp; }
    public void setCmp(String cmp) { this.cmp = cmp; }

    public int getEspecialidadId() { return especialidadId; }
    public void setEspecialidadId(int especialidadId) { this.especialidadId = especialidadId; }

    public int getSedeId() { return sedeId; }
    public void setSedeId(int sedeId) { this.sedeId = sedeId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
