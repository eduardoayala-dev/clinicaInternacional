package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="disponibilidad")
public class DisponibilidadEntidad {
    @Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) @Column(name="id_disponibilidad") private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_medico") private MedicoEntidad medico;
    @Column(name="id_sede") private Integer sede;
    @Column(name="fecha") private java.time.LocalDate fecha;
    @Column(name="hora_inicio") private java.time.LocalTime inicio;
    @Column(name="hora_fin") private java.time.LocalTime fin;
    @Column(name="duracion_minutos") private Integer minutos;
    @Column(name="estado") private String estado;

    protected DisponibilidadEntidad() {}
    public DisponibilidadEntidad(MedicoEntidad medico, int sede, java.time.LocalDate fecha,
            java.time.LocalTime inicio, java.time.LocalTime fin, int minutos) {
        this.medico = medico; this.sede = sede; this.fecha = fecha;
        this.inicio = inicio; this.fin = fin; this.minutos = minutos; this.estado = "PUBLICADO";
    }
    public Integer id() { return id; }
    public Integer sede() { return sede; }
    public java.time.LocalDate fecha() { return fecha; }
    public java.time.LocalTime inicio() { return inicio; }
    public java.time.LocalTime fin() { return fin; }
    public Integer minutos() { return minutos; }
    public void cambiarEstado(String estado) { this.estado = estado; }
}
