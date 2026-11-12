package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name="paciente")
@Immutable
public class PacienteEntidad {
    @Id @Column(name="id_paciente") private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_usuario") private UsuarioEntidad usuario;

    protected PacienteEntidad() {}
}
