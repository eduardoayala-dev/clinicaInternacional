package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="cita")
public class CitaEntidad {
    @Column(name="id_paciente") private Integer paciente;
    @Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) @Column(name="id_cita") private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_medico") private MedicoEntidad medico;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_disponibilidad") private DisponibilidadEntidad disponibilidad;
    @Column(name="fecha_hora") private java.time.LocalDateTime inicio;
    @Column(name="estado") private String estado;

    @Column(name="id_motivo") private Integer motivo;
    @Column(name="nivel_urgencia") private String prioridad;
    @Column(name="riesgo_cancelacion") private String riesgo;
    @Column(name="fecha_recomendada_proxima") private java.time.LocalDate recomendada;

    protected CitaEntidad() {}
    public CitaEntidad(int paciente, MedicoEntidad medico, DisponibilidadEntidad disponibilidad,
            java.time.LocalDateTime inicio, Integer motivo, String prioridad, String riesgo) {
        this.paciente = paciente; this.medico = medico; this.disponibilidad = disponibilidad;
        this.inicio = inicio; this.motivo = motivo; this.prioridad = prioridad;
        this.riesgo = riesgo; this.estado = "CONFIRMADA";
    }
    public Integer id() { return id; }
}
