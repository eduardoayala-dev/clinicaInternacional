package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="auditoria_seguridad")
public class AuditoriaEntidad {
    @Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) @Column(name="id_auditoria") private Integer id;
    @Column(name="id_usuario") private Integer usuario;
    @Column(name="evento") private String evento;
    @Column(name="descripcion") private String descripcion;
    @Column(name="sospechoso") private boolean sospechoso;

    protected AuditoriaEntidad() {}
    public AuditoriaEntidad(int usuario, String evento, String descripcion) {
        this.usuario = usuario; this.evento = evento; this.descripcion = descripcion;
    }
}
