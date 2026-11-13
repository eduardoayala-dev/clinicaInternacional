package pe.edu.utp.clinica.config;

import java.util.Map;

public final class EntornoAplicacion {
    private EntornoAplicacion() { }
    public static boolean produccion(Map<String,String> env) {
        return "production".equalsIgnoreCase(env.getOrDefault("APP_ENV","development").trim())
                || env.containsKey("RAILWAY_ENVIRONMENT") || env.containsKey("RAILWAY_ENVIRONMENT_NAME")
                || env.containsKey("RAILWAY_PROJECT_ID");
    }
}
