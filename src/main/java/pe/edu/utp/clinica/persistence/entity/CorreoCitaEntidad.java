package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "correo_cita")
@org.hibernate.annotations.DynamicInsert
public class CorreoCitaEntidad extends ColaCorreoEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    protected CorreoCitaEntidad() {}

    public CorreoCitaEntidad(int id, LocalDateTime disponible) {
        super(disponible);
        this.id = id;
    }
}
