package pe.edu.utp.clinica.persistence;

import jakarta.persistence.Tuple;

import java.util.*;

public final class FilaConsulta {
    private final Map<String, Object> valores;

    public FilaConsulta(Tuple fila) {
        var copia = new LinkedHashMap<String, Object>();
        for (var elemento : fila.getElements()) {
            String alias =
                    Objects.requireNonNull(elemento.getAlias(), "La proyección requiere un alias.");
            if (copia.containsKey(alias))
                throw new IllegalArgumentException("Alias repetido en la proyección.");
            copia.put(alias, fila.get(elemento));
        }
        valores = Collections.unmodifiableMap(copia);
    }

    public Object valor(String campo) {
        if (!valores.containsKey(campo))
            throw new IllegalArgumentException("Campo ausente en la proyección: " + campo);
        return valores.get(campo);
    }

    public <T> T valor(String campo, Class<T> tipo) {
        return tipo.cast(valor(campo));
    }

    public String texto(String campo) {
        return valor(campo, String.class);
    }

    public int entero(String campo) {
        Object valor = valor(campo);
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    public boolean booleano(String campo) {
        Object valor = valor(campo);
        return valor instanceof Boolean bandera
                ? bandera
                : valor instanceof Number numero && numero.intValue() != 0;
    }

    @Override
    public String toString() {
        return "FilaConsulta[privada]";
    }
}
