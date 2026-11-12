package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class ColaCorreoEntidad {
    @Column(name = "estado")
    private String estado = "PENDIENTE";

    @Column(name = "intentos")
    private Integer intentos = 0;

    @Column(name = "disponible_en")
    private LocalDateTime disponible;

    @Column(name = "iniciada_en")
    private LocalDateTime iniciada;

    @Column(name = "finalizada_en")
    private LocalDateTime finalizada;

    protected ColaCorreoEntidad() {}

    protected ColaCorreoEntidad(LocalDateTime disponible) {
        this.disponible = disponible;
    }
}
