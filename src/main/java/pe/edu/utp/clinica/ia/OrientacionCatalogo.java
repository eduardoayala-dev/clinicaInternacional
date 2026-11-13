package pe.edu.utp.clinica.ia;

import pe.edu.utp.clinica.dao.AgendaException;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OrientacionCatalogo {
    public record Motivo(int id, String descripcion) {}
    public record Especialidad(int id, String nombre, List<Motivo> motivos) {
        public Especialidad { motivos=motivos==null?List.of():List.copyOf(motivos); }
        public Especialidad(int id,String nombre){this(id,nombre,List.of());}
    }
    public record Eleccion(int idEspecialidad, String motivo, Integer idMotivo) {
        public Eleccion(int idEspecialidad,String motivo){this(idEspecialidad,motivo,null);}
    }
    public record Resultado(String tipo, Integer idEspecialidad, String especialidad, String explicacion,
                            String proveedor, boolean demostracion, Integer idMotivo, String motivoConsulta) {}
    @FunctionalInterface public interface Clasificador { Eleccion elegir(String relato, List<Especialidad> catalogo); }
    private final Clasificador clasificador;
    private final boolean gemini;
    private static final Map<String,Set<String>> EJEMPLOS = Map.of(
            "caso ficticio: picazon y manchas en la piel", Set.of("dermatologia"),
            "caso ficticio: molestia articular en la rodilla", Set.of("traumatologia","traumatologia y ortopedia"),
            "caso ficticio: acidez y molestia estomacal", Set.of("gastroenterologia"));

    public OrientacionCatalogo(Clasificador clasificador, boolean gemini) {this.clasificador=clasificador;this.gemini=gemini;}

    public Resultado orientar(String relato, boolean ficticio, List<Especialidad> catalogo) {
        if (!ficticio) throw new AgendaException(400,"CASO_FICTICIO_REQUERIDO","Esta demostración solo admite casos ficticios, sin información personal.");
        if (relato==null || relato.strip().length()<12 || relato.length()>1200) throw new AgendaException(400,"RELATO_INVALIDO","Describe un caso ficticio de entre 12 y 1200 caracteres.");
        if (catalogo.isEmpty()) throw noDisponible("No hay especialidades activas. Puedes volver a la búsqueda manual.");
        Set<String> esperado=EJEMPLOS.get(normalizar(relato.strip()));
        if (!gemini && esperado!=null) {
            var coincidencias=catalogo.stream().filter(e->esperado.contains(normalizar(e.nombre().strip()))).toList();
            if(coincidencias.size()==1) return sugerencia(coincidencias.get(0),"EJEMPLO_LOCAL",null);
            throw new AgendaException(409,"EJEMPLO_SIN_ESPECIALIDAD","Este ejemplo no tiene una especialidad activa inequívoca en el catálogo. Elige otra manualmente.");
        }
        if (!gemini) throw noDisponible("Gemini no está conectado. Prueba uno de los ejemplos ficticios o elige la especialidad manualmente.");
        Eleccion eleccion=clasificador.elegir(relato.strip(),List.copyOf(catalogo));
        if (eleccion.idEspecialidad()==0 && "ACLARAR".equals(eleccion.motivo())) {
            if(eleccion.idMotivo()!=null&&eleccion.idMotivo()!=0)throw noDisponible("La orientación recibida no es válida. Puedes elegir una especialidad manualmente.");
            return new Resultado("ACLARAR",null,null,"Describe un poco más las molestias o elige una especialidad manualmente.","GEMINI",true,null,null);
        }
        if (!"CATALOGO".equals(eleccion.motivo())) throw noDisponible("No se obtuvo una sugerencia válida. Puedes elegir la especialidad manualmente.");
        var especialidad=catalogo.stream().filter(e->e.id()==eleccion.idEspecialidad()).findFirst().orElseThrow(()->noDisponible("La especialidad sugerida no está disponible. Vuelve a intentarlo o elige manualmente."));
        return sugerencia(especialidad,"GEMINI",motivoCompatible(especialidad,eleccion.idMotivo()));
    }

    private static Motivo motivoCompatible(Especialidad especialidad,Integer idMotivo) {
        if(idMotivo==null || idMotivo==0)return null;
        return especialidad.motivos().stream().filter(m->m.id()==idMotivo).findFirst()
                .orElseThrow(()->noDisponible("El motivo sugerido no pertenece a la especialidad disponible. Elige el motivo manualmente."));
    }

    private static Resultado sugerencia(Especialidad e,String proveedor,Motivo motivo) {
        return new Resultado("SUGERENCIA",e.id(),e.nombre(),"Esta especialidad permite una primera evaluación de las molestias descritas. Puedes continuar con esta opción o elegir otra.",proveedor,true,motivo==null?null:motivo.id(),motivo==null?null:motivo.descripcion());
    }
    private static String normalizar(String texto) {return Normalizer.normalize(texto,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT);}
    static AgendaException noDisponible(String mensaje) {return new AgendaException(503,"ORIENTACION_NO_DISPONIBLE",mensaje);}
}
