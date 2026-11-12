package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inasistencia_cita")
public class InasistenciaEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    @Column(name = "id_usuario_personal")
    private Integer usuario;

    @Column(name = "fecha_registro")
    private LocalDateTime fecha;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "tolerancia_minutos")
    private Integer tolerancia;

    protected InasistenciaEntidad() {}

    public InasistenciaEntidad(
            int cita, int usuario, LocalDateTime fecha, String motivo, int tolerancia) {
        this.id = cita;
        this.usuario = usuario;
        this.fecha = fecha;
        this.motivo = motivo;
        this.tolerancia = tolerancia;
    }
}
