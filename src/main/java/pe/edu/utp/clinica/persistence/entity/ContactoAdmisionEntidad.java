package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacto_admision_cita")
public class ContactoAdmisionEntidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contacto")
    private Long id;

    @Column(name = "id_cita")
    private Integer cita;

    @Column(name = "id_usuario_personal")
    private Integer usuario;

    @Column(name = "fecha_contacto")
    private LocalDateTime fecha;

    @Column(name = "canal")
    private String canal;

    @Column(name = "resultado")
    private String resultado;

    protected ContactoAdmisionEntidad() {}

    public ContactoAdmisionEntidad(
            int cita, int usuario, LocalDateTime fecha, String canal, String resultado) {
        this.cita = cita;
        this.usuario = usuario;
        this.fecha = fecha;
        this.canal = canal;
        this.resultado = resultado;
    }
}
