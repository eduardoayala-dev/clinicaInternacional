package pe.edu.utp.clinica.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import pe.edu.utp.clinica.correo.*;
import java.time.Clock;

@WebListener
public final class CorreosCitaContextListener implements ServletContextListener {
    public static final String DESPERTAR=CorreosCitaContextListener.class.getName()+".despertar";
    private CorreosDespachador despachador;
    private CorreosDespachador ofertas;
    private CorreosDespachador cambios;
    private RecordatoriosProgramador recordatorios;

    @Override public void contextInitialized(ServletContextEvent evento) {
        if(!Boolean.parseBoolean(System.getenv().getOrDefault("APPOINTMENT_EMAIL_ENABLED","false")))return;
        if(!"gmail".equals(System.getenv("MAIL_MODE")))throw new IllegalArgumentException("Los avisos por correo requieren MAIL_MODE=gmail.");
        var emisor=ConfiguracionCorreo.crear(System.getenv());
        var reloj=Clock.systemUTC();
        despachador=new CorreosDespachador(new CorreosCitaJdbc(),emisor,reloj);
        cambios=new CorreosDespachador(new CorreosCambiosJdbc(),emisor,reloj);
        ofertas=new CorreosDespachador(new CorreosOfertaJdbc(),emisor,reloj);
        if(Boolean.parseBoolean(System.getenv().getOrDefault("RECORDATORIOS_EMAIL_ENABLED","false"))){
            var almacen=new CorreosRecordatorioJdbc(pe.edu.utp.clinica.service.InvitacionPersonalServicio.basePublica(System.getenv()));
            java.util.function.IntSupplier horas=()->new pe.edu.utp.clinica.dao.ConfiguracionOfertasDAO().leer().recordatorioHoras();
            recordatorios=new RecordatoriosProgramador(almacen,new AvisosVencimientoJdbc(),new CorreosDespachador(almacen,emisor,reloj),reloj,horas,cambios::despertar);
            recordatorios.iniciar();
        }
        evento.getServletContext().setAttribute(DESPERTAR,(Runnable)this::despertar);
        despertar();
    }

    private void despertar() { despachador.despertar(); ofertas.despertar(); cambios.despertar(); }

    @Override public void contextDestroyed(ServletContextEvent evento) {
        evento.getServletContext().removeAttribute(DESPERTAR);
        if(despachador!=null)despachador.close();
        if(ofertas!=null)ofertas.close();
        if(cambios!=null)cambios.close();
        if(recordatorios!=null)recordatorios.close();
    }
}
