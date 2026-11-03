package pe.edu.utp.clinica.ia;

import java.util.List;

public record PrediccionInasistencia(String version, String origen, boolean experimental,
        double probabilidad, String nivel, String accionSugerida, List<Factor> factores,
        List<String> advertencias) {
    public PrediccionInasistencia {
        factores = List.copyOf(factores);
        advertencias = List.copyOf(advertencias);
    }
    public record Factor(String variable, String descripcion, double valor, double referencia,
                         String influencia, double contribucionLogit) {}
}
