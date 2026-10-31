package pe.edu.utp.clinica.correo;

import java.sql.SQLException;
import java.time.Clock;

public final class CorreosCitaServicio {
    private final CorreosCitaJdbc almacen;
    private final EmisorCorreo emisor;
    private final Clock reloj;

    public CorreosCitaServicio(CorreosCitaJdbc almacen,EmisorCorreo emisor,Clock reloj) {
        this.almacen=almacen;this.emisor=emisor;this.reloj=reloj;
    }

    public boolean procesarUno()throws SQLException {
        var tarea=almacen.tomar(reloj.instant());
        if(tarea.isEmpty())return false;
        var pendiente=tarea.get();
        var datos=almacen.datos(pendiente.cita(),reloj.instant());
        if(datos.isEmpty()) {
            almacen.terminar(pendiente.cita(),CorreosCitaJdbc.Resultado.OMITIDO,reloj.instant());
            return true;
        }
        Correo mensaje;
        try { mensaje=datos.get().correo(); }
        catch(IllegalArgumentException e) {
            almacen.terminar(pendiente.cita(),CorreosCitaJdbc.Resultado.NO_CONFIRMADO,reloj.instant());
            return true;
        }
        try { emisor.enviar(mensaje); }
        catch(EnvioCorreoException e) {
            if(e.reintentable())almacen.reintentar(pendiente,reloj.instant());
            else almacen.terminar(pendiente.cita(),CorreosCitaJdbc.Resultado.NO_CONFIRMADO,reloj.instant());
            return true;
        }
        almacen.terminar(pendiente.cita(),CorreosCitaJdbc.Resultado.ENVIADO,reloj.instant());
        return true;
    }
}
