package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.UsuarioDAO;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.SecurityUtil;
import static org.junit.jupiter.api.Assertions.*;

class AuthSinDemostracionTest {
    private static class Usuarios extends UsuarioDAO {
        Usuario cuenta;
        @Override public Usuario buscarPorEmailODni(String id){return cuenta;}
        @Override public void registrarAuditoria(Integer id,String ip,String evento,String descripcion,boolean sospechoso){}
    }
    @Test void noHayUsuariosSinteticosNiUnaPasswordMaestra() {
        var usuarios=new Usuarios();var auth=new AuthService(usuarios);
        assertNull(auth.autenticar("medico@clinica.pe","Password123!","127.0.0.1"));
        usuarios.cuenta=new Usuario(10,"Médico","Prueba","medico@clinica.pe",SecurityUtil.hashPassword("UnaClavePrivada#2026"),"MEDICO",null,true);
        assertNull(auth.autenticar("medico@clinica.pe","Password123!","127.0.0.1"));
        assertNotNull(auth.autenticar("medico@clinica.pe","UnaClavePrivada#2026","127.0.0.1"));
    }
    @Test void unaCuentaPendienteNoPuedeIngresarAunqueLaClaveCoincida() {
        var usuarios=new Usuarios();usuarios.cuenta=new Usuario(11,"Personal","Pendiente","pendiente@example.test",SecurityUtil.hashPassword("UnaClavePrivada#2026"),"MEDICO",null,false);
        assertNull(new AuthService(usuarios).autenticar("pendiente@example.test","UnaClavePrivada#2026","127.0.0.1"));
    }
}
