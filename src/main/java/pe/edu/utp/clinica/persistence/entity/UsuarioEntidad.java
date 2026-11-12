package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "usuario")
@DynamicInsert
public class UsuarioEntidad {
    @Column(name = "nombres")
    private String nombres;

    @Column(name = "apellidos")
    private String apellidos;

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(name = "activo")
    private boolean activo;

    @Column(name = "rol")
    private String rol;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private java.time.LocalDateTime registro;

    @jakarta.persistence.OneToMany(mappedBy = "usuario")
    private java.util.Set<PacienteEntidad> pacientes;

    @jakarta.persistence.OneToMany(mappedBy = "usuario")
    private java.util.Set<MedicoEntidad> medicos;

    @jakarta.persistence.OneToMany(mappedBy = "usuario")
    private java.util.Set<PersonalEntidad> personal;

    @jakarta.persistence.OneToMany(mappedBy = "usuario")
    private java.util.Set<AdministradorEntidad> administradores;

    @jakarta.persistence.OneToMany(mappedBy = "usuario")
    private java.util.Set<InvitacionPerfilEntidad> invitaciones;

    protected UsuarioEntidad() {}

    public UsuarioEntidad(pe.edu.utp.clinica.model.Usuario usuario) {
        this.nombres = usuario.getNombres();
        this.apellidos = usuario.getApellidos();
        this.email = usuario.getEmail();
        this.passwordHash = usuario.getPasswordHash();
        this.rol = usuario.getRol();
        this.activo = usuario.isActivo();
    }

    public UsuarioEntidad(
            String nombres,
            String apellidos,
            String email,
            String password,
            String rol,
            boolean activo) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.passwordHash = password;
        this.rol = rol;
        this.activo = activo;
    }

    public int id() {
        return id;
    }
}
