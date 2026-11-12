package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "medico")
public class MedicoEntidad {
    @Column(name = "cmp")
    private String cmp;

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "id_medico")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private UsuarioEntidad usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_especialidad")
    private EspecialidadEntidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede")
    private SedeEntidad sede;

    @Column(name = "estado")
    private String estado;

    protected MedicoEntidad() {}

    public MedicoEntidad(
            UsuarioEntidad usuario,
            String cmp,
            EspecialidadEntidad especialidad,
            SedeEntidad sede) {
        this.usuario = usuario;
        this.cmp = cmp;
        this.especialidad = especialidad;
        this.sede = sede;
        this.estado = "ACTIVO";
    }
}
