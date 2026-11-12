package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name="usuario")
@Immutable
public class UsuarioEntidad {
    @Column(name="nombres") private String nombres;
    @Column(name="apellidos") private String apellidos;
    @Id @Column(name="id_usuario") private Integer id;
    @Column(name="activo") private boolean activo;
    @Column(name="rol") private String rol;

    protected UsuarioEntidad() {}
}
