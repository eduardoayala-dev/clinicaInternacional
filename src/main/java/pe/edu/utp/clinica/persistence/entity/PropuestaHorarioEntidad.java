package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "propuesta_horario")
@org.hibernate.annotations.DynamicInsert
public class PropuestaHorarioEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propuesta")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medico")
    private MedicoEntidad medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede")
    private SedeEntidad sede;

    @Column(name = "dias_semana")
    private Integer dias;

    @Column(name = "vigente_desde")
    private LocalDate desde;

    @Column(name = "vigente_hasta")
    private LocalDate hasta;

    @Column(name = "hora_inicio")
    private LocalTime inicio;

    @Column(name = "hora_fin")
    private LocalTime fin;

    @Column(name = "duracion_minutos")
    private Integer minutos;

    @Column(name = "estado")
    private String estado = "PENDIENTE";

    @Column(name = "creada_en")
    private LocalDateTime creada;

    @Column(name = "revisada_en")
    private LocalDateTime revisada;

    @Column(name = "id_revisor")
    private Integer revisor;

    @Column(name = "motivo_revision")
    private String motivo;

    protected PropuestaHorarioEntidad() {}

    public PropuestaHorarioEntidad(
            MedicoEntidad medico,
            SedeEntidad sede,
            int dias,
            LocalDate desde,
            LocalDate hasta,
            LocalTime inicio,
            LocalTime fin,
            int minutos,
            LocalDateTime creada) {
        this.medico = medico;
        this.sede = sede;
        this.dias = dias;
        this.desde = desde;
        this.hasta = hasta;
        this.inicio = inicio;
        this.fin = fin;
        this.minutos = minutos;
        this.creada = creada;
    }

    public int id() {
        return id;
    }
}
