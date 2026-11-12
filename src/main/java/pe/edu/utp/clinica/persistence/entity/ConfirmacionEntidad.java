package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "confirmacion_asistencia")
@org.hibernate.annotations.DynamicInsert
public class ConfirmacionEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    @Column(name = "creada_en")
    private java.time.LocalDateTime creada;

    @Column(name = "vence_en")
    private java.time.LocalDateTime vence;

    @Column(name = "confirmada_en")
    private java.time.LocalDateTime confirmada;

    @Column(name = "canal")
    private String canal;

    @Column(name = "id_usuario_confirmo")
    private Integer usuario;

    protected ConfirmacionEntidad() {}

    public ConfirmacionEntidad(
            int cita, java.time.LocalDateTime creada, java.time.LocalDateTime vence) {
        this.id = cita;
        this.creada = creada;
        this.vence = vence;
    }
}
