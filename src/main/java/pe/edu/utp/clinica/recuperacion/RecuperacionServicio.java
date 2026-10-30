package pe.edu.utp.clinica.recuperacion;

import pe.edu.utp.clinica.correo.*;
import java.sql.SQLException;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static pe.edu.utp.clinica.recuperacion.VerificacionJdbc.Proposito;

public final class RecuperacionServicio {
    public static final String CONTEXTO=RecuperacionServicio.class.getName();
    private static final Logger LOGGER=Logger.getLogger(RecuperacionServicio.class.getName());
    private final VerificacionJdbc almacen;
    private final EmisorCorreo correo;
    private final Clock reloj;
    private final Executor tareas;

    public RecuperacionServicio(VerificacionJdbc almacen,EmisorCorreo correo,Clock reloj,Executor tareas) {
        this.almacen=almacen;this.correo=correo;this.reloj=reloj;this.tareas=tareas;
    }

    public String solicitar(String identificador,boolean personal,String vinculo,String ip) {
        String limpio=identificador==null?"":identificador.trim().toLowerCase(Locale.ROOT);
        if(personal)Correo.validarDireccion(limpio);
        else if(!limpio.matches("[a-z0-9]{8,15}"))throw new IllegalArgumentException("Documento inválido.");
        String id=UUID.randomUUID().toString();
        tareas.execute(()->{
            try {
                var entrega=almacen.iniciarRecuperacion(id,limpio,personal,vinculo,ip,reloj.instant());
                entrega.ifPresent(datos->enviarCodigo(datos,Proposito.RECUPERAR,false));
            } catch(SQLException e){LOGGER.warning("No se pudo preparar el código de recuperación.");}
        });
        return id;
    }

    public Optional<String> iniciarCambio(int usuario,String sesion,String vinculo,Proposito proposito,String password,String ip)throws SQLException {
        String id=UUID.randomUUID().toString();
        var entrega=almacen.iniciarCambio(id,new VerificacionJdbc.InicioCambio(usuario,sesion,vinculo,proposito,password,ip),reloj.instant());
        if(entrega.isEmpty())return Optional.empty();
        tareas.execute(()->enviarCodigo(entrega.get(),proposito,false));
        return Optional.of(id);
    }

    public boolean verificar(String id,String vinculo,Proposito proposito,String sesion,String codigo)throws SQLException {
        return almacen.verificar(id,vinculo,proposito,sesion,codigo,reloj.instant());
    }
    public boolean autorizada(String id,String vinculo,Proposito proposito,String sesion)throws SQLException {
        return almacen.autorizada(id,vinculo,proposito,sesion,reloj.instant());
    }
    public boolean cambiarPassword(String id,String vinculo,Proposito proposito,String sesion,String nueva)throws SQLException {
        return almacen.cambiarPassword(id,vinculo,proposito,sesion,nueva,reloj.instant());
    }
    public boolean prepararCorreo(String id,String vinculo,String sesion,String nuevo)throws SQLException {
        var entrega=almacen.prepararCorreo(id,vinculo,sesion,nuevo,reloj.instant());
        if(entrega.isEmpty())return false;
        tareas.execute(()->enviarCodigo(entrega.get(),Proposito.CAMBIAR_CORREO,true));
        return true;
    }
    public boolean confirmarCorreo(String id,String vinculo,String sesion,String codigo)throws SQLException {
        var resultado=almacen.confirmarCorreo(id,vinculo,sesion,codigo,reloj.instant());
        if(resultado.cambiado()) {
            try {tareas.execute(()->{
                try {correo.enviar(PlantillasCorreo.correoActualizado(resultado.anterior()));}
                catch(EnvioCorreoException e){LOGGER.warning("No se pudo entregar el aviso de cambio de correo.");}
            });}catch(RejectedExecutionException e){LOGGER.warning("No se pudo encolar el aviso de cambio de correo.");}
        }
        return resultado.cambiado();
    }
    private void enviarCodigo(VerificacionJdbc.Entrega datos,Proposito proposito,boolean nuevo) {
        String operacion=switch(proposito){case RECUPERAR->"recuperar tu contraseña";case CAMBIAR_PASSWORD->"cambiar tu contraseña";case CAMBIAR_CORREO->nuevo?"confirmar tu nueva dirección de correo":"iniciar un cambio de correo";};
        try {correo.enviar(PlantillasCorreo.verificacion(datos.correo(),operacion,datos.codigo()));}
        catch(EnvioCorreoException e){LOGGER.warning("No se pudo entregar el código de verificación.");}
    }
}
