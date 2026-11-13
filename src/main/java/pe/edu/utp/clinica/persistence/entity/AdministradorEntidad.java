package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "administrador")
public class AdministradorEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_administrador")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private UsuarioEntidad usuario;

    @Column(name = "estado")
    private String estado;

    protected AdministradorEntidad() {}

    public AdministradorEntidad(UsuarioEntidad usuario) {
        this.usuario = usuario;
        this.estado = "ACTIVO";
    }
}
