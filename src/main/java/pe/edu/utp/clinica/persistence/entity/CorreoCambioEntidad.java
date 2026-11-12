package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="correo_cambio_cita")
public class CorreoCambioEntidad {
    @Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) @Column(name="id_aviso") private Integer id;
    @Column(name="id_cita") private Integer cita;
    @Column(name="id_cita_anterior") private Integer anterior;
    @Column(name="tipo") private String tipo;
    @Column(name="disponible_en") private java.time.LocalDateTime disponible;

    protected CorreoCambioEntidad() {}
    public CorreoCambioEntidad(int cita, Integer anterior, String tipo, java.time.LocalDateTime disponible) {
        this.cita = cita; this.anterior = anterior; this.tipo = tipo; this.disponible = disponible;
    }
}
