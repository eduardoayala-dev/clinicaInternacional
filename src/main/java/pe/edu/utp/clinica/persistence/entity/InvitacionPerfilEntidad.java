package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "invitacion_personal")
@org.hibernate.annotations.DynamicInsert
public class InvitacionPerfilEntidad {
    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private UsuarioEntidad usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede_destino")
    private SedeEntidad sede;

    @Column(name = "token_hash")
    private byte[] token;

    @Column(name = "creada_por")
    private Integer actor;

    @Column(name = "creada_en")
    private java.time.LocalDateTime creada;

    @Column(name = "vence_en")
    private java.time.LocalDateTime vence;

    @Column(name = "utilizada_en")
    private java.time.LocalDateTime utilizada;

    @Column(name = "revocada_en")
    private java.time.LocalDateTime revocada;

    @Column(name = "estado_envio")
    private String envio;

    protected InvitacionPerfilEntidad() {}

    public InvitacionPerfilEntidad(
            int usuario, int actor, byte[] token, java.time.LocalDateTime ahora) {
        this.id = usuario;
        this.actor = actor;
        this.token = token.clone();
        this.creada = ahora;
        this.vence = ahora.plusMinutes(30);
        this.envio = "PENDIENTE";
    }
}
