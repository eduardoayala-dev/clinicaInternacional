package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "personal_administrativo")
public class PersonalEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personal")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private UsuarioEntidad usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede")
    private SedeEntidad sede;

    @Column(name = "codigo_empleado")
    private String codigo;

    @Column(name = "cargo")
    private String cargo;

    protected PersonalEntidad() {}

    public PersonalEntidad(UsuarioEntidad usuario, SedeEntidad sede, String codigo, String cargo) {
        this.usuario = usuario;
        this.sede = sede;
        this.codigo = codigo;
        this.cargo = cargo;
    }
}
