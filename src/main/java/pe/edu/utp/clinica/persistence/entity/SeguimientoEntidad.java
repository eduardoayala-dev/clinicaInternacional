package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="seguimiento_cita")
@org.hibernate.annotations.Immutable
public class SeguimientoEntidad {
    @Id @Column(name="id_cita_origen") private Integer id;
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="id_cita_control") private CitaEntidad control;

    protected SeguimientoEntidad() {}

}
