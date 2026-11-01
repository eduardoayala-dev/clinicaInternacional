package pe.edu.utp.clinica.util;

public final class Html {
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON=new com.fasterxml.jackson.databind.ObjectMapper();
    private Html(){}
    public static String texto(Object valor){
        return java.util.Objects.toString(valor,"").replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
    public static String json(Object valor) {
        try {
            return JSON.writeValueAsString(valor).replace("<","\\u003c").replace(">","\\u003e").replace("&","\\u0026")
                    .replace("\u2028","\\u2028").replace("\u2029","\\u2029");
        } catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("No se pudieron preparar los datos de la vista.");}
    }
}
