package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name="sede")
@Immutable
public class SedeEntidad {
    @Column(name="nombre") private String nombre;
    @Column(name="direccion") private String direccion;
    @Id @Column(name="id_sede") private Integer id;
    @Column(name="estado") private String estado;

    protected SedeEntidad() {}
}
