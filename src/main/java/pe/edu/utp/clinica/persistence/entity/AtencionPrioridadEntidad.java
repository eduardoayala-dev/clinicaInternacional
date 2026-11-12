package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="atencion_cita")
@org.hibernate.annotations.Immutable
public class AtencionPrioridadEntidad {
    @Id @Column(name="id_cita") private Integer id;
    @Column(name="id_medico") private Integer medico;
    @Column(name="fecha_fin") private java.time.LocalDateTime fin;
    @Column(name="id_especialidad_prioridad") private Integer especialidad;

    protected AtencionPrioridadEntidad() {}

}
