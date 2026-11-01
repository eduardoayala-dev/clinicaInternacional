package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.InvitacionPersonalDAO;
import pe.edu.utp.clinica.correo.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InvitacionPersonalServicioTest {
    private static class Almacen extends InvitacionPersonalDAO {
        byte[] hash;Boolean enviado;
        @Override public Entrega crear(int actor,Solicitud datos,byte[] token){hash=token;assertEquals(90,actor);return new Entrega(100,"persona@example.test");}
        @Override public void registrarEnvio(int usuario,byte[] token,boolean enviado){assertEquals(100,usuario);assertArrayEquals(hash,token);this.enviado=enviado;}
    }
    @Test void enviaUnEnlaceConFragmentoSinContraseñaNiTokenEnElResultado() {
        var almacen=new Almacen();var mensajes=new ArrayList<Correo>();
        var servicio=new InvitacionPersonalServicio(almacen,c->{mensajes.add(c);return "simulado";},"http://localhost:8080");
        var resultado=servicio.invitar(90,null);assertTrue(resultado.enviado());assertEquals(true,almacen.enviado);
        Correo correo=mensajes.get(0);assertTrue(correo.texto().matches("(?s).*http://localhost:8080/activar-personal#[A-Za-z0-9_-]{43}.*"));
        assertTrue(correo.html().contains("Clínica Internacional"));assertFalse(correo.texto().contains("Password123"));assertFalse(resultado.toString().contains("token"));
    }
    @Test void siGmailFallaLaCuentaSiguePendienteYSeInformaSinInventarEntrega() {
        var almacen=new Almacen();var servicio=new InvitacionPersonalServicio(almacen,c->{throw new EnvioCorreoException("Fallo",true);},"http://localhost:8080");
        assertFalse(servicio.invitar(90,null).enviado());assertEquals(false,almacen.enviado);
    }
    @Test void nuncaUsaElHostDeUnaPeticionNiPermiteHttpPublico() {
        assertEquals("http://localhost:8080",InvitacionPersonalServicio.basePublica(Map.of()));
        assertEquals("https://clinica.up.railway.app",InvitacionPersonalServicio.basePublica(Map.of("RAILWAY_PROJECT_ID","prueba","RAILWAY_PUBLIC_DOMAIN","clinica.up.railway.app")));
        for(String url:List.of("http://externo.test","https://usuario@externo.test","https://externo.test?token=x","https://externo.test#fragmento")) {
            assertThrows(IllegalArgumentException.class,()->InvitacionPersonalServicio.basePublica(Map.of("APP_PUBLIC_URL",url)));
        }
        assertThrows(IllegalArgumentException.class,()->InvitacionPersonalServicio.basePublica(Map.of("APP_ENV","production")));
    }
}
