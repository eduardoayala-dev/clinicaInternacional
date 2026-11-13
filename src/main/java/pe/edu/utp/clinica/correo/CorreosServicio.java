package pe.edu.utp.clinica.correo;

import java.sql.SQLException;
import java.time.Clock;

public final class CorreosServicio {
    private final CorreosPendientesJdbc almacen;
    private final EmisorCorreo emisor;
    private final Clock reloj;

    public CorreosServicio(CorreosPendientesJdbc almacen,EmisorCorreo emisor,Clock reloj) {
        this.almacen=almacen;this.emisor=emisor;this.reloj=reloj;
    }

    public boolean procesarUno()throws SQLException {
        var tarea=almacen.tomar(reloj.instant());
        if(tarea.isEmpty())return false;
        var pendiente=tarea.get();
        Correo mensaje;
        try {
            var datos=almacen.mensaje(pendiente.id(),reloj.instant());
            if(datos.isEmpty()) {
                almacen.terminar(pendiente.id(),CorreosPendientesJdbc.Resultado.OMITIDO,reloj.instant());
                return true;
            }
            mensaje=datos.get();
        }
        catch(IllegalArgumentException e) {
            almacen.terminar(pendiente.id(),CorreosPendientesJdbc.Resultado.NO_CONFIRMADO,reloj.instant());
            return true;
        }
        try { emisor.enviar(mensaje); }
        catch(EnvioCorreoException e) {
            if(e.reintentable())almacen.reintentar(pendiente,reloj.instant());
            else almacen.terminar(pendiente.id(),CorreosPendientesJdbc.Resultado.NO_CONFIRMADO,reloj.instant());
            return true;
        }
        almacen.terminar(pendiente.id(),CorreosPendientesJdbc.Resultado.ENVIADO,reloj.instant());
        return true;
    }
}
