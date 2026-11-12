package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_autenticacion")
public class SesionEntidad {
    @Id
    @Column(name = "id_sesion")
    private String id;

    @Column(name = "id_usuario")
    private Integer usuario;

    @Column(name = "refresh_hash")
    private byte[] refresh;

    @Column(name = "csrf_token")
    private String csrf;

    @Column(name = "rol_inicial")
    private String rol;

    @Column(name = "credencial_hash")
    private byte[] credencial;

    @Column(name = "creada_en")
    private LocalDateTime creada;

    @Column(name = "ultima_actividad_en")
    private LocalDateTime actividad;

    @Column(name = "vence_en")
    private LocalDateTime vence;

    @Column(name = "revocada_en")
    private LocalDateTime revocada;

    protected SesionEntidad() {}

    public SesionEntidad(
            String id,
            int usuario,
            byte[] refresh,
            String csrf,
            String rol,
            byte[] credencial,
            LocalDateTime ahora,
            LocalDateTime vence) {
        this.id = id;
        this.usuario = usuario;
        this.refresh = refresh.clone();
        this.csrf = csrf;
        this.rol = rol;
        this.credencial = credencial.clone();
        this.creada = ahora;
        this.actividad = ahora;
        this.vence = vence;
    }

    public String id() {
        return id;
    }

    public int usuario() {
        return usuario;
    }

    public String csrf() {
        return csrf;
    }

    public String rol() {
        return rol;
    }

    public byte[] credencial() {
        return credencial.clone();
    }

    public LocalDateTime actividad() {
        return actividad;
    }

    public LocalDateTime vence() {
        return vence;
    }

    public boolean revocada() {
        return revocada != null;
    }

    public void registrarActividad(LocalDateTime ahora) {
        if (revocada == null) actividad = ahora;
    }

    public void revocar(LocalDateTime ahora) {
        if (revocada == null) revocada = ahora;
    }
}
