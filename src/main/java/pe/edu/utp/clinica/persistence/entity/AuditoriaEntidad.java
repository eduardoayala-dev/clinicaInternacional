package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditoria_seguridad")
@org.hibernate.annotations.DynamicInsert
public class AuditoriaEntidad {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer id;

    @Column(name = "id_usuario")
    private Integer usuario;

    @Column(name = "evento")
    private String evento;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "sospechoso")
    private boolean sospechoso;

    @Column(name = "ip_origen")
    private String ip;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private java.time.LocalDateTime fecha;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private UsuarioEntidad actor;

    protected AuditoriaEntidad() {}

    public AuditoriaEntidad(int usuario, String evento, String descripcion) {
        this.usuario = usuario;
        this.evento = evento;
        this.descripcion = descripcion;
    }

    public AuditoriaEntidad(
            Integer usuario, String ip, String evento, String descripcion, boolean sospechoso) {
        this.usuario = usuario;
        this.ip = ip;
        this.evento = evento;
        this.descripcion = descripcion;
        this.sospechoso = sospechoso;
    }
}
