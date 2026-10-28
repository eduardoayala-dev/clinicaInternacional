package pe.edu.utp.clinica.util;

public final class Html {
    private Html(){}
    public static String texto(Object valor){
        return java.util.Objects.toString(valor,"").replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}
