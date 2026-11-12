package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "correo_oferta")
@org.hibernate.annotations.DynamicInsert
public class CorreoOfertaEntidad extends ColaCorreoEntidad {
    @Id
    @Column(name = "id_oferta")
    private Integer id;

    protected CorreoOfertaEntidad() {}

    public CorreoOfertaEntidad(int id, LocalDateTime disponible) {
        super(disponible);
        this.id = id;
    }
}
