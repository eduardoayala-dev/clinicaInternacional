package pe.edu.utp.clinica.catalogo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class CodigoSede {
    private static final Set<String> OMITIR=Set.of("SEDE","CLINICA","DE","DEL","Y");
    private CodigoSede(){}
    public static String base(String nombre){
        String limpio=Normalizer.normalize(nombre==null?"":nombre,Normalizer.Form.NFD).replaceAll("\\p{M}","").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]"," ");
        var iniciales=new StringBuilder();
        for(String palabra:limpio.split(" +"))if(!palabra.isBlank()&&!OMITIR.contains(palabra)&&iniciales.length()<10)iniciales.append(palabra.charAt(0));
        if(iniciales.isEmpty())throw new IllegalArgumentException("El nombre debe contener letras o números para generar el código.");
        return "SED-"+iniciales;
    }
}
