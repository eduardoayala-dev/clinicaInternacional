package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "disponibilidad")
public class DisponibilidadPropuestaEntidad {
    @Id
    @Column(name = "id_disponibilidad")
    private Integer id;

    @Column(name = "id_propuesta")
    private Integer propuesta;

    protected DisponibilidadPropuestaEntidad() {}
}
