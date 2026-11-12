package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reasignacion_inteligente")
public class OfertaEntidad {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_reasignacion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita_original")
    private CitaEntidad cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_disponibilidad")
    private DisponibilidadEntidad disponibilidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lista_espera")
    private ListaEsperaEntidad lista;

    @Column(name = "fecha_limite_respuesta")
    private java.time.LocalDateTime vence;

    @Column(name = "estado")
    private String estado;

    @Column(name = "fecha_oferta")
    private java.time.LocalDateTime creada;

    @Column(name = "id_cita_asignada")
    private Integer asignada;

    protected OfertaEntidad() {}

    public OfertaEntidad(
            CitaEntidad cita,
            ListaEsperaEntidad lista,
            DisponibilidadEntidad disponibilidad,
            java.time.LocalDateTime creada,
            java.time.LocalDateTime vence) {
        this.cita = cita;
        this.lista = lista;
        this.disponibilidad = disponibilidad;
        this.creada = creada;
        this.vence = vence;
        this.estado = "PENDIENTE";
    }

    public int id() {
        return id;
    }
}
