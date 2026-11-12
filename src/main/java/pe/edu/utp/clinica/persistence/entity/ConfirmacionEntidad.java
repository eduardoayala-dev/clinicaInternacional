package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="confirmacion_asistencia")
public class ConfirmacionEntidad {
    @Id @Column(name="id_cita") private Integer id;
    @Column(name="creada_en") private java.time.LocalDateTime creada;
    @Column(name="vence_en") private java.time.LocalDateTime vence;

    protected ConfirmacionEntidad() {}
    public ConfirmacionEntidad(int cita, java.time.LocalDateTime creada, java.time.LocalDateTime vence) {
        this.id = cita; this.creada = creada; this.vence = vence;
    }
}
