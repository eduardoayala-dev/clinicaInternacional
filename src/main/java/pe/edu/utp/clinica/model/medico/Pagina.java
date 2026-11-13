package pe.edu.utp.clinica.model.medico;

import java.util.List;

public record Pagina<T>(List<T> items, int pagina, int tamano, boolean tieneSiguiente) {
    public Pagina { items = List.copyOf(items); }
}
