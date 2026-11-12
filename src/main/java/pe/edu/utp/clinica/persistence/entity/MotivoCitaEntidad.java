package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name="motivo_cita")
@Immutable
public class MotivoCitaEntidad {
    @Id @Column(name="id_motivo") private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_especialidad") private EspecialidadEntidad especialidad;
    @Column(name="descripcion") private String descripcion;
    @Column(name="estado") private String estado;

    protected MotivoCitaEntidad() {}
}
