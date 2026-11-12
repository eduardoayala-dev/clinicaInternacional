package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "atencion_cita")
@org.hibernate.annotations.DynamicInsert
public class AtencionPrioridadEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    @Column(name = "id_medico")
    private Integer medico;

    @Column(name = "fecha_fin")
    private java.time.LocalDateTime fin;

    @Column(name = "id_especialidad_prioridad")
    private Integer especialidad;

    @Column(name = "id_paciente")
    private Integer paciente;

    @Column(name = "fecha_inicio")
    private java.time.LocalDateTime inicio;

    @Column(name = "motivo_no_atencion")
    private String motivo;

    @Column(name = "medico_activo", insertable = false, updatable = false)
    private Integer medicoActivo;

    @Column(name = "paciente_activo", insertable = false, updatable = false)
    private Integer pacienteActivo;

    public AtencionPrioridadEntidad(
            int cita,
            int medico,
            int paciente,
            java.time.LocalDateTime inicio,
            java.time.LocalDateTime fin,
            String motivo) {
        this.id = cita;
        this.medico = medico;
        this.paciente = paciente;
        this.inicio = inicio;
        this.fin = fin;
        this.motivo = motivo;
    }

    protected AtencionPrioridadEntidad() {}
}
