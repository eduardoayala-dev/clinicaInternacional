package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lista_espera")
public class ListaEsperaEntidad {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_lista_espera")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente")
    private PacienteEntidad paciente;

    @Column(name = "estado")
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad")
    private EspecialidadEntidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede")
    private SedeEntidad sede;

    @Column(name = "nivel_urgencia")
    private String prioridad;

    @Column(name = "rango_horario")
    private String horario;

    @Column(name = "canal_notificacion")
    private String canales;

    @Column(name = "fecha_registro")
    private java.time.LocalDateTime registro;

    @Column(name = "anticipacion_minutos")
    private Integer anticipacion;

    @Column(name = "id_medico_preferido")
    private Integer medico;

    @Column(name = "posicion")
    private Integer posicion;

    protected ListaEsperaEntidad() {}

    public ListaEsperaEntidad(
            PacienteEntidad paciente,
            EspecialidadEntidad especialidad,
            SedeEntidad sede,
            String prioridad,
            String horario,
            String canales,
            java.time.LocalDateTime registro,
            int anticipacion,
            Integer medico) {
        this.paciente = paciente;
        this.especialidad = especialidad;
        this.sede = sede;
        this.prioridad = prioridad;
        this.horario = horario;
        this.canales = canales;
        this.registro = registro;
        this.anticipacion = anticipacion;
        this.medico = medico;
        this.posicion = 0;
        this.estado = "ACTIVA";
    }

    public int id() {
        return id;
    }
}
