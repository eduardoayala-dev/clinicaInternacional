package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="retencion_cupo")
public class RetencionEntidad {
    @Id @Column(name="id_paciente") private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_medico") private MedicoEntidad medico;
    @Column(name="inicio") private java.time.LocalDateTime inicio;
    @Column(name="duracion_minutos") private Integer minutos;
    @Column(name="vence") private java.time.LocalDateTime vence;

    @Column(name="identificador") private String identificador;

    protected RetencionEntidad() {}
    public RetencionEntidad(int paciente, MedicoEntidad medico, java.time.LocalDateTime inicio,
            int minutos, String identificador, java.time.LocalDateTime vence) {
        this.id = paciente; this.medico = medico; this.inicio = inicio;
        this.minutos = minutos; this.identificador = identificador; this.vence = vence;
    }
}
