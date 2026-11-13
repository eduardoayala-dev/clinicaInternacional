package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "correo_recordatorio")
@org.hibernate.annotations.DynamicInsert
public class CorreoRecordatorioEntidad extends ColaCorreoEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    protected CorreoRecordatorioEntidad() {}

    public CorreoRecordatorioEntidad(int id, LocalDateTime disponible) {
        super(disponible);
        this.id = id;
    }
}
