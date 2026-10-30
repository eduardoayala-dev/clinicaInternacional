package pe.edu.utp.clinica.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public final class GmailCorreo implements EmisorCorreo {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final URI TOKEN = URI.create("https://oauth2.googleapis.com/token");
    private static final URI ENVIAR = URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send");
    private final String cliente;
    private final String secreto;
    private final String renovacion;
    private final String remitente;
    private final Transporte transporte;
    private final Clock reloj;
    private String acceso;
    private Instant vence = Instant.EPOCH;

    public GmailCorreo(String cliente, String secreto, String renovacion, String remitente) {
        this(cliente, secreto, renovacion, remitente, transporteHttp(), Clock.systemUTC());
    }

    GmailCorreo(String cliente, String secreto, String renovacion, String remitente,
                Transporte transporte, Clock reloj) {
        this.cliente = validarSecreto(cliente);
        this.secreto = validarSecreto(secreto);
        this.renovacion = validarSecreto(renovacion);
        this.remitente = Correo.validarDireccion(remitente);
        this.transporte = transporte;
        this.reloj = reloj;
    }

    private static String validarSecreto(String valor) {
        if (valor == null || valor.isBlank() || valor.length() > 8192
                || valor.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalArgumentException("Faltan credenciales válidas de Gmail API.");
        }
        return valor;
    }

    private static Transporte transporteHttp() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return peticion -> {
            var respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Respuesta(respuesta.statusCode(), respuesta.body());
        };
    }

    private synchronized String obtenerAcceso() throws IOException, InterruptedException, EnvioCorreoException {
        if (acceso != null && reloj.instant().isBefore(vence)) return acceso;
        String formulario = "grant_type=refresh_token&client_id=" + codificar(cliente)
                + "&client_secret=" + codificar(secreto) + "&refresh_token=" + codificar(renovacion);
        var peticion = HttpRequest.newBuilder(TOKEN).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario)).build();
        Respuesta respuesta = transporte.enviar(peticion);
        comprobarEstado(respuesta.estado(), "No se pudo renovar la autorización de Gmail.");
        var datos = JSON.readTree(respuesta.cuerpo());
        String nuevo = datos.path("access_token").asText();
        long segundos = datos.path("expires_in").asLong();
        if (!"Bearer".equalsIgnoreCase(datos.path("token_type").asText())
                || nuevo.isBlank() || nuevo.length() > 8192 || segundos < 60 || segundos > 86_400
                || nuevo.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new EnvioCorreoException("Google devolvió una autorización incompleta.", false);
        }
        acceso = nuevo;
        vence = reloj.instant().plusSeconds(segundos - 30);
        return acceso;
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    static String mime(Correo correo, String remitente) {
        String contenido = Base64.getMimeEncoder(76, new byte[]{13, 10})
                .encodeToString(correo.texto().getBytes(StandardCharsets.UTF_8));
        return "From: " + cabecera("Clínica Internacional") + " <" + remitente + ">\r\nTo: " + correo.destinatario()
                + "\r\nSubject: " + cabecera(correo.asunto()) + "\r\nMIME-Version: 1.0"
                + "\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n" + contenido;
    }

    private static String cabecera(String texto) {
        var partes = new java.util.ArrayList<String>();
        var parte = new StringBuilder();
        int bytes = 0;
        for (int posicion = 0; posicion < texto.length();) {
            int punto = texto.codePointAt(posicion);
            String caracter = new String(Character.toChars(punto));
            int longitud = caracter.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + longitud > 30) {
                partes.add(palabraCodificada(parte.toString()));
                parte.setLength(0);
                bytes = 0;
            }
            parte.append(caracter);
            bytes += longitud;
            posicion += Character.charCount(punto);
        }
        partes.add(palabraCodificada(parte.toString()));
        return String.join("\r\n ", partes);
    }

    private static String palabraCodificada(String texto) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    @Override public String enviar(Correo correo) throws EnvioCorreoException {
        try {
            String token = obtenerAcceso();
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mime(correo, remitente).getBytes(StandardCharsets.UTF_8));
            var peticion = HttpRequest.newBuilder(ENVIAR).timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("raw", raw)))).build();
            Respuesta respuesta = transporte.enviar(peticion);
            if (respuesta.estado() == 401) invalidarAcceso(token);
            comprobarEstado(respuesta.estado(), "Google rechazó el envío del correo.");
            String id = JSON.readTree(respuesta.cuerpo()).path("id").asText();
            if (id.isBlank() || id.length() > 128) {
                throw new EnvioCorreoException("Google no confirmó la aceptación del correo.", false);
            }
            return id;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnvioCorreoException("El envío fue interrumpido; su resultado es desconocido.", false);
        } catch (IOException e) {
            throw new EnvioCorreoException("No se pudo confirmar el envío; comprobar en Gmail.", false);
        }
    }

    private synchronized void invalidarAcceso(String anterior) {
        if (anterior.equals(acceso)) { acceso = null; vence = Instant.EPOCH; }
    }

    private static void comprobarEstado(int estado, String mensaje) throws EnvioCorreoException {
        if (estado != 200) throw new EnvioCorreoException(mensaje + " HTTP " + estado,
                estado == 429 || estado >= 500);
    }

    @FunctionalInterface interface Transporte {
        Respuesta enviar(HttpRequest peticion) throws IOException, InterruptedException;
    }

    record Respuesta(int estado, String cuerpo) {}
}
