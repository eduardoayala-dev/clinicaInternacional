package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "especialidad")
@Immutable
public class EspecialidadEntidad {
    @Column(name = "nombre")
    private String nombre;

    @jakarta.persistence.OneToMany(mappedBy = "especialidad")
    private java.util.Set<MedicoEntidad> medicos;

    @Id
    @Column(name = "id_especialidad")
    private Integer id;

    @Column(name = "estado")
    private String estado;

    @jakarta.persistence.OneToMany(mappedBy = "especialidad")
    private java.util.Set<MotivoCitaEntidad> motivos;

    protected EspecialidadEntidad() {}
}
