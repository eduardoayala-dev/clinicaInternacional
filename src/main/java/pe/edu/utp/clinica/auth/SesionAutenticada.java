package pe.edu.utp.clinica.auth;

import pe.edu.utp.clinica.model.Usuario;
import java.time.Instant;

public record SesionAutenticada(String id, Usuario usuario, String csrf, Instant vence) { }
