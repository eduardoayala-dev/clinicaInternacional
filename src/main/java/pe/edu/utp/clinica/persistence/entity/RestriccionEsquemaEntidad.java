package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "TABLE_CONSTRAINTS", catalog = "information_schema")
@org.hibernate.annotations.Immutable
public class RestriccionEsquemaEntidad {
    @Id
    @Column(name = "CONSTRAINT_NAME")
    private String nombre;

    @Column(name = "TABLE_SCHEMA")
    private String esquema;

    @Column(name = "TABLE_NAME")
    private String tabla;

    @Column(name = "CONSTRAINT_TYPE")
    private String tipo;

    protected RestriccionEsquemaEntidad() {}
}
