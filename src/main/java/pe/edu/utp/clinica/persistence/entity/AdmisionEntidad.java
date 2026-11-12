package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admision_cita")
public class AdmisionEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    @Column(name = "fecha_llegada")
    private LocalDateTime llegada;

    @Column(name = "id_usuario_personal")
    private Integer usuario;

    @Column(name = "tolerancia_minutos")
    private Integer tolerancia;

    @Column(name = "llego_tarde")
    private boolean tarde;

    public AdmisionEntidad(
            int cita, int usuario, LocalDateTime llegada, int tolerancia, boolean tarde) {
        this.id = cita;
        this.usuario = usuario;
        this.llegada = llegada;
        this.tolerancia = tolerancia;
        this.tarde = tarde;
    }

    protected AdmisionEntidad() {}
}
