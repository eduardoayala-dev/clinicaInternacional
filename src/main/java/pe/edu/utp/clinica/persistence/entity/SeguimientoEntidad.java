package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seguimiento_cita")
public class SeguimientoEntidad {
    @Id
    @Column(name = "id_cita_origen")
    private Integer id;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_cita_control")
    private CitaEntidad control;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_cita_origen", insertable = false, updatable = false)
    private CitaEntidad origen;

    @Column(name = "id_usuario_medico")
    private Integer usuario;

    @Column(name = "fecha_vinculacion")
    private java.time.LocalDateTime fecha;

    protected SeguimientoEntidad() {}

    public SeguimientoEntidad(
            int origen, CitaEntidad control, int usuario, java.time.LocalDateTime fecha) {
        this.id = origen;
        this.control = control;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public void vincular(CitaEntidad control, int usuario, java.time.LocalDateTime fecha) {
        this.control = control;
        this.usuario = usuario;
        this.fecha = fecha;
    }
}
