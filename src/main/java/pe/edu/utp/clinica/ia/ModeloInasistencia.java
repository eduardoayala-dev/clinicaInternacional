package pe.edu.utp.clinica.ia;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModeloInasistencia {
    private static final int MAX_BYTES_MODELO = 65536;
    private static final JsonMapper JSON = JsonMapper.builder(JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(8)
                            .maxStringLength(512).maxNumberLength(64).build())
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();
    private static final List<String> NOMBRES = List.of("inasistencias_previas", "atenciones_previas", "anticipacion_dias", "confirmada_por_paciente");
    private static final List<String> DESCRIPCIONES = List.of("Inasistencias anteriores", "Atenciones anteriores", "Anticipación de la reserva", "Confirmación expresa del paciente");
    public record Variable(String nombre, double media, double escala, double coeficiente) {}
    public record Artefacto(int esquema, String version, String objetivo, String origen, boolean experimental,
                           double intercepto, double umbralMedio, double umbralAlto, List<Variable> variables) {
        public Artefacto { variables = variables == null ? List.of() : List.copyOf(variables); }
    }
    private final Artefacto artefacto;

    public static ModeloInasistencia demostracion() throws IOException {
        try (InputStream input = ModeloInasistencia.class.getResourceAsStream("/ia/riesgo-inasistencia-demo.json")) {
            if (input == null) throw new IOException("Modelo demostrativo no disponible.");
            return cargar(input);
        }
    }

    public static ModeloInasistencia cargar(InputStream input) throws IOException {
        if (input == null) throw invalido();
        byte[] contenido = input.readNBytes(MAX_BYTES_MODELO + 1);
        if (contenido.length > MAX_BYTES_MODELO) throw invalido();
        Artefacto modelo = JSON.readValue(contenido, Artefacto.class);
        validar(modelo);
        return new ModeloInasistencia(modelo);
    }

    private ModeloInasistencia(Artefacto artefacto) { this.artefacto = artefacto; }
    public Artefacto informacion() { return artefacto; }

    public PrediccionInasistencia predecir(EntradaInasistencia entrada) {
        double logit = artefacto.intercepto();
        var factores = new ArrayList<PrediccionInasistencia.Factor>();
        double[] valores = entrada.valores();
        for (int i=0; i<valores.length; i++) {
            Variable v = artefacto.variables().get(i);
            double contribucion = (valores[i]-v.media()) / v.escala() * v.coeficiente();
            logit += contribucion;
            String influencia = influencia(contribucion);
            String descripcion = i==3 && !entrada.confirmadaPorPaciente() ? "Sin confirmación expresa del paciente" : DESCRIPCIONES.get(i);
            factores.add(new PrediccionInasistencia.Factor(v.nombre(), descripcion, valores[i], v.media(), influencia, contribucion));
        }
        double probabilidad = logit >= 0 ? 1.0/(1.0+Math.exp(-logit)) : Math.exp(logit)/(1.0+Math.exp(logit));
        String nivel = nivel(probabilidad);
        factores.sort(Comparator.comparingDouble((PrediccionInasistencia.Factor f) -> Math.abs(f.contribucionLogit())).reversed());
        return new PrediccionInasistencia(artefacto.version(), artefacto.origen(), artefacto.experimental(),
                probabilidad, nivel, accion(nivel), factores, advertencias(entrada));
    }

    private String nivel(double probabilidad) {
        if (probabilidad >= artefacto.umbralAlto()) return "ALTO";
        if (probabilidad >= artefacto.umbralMedio()) return "MEDIO";
        return "BAJO";
    }

    private static String influencia(double contribucion) {
        if (contribucion > 0) return "AUMENTA";
        if (contribucion < 0) return "REDUCE";
        return "NEUTRA";
    }

    private static List<String> advertencias(EntradaInasistencia entrada) {
        var avisos = new ArrayList<String>();
        avisos.add("Modelo entrenado con datos sintéticos; no demuestra precisión para pacientes reales.");
        avisos.add("Los factores describen asociaciones del modelo, no causas ni decisiones clínicas.");
        if (entrada.inasistenciasPrevias()+entrada.atencionesPrevias()==0) avisos.add("Sin historial previo: la estimación depende de los demás datos y del patrón sintético aprendido.");
        if (entrada.inasistenciasPrevias()+entrada.atencionesPrevias()>11 || entrada.anticipacionDias()>45) avisos.add("Entrada fuera del rango observado en el entrenamiento sintético; la extrapolación no está validada.");
        return avisos;
    }

    private static String accion(String nivel) {
        return switch (nivel) {
            case "ALTO" -> "Sugerir revisión prioritaria de confirmación y contacto por admisión. No cancelar ni reasignar por este resultado.";
            case "MEDIO" -> "Sugerir revisar la confirmación y los avisos pendientes.";
            default -> "Mantener el seguimiento habitual de la cita.";
        };
    }

    private static void validar(Artefacto m) throws IOException {
        if (m == null || m.esquema()!=1 || !"NO_ASISTIO".equals(m.objetivo()) || !"SINTETICO".equals(m.origen()) || !m.experimental()) throw invalido();
        if (m.version()==null || !m.version().matches("[a-z0-9-]{1,80}") || m.variables().size()!=NOMBRES.size()) throw invalido();
        if (!Double.isFinite(m.intercepto()) || Math.abs(m.intercepto())>10000 || !(m.umbralMedio()>0 && m.umbralMedio()<m.umbralAlto() && m.umbralAlto()<1)) throw invalido();
        for (int i=0;i<NOMBRES.size();i++) {
            validarVariable(m.variables().get(i),NOMBRES.get(i));
        }
    }
    private static void validarVariable(Variable v,String nombre) throws IOException {
        if (v == null || !nombre.equals(v.nombre()) || !Double.isFinite(v.media()) || Math.abs(v.media())>1000000) throw invalido();
        if (!Double.isFinite(v.escala()) || v.escala()<1e-9 || v.escala()>1e9) throw invalido();
        if (!Double.isFinite(v.coeficiente()) || Math.abs(v.coeficiente())>10000) throw invalido();
    }
    private static IOException invalido() { return new IOException("El modelo de inasistencia no tiene un formato compatible."); }
}
