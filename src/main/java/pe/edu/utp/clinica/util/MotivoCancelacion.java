package pe.edu.utp.clinica.util;

import pe.edu.utp.clinica.dao.AgendaException;

public final class MotivoCancelacion {
    private MotivoCancelacion() {}
    public static String validar(String motivo) {
        String texto=motivo==null?"":motivo.strip();
        if(texto.length()<5 || texto.length()>160 || texto.chars().anyMatch(Character::isISOControl)
                || texto.indexOf('<')>=0 || texto.indexOf('>')>=0) {
            throw new AgendaException(400,"MOTIVO_CANCELACION_REQUERIDO","Indica un motivo de cancelación de 5 a 160 caracteres, sin etiquetas ni saltos de línea.");
        }
        return texto;
    }
}
