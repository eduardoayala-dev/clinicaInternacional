package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="notificacion")
public class NotificacionEntidad {
    @Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) @Column(name="id_notificacion") private Integer id;
    @Column(name="id_usuario") private Integer usuario;
    @Column(name="id_cita") private Integer cita;
    @Column(name="tipo") private String tipo;
    @Column(name="canal") private String canal;
    @Column(name="mensaje") private String mensaje;
    @Column(name="estado_envio") private String estadoEnvio;
    @Column(name="leida") private boolean leida;
    @Column(name="fecha_envio") private java.time.LocalDateTime fecha;

    protected NotificacionEntidad() {}
    public NotificacionEntidad(int usuario, int cita, String mensaje, java.time.LocalDateTime fecha) {
        this.usuario = usuario; this.cita = cita; this.mensaje = mensaje; this.fecha = fecha;
        this.tipo = "Citas"; this.canal = "PLATAFORMA"; this.estadoEnvio = "ENVIADA";
    }
}
