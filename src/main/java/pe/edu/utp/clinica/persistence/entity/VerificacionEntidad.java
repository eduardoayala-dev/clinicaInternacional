package pe.edu.utp.clinica.persistence.entity;

import jakarta.persistence.*;

import pe.edu.utp.clinica.recuperacion.VerificacionJdbc.Etapa;
import pe.edu.utp.clinica.recuperacion.VerificacionJdbc.Proposito;

import java.time.*;

@Entity
@Table(name = "verificacion_cuenta")
public class VerificacionEntidad {
    @Id
    @Column(name = "id_verificacion")
    private String id;

    @Column(name = "id_usuario")
    private Integer usuario;

    @Column(name = "proposito")
    private String proposito;

    @Column(name = "id_sesion")
    private String sesion;

    @Column(name = "vinculo_hash")
    private byte[] vinculo;

    @Column(name = "credencial_hash")
    private byte[] credencial;

    @Column(name = "correo_hash")
    private byte[] correo;

    @Column(name = "codigo_hash")
    private byte[] codigo;

    @Column(name = "etapa")
    private String etapa;

    @Column(name = "intentos")
    private Integer intentos;

    @Column(name = "correo_nuevo")
    private String nuevo;

    @Column(name = "creada_en")
    private LocalDateTime creada;

    @Column(name = "vence_en")
    private LocalDateTime vence;

    protected VerificacionEntidad() {}

    public VerificacionEntidad(
            String id,
            int usuario,
            Proposito proposito,
            String sesion,
            byte[] vinculo,
            byte[] credencial,
            byte[] correo,
            byte[] codigo,
            Instant ahora) {
        this.id = id;
        this.usuario = usuario;
        this.proposito = proposito.name();
        this.sesion = sesion;
        this.vinculo = vinculo.clone();
        this.credencial = credencial.clone();
        this.correo = correo.clone();
        this.codigo = codigo.clone();
        this.etapa = Etapa.CODIGO_ACTUAL.name();
        this.intentos = 0;
        this.creada = utc(ahora);
        this.vence = utc(ahora.plusSeconds(600));
    }

    private static LocalDateTime utc(Instant fecha) {
        return LocalDateTime.ofInstant(fecha, ZoneOffset.UTC);
    }

    private static byte[] copia(byte[] valor) {
        return valor == null ? null : valor.clone();
    }

    public String id() {
        return id;
    }

    public int usuario() {
        return usuario;
    }

    public Proposito proposito() {
        return Proposito.valueOf(proposito);
    }

    public String sesion() {
        return sesion;
    }

    public byte[] vinculo() {
        return copia(vinculo);
    }

    public byte[] credencial() {
        return copia(credencial);
    }

    public byte[] correo() {
        return copia(correo);
    }

    public byte[] codigo() {
        return copia(codigo);
    }

    public Etapa etapa() {
        return Etapa.valueOf(etapa);
    }

    public int intentos() {
        return intentos;
    }

    public String nuevo() {
        return nuevo;
    }

    public Instant creada() {
        return creada.toInstant(ZoneOffset.UTC);
    }

    public Instant vence() {
        return vence.toInstant(ZoneOffset.UTC);
    }

    public void autorizar(Instant limite) {
        etapa = Etapa.AUTORIZADO.name();
        codigo = null;
        intentos = 0;
        vence = utc(limite);
    }

    public void prepararCorreo(String correo, byte[] codigo, Instant limite) {
        nuevo = correo;
        this.codigo = codigo.clone();
        etapa = Etapa.CODIGO_NUEVO.name();
        intentos = 0;
        vence = utc(limite);
    }

    public void registrarIntento(boolean coincide) {
        intentos++;
        if (!coincide && intentos >= 5) {
            etapa = Etapa.BLOQUEADO.name();
            codigo = null;
        }
    }

    public void finalizar() {
        etapa = Etapa.FINALIZADO.name();
        codigo = null;
    }

    @Override
    public String toString() {
        return "Verificacion[privada]";
    }
}
