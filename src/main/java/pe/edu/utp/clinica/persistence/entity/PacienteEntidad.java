package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "paciente")
public class PacienteEntidad {
    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private UsuarioEntidad usuario;

    @Column(name = "dni")
    private String dni;

    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate nacimiento;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "canal_aviso")
    private String canal;

    @Column(name = "sede_preferida")
    private String sedePreferida;

    @Column(name = "alergias")
    private String alergias;

    @Column(name = "tipo_cobertura")
    private String cobertura;

    protected PacienteEntidad() {}

    public PacienteEntidad(UsuarioEntidad usuario, pe.edu.utp.clinica.model.Paciente paciente) {
        this.usuario = usuario;
        this.dni = paciente.getDni();
        this.nacimiento = paciente.getFechaNacimiento();
        this.telefono = paciente.getTelefono();
        this.direccion = paciente.getDireccion();
        this.canal = paciente.getCanalAviso();
        this.sedePreferida = paciente.getSedePreferida();
        this.alergias = paciente.getAlergias();
        this.cobertura = paciente.getTipoCobertura();
    }

    public int id() {
        return id;
    }
}
