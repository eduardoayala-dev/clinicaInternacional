package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_ofertas")
public class ConfiguracionOfertasEntidad {
    @Id
    @Column(name = "id_configuracion")
    private Integer id;

    @Column(name = "minutos_respuesta")
    private Integer minutos;

    @Column(name = "anticipacion_minima")
    private Integer anticipacion;

    @Column(name = "version")
    private Long version;

    @Column(name = "confirmacion_horas")
    private Integer confirmacion;

    @Column(name = "tolerancia_llegada")
    private Integer tolerancia;

    @Column(name = "recordatorio_horas")
    private Integer recordatorio;

    @Column(name = "intentos_confirmacion")
    private Integer intentos;

    @Column(name = "actualizado_por")
    private Integer actor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizada;

    protected ConfiguracionOfertasEntidad() {}
}
