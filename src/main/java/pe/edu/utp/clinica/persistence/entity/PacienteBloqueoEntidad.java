package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="paciente")
@org.hibernate.annotations.Immutable
public class PacienteBloqueoEntidad {
    @Id @Column(name="id_paciente") private Integer id;
    @Column(name="id_usuario") private Integer usuario;

    protected PacienteBloqueoEntidad() {}
    public Integer usuario() { return usuario; }
}
