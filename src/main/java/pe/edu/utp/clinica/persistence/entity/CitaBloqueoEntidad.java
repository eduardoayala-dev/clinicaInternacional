package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="cita")
@org.hibernate.annotations.Immutable
public class CitaBloqueoEntidad {
    @Id @Column(name="id_cita") private Integer id;
    @Column(name="id_paciente") private Integer paciente;
    @Column(name="fecha_hora") private java.time.LocalDateTime inicio;
    @Column(name="estado") private String estado;

    protected CitaBloqueoEntidad() {}
    public java.time.LocalDateTime inicio() { return inicio; }
    public String estado() { return estado; }
}
