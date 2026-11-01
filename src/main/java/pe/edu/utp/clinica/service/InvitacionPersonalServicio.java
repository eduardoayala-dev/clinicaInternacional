package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.auth.SecretosSesion;
import pe.edu.utp.clinica.config.EntornoAplicacion;
import pe.edu.utp.clinica.correo.*;
import pe.edu.utp.clinica.dao.InvitacionPersonalDAO;
import java.net.URI;
import java.util.Map;

public class InvitacionPersonalServicio {
    private final InvitacionPersonalDAO almacen;
    private final EmisorCorreo emisor;
    private final String base;
    public record Resultado(int usuario,boolean enviado) {}
    public InvitacionPersonalServicio(InvitacionPersonalDAO almacen,EmisorCorreo emisor,String base) {
        this.almacen=almacen;this.emisor=emisor;this.base=base;
    }
    public static InvitacionPersonalServicio crear(Map<String,String> entorno) {
        return new InvitacionPersonalServicio(new InvitacionPersonalDAO(),ConfiguracionCorreo.crear(entorno),basePublica(entorno));
    }
    public Resultado invitar(int actor,InvitacionPersonalDAO.Solicitud solicitud) {
        String secreto=SecretosSesion.nuevo();byte[] hash=SecretosSesion.hash(secreto);
        var entrega=almacen.crear(actor,solicitud,hash);
        return enviar(entrega,secreto,hash);
    }
    public Resultado reenviar(int actor,int usuario) {
        String secreto=SecretosSesion.nuevo();byte[] hash=SecretosSesion.hash(secreto);
        return enviar(almacen.reenviar(actor,usuario,hash),secreto,hash);
    }
    private Resultado enviar(InvitacionPersonalDAO.Entrega entrega,String secreto,byte[] hash) {
        boolean enviado;
        try { emisor.enviar(PlantillasCorreo.invitacionPersonal(entrega.correo(),base+"/activar-personal#"+secreto)); enviado=true; }
        catch(EnvioCorreoException e) { enviado=false; }
        almacen.registrarEnvio(entrega.usuario(),hash,enviado);
        return new Resultado(entrega.usuario(),enviado);
    }
    public static String basePublica(Map<String,String> entorno) {
        String valor=entorno.get("APP_PUBLIC_URL");
        if(valor==null && entorno.get("RAILWAY_PUBLIC_DOMAIN")!=null)valor="https://"+entorno.get("RAILWAY_PUBLIC_DOMAIN");
        if(valor==null && !EntornoAplicacion.produccion(entorno))valor="http://localhost:8080";
        try {
            URI uri=URI.create(valor==null?"":valor);
            boolean local="http".equals(uri.getScheme()) && !EntornoAplicacion.produccion(entorno)
                    && ("localhost".equals(uri.getHost())||"127.0.0.1".equals(uri.getHost()));
            if(uri.getHost()==null || (!"https".equals(uri.getScheme())&&!local) || uri.getUserInfo()!=null || uri.getRawQuery()!=null || uri.getRawFragment()!=null)throw new IllegalArgumentException();
            return uri.toString().replaceAll("/+$","");
        } catch(IllegalArgumentException e){throw new IllegalArgumentException("Configura APP_PUBLIC_URL con la dirección pública HTTPS del sistema.");}
    }
}
