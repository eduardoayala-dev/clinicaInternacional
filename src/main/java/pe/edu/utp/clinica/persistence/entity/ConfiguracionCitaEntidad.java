package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="configuracion_ofertas")
@org.hibernate.annotations.Immutable
public class ConfiguracionCitaEntidad {
    @Id @Column(name="id_configuracion") private Integer id;
    @Column(name="confirmacion_horas") private Integer confirmacionHoras;
    protected ConfiguracionCitaEntidad() {}
}
