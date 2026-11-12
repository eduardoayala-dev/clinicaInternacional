package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="medico")
@org.hibernate.annotations.Immutable
public class MedicoBloqueoEntidad {
    @Id @Column(name="id_medico") private Integer id;
    @Column(name="id_sede") private Integer sede;
    @Column(name="id_especialidad") private Integer especialidad;
    @Column(name="estado") private String estado;

    protected MedicoBloqueoEntidad() {}
    public Integer sede() { return sede; }
    public Integer especialidad() { return especialidad; }
    public String estado() { return estado; }
}
