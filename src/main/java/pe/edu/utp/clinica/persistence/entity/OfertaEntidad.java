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
@Table(name="reasignacion_inteligente")
@Immutable
public class OfertaEntidad {
    @Id @Column(name="id_reasignacion") private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_cita_original") private CitaEntidad cita;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_disponibilidad") private DisponibilidadEntidad disponibilidad;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_lista_espera") private ListaEsperaEntidad lista;
    @Column(name="fecha_limite_respuesta") private java.time.LocalDateTime vence;
    @Column(name="estado") private String estado;

    protected OfertaEntidad() {}
}
