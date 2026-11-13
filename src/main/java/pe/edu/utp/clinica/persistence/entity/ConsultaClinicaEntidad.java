package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import pe.edu.utp.clinica.model.medico.ConsultaClinica;

@Entity
@Table(name = "consulta_clinica")
public class ConsultaClinicaEntidad {
    @Id
    @Column(name = "id_cita")
    private Integer id;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "antecedentes")
    private String antecedentes;

    @Column(name = "hallazgos")
    private String hallazgos;

    @Column(name = "evaluacion")
    private String evaluacion;

    @Column(name = "diagnostico")
    private String diagnostico;

    @Column(name = "plan")
    private String plan;

    protected ConsultaClinicaEntidad() {}

    public ConsultaClinicaEntidad(int cita, ConsultaClinica consulta) {
        this.id = cita;
        this.motivo = consulta.motivo();
        this.antecedentes = consulta.antecedentes();
        this.hallazgos = consulta.hallazgos();
        this.evaluacion = consulta.evaluacion();
        this.diagnostico = consulta.diagnostico();
        this.plan = consulta.plan();
    }
}
