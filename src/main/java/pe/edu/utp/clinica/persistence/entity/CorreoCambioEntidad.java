package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "correo_cambio_cita")
@org.hibernate.annotations.DynamicInsert
public class CorreoCambioEntidad extends ColaCorreoEntidad {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_aviso")
    private Integer id;

    @Column(name = "id_cita")
    private Integer cita;

    @Column(name = "id_cita_anterior")
    private Integer anterior;

    @Column(name = "tipo")
    private String tipo;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_cita", insertable = false, updatable = false)
    private CitaEntidad citaRelacion;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(
            name = "id_cita_anterior",
            insertable = false,
            updatable = false)
    private CitaEntidad anteriorRelacion;

    protected CorreoCambioEntidad() {}

    public CorreoCambioEntidad(
            int cita, Integer anterior, String tipo, java.time.LocalDateTime disponible) {
        super(disponible);
        this.cita = cita;
        this.anterior = anterior;
        this.tipo = tipo;
    }
}
