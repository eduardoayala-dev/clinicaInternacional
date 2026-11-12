package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(name = "limite_recuperacion")
public class LimiteRecuperacionEntidad {
    @Id
    @Column(name = "clave_hash")
    private byte[] id;

    @Column(name = "solicitudes")
    private Integer solicitudes;

    @Column(name = "vence_en")
    private LocalDateTime vence;

    protected LimiteRecuperacionEntidad() {}

    public LimiteRecuperacionEntidad(byte[] id, Instant ahora) {
        this.id = id.clone();
        this.solicitudes = 0;
        this.vence = LocalDateTime.ofInstant(ahora, ZoneOffset.UTC);
    }

    public boolean permitir(int maximo, int segundos, Instant ahora) {
        boolean vencido = !ahora.isBefore(vence.toInstant(ZoneOffset.UTC));
        int contador = vencido ? 0 : solicitudes;
        if (contador >= maximo) return false;
        solicitudes = contador + 1;
        if (vencido) vence = LocalDateTime.ofInstant(ahora.plusSeconds(segundos), ZoneOffset.UTC);
        return true;
    }
}
