package pe.edu.utp.clinica.ia;

import pe.edu.utp.clinica.dao.SeguimientoIaDAO;

public record SeguimientoIa(SeguimientoIaDAO.Instantanea instantanea, PrediccionInasistencia prediccion,
                           String nivelSeguimiento, String criterioSeguimiento) {
    public static SeguimientoIa evaluar(SeguimientoIaDAO.Instantanea datos, ModeloInasistencia modelo) {
        var prediccion=modelo.predecir(datos.entrada());
        String nivel=prediccion.nivel();
        if(datos.plazoVencido()) nivel=switch(nivel) { case "BAJO" -> "MEDIO"; default -> "ALTO"; };
        return new SeguimientoIa(datos,prediccion,nivel,datos.plazoVencido()
                ? "Plazo de confirmación vencido: regla de seguimiento que eleva un nivel, sin modificar la probabilidad del modelo."
                : "Nivel del modelo experimental. La confirmación y los contactos se gestionan por admisión.");
    }
}
