package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_asistencia")
public class HistorialAsistenciaEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer id;

    @Column(name = "id_paciente")
    private Integer paciente;

    @Column(name = "id_cita")
    private Integer cita;

    @Column(name = "asistio")
    private boolean asistio;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "fecha_registro")
    private LocalDateTime fecha;

    protected HistorialAsistenciaEntidad() {}

    public HistorialAsistenciaEntidad(
            int paciente, int cita, boolean asistio, String observacion, LocalDateTime fecha) {
        this.paciente = paciente;
        this.cita = cita;
        this.asistio = asistio;
        this.observacion = observacion;
        this.fecha = fecha;
    }
}
