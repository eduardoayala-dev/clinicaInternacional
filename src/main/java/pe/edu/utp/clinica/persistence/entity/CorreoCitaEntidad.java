package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="correo_cita")
public class CorreoCitaEntidad {
    @Id @Column(name="id_cita") private Integer id;
    @Column(name="estado") private String estado;
    @Column(name="intentos") private int intentos;
    @Column(name="disponible_en") private java.time.LocalDateTime disponible;

    protected CorreoCitaEntidad() {}
    public CorreoCitaEntidad(int cita, java.time.LocalDateTime disponible) {
        this.id = cita; this.disponible = disponible; this.estado = "PENDIENTE";
    }
}
