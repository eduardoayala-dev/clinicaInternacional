package pe.edu.utp.clinica.servlet;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.filter.CsrfFilter;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.service.AuthService;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.util.CsrfToken;
import static org.junit.jupiter.api.Assertions.*;

class SesionSeguraTest {
    @Test void recoveredLoginGetsFreshTokenAndFixedNoticeWithoutEchoingUserInput() throws Exception {
        for (String path : new String[]{"/login/paciente", "/login/personal"}) {
            var servlet = new LoginServlet();
            var x = new HttpExchange();
            x.path = path;
            x.method = "GET";
            x.session = null;
            x.parameters.put("error", "sesion_formulario");
            new CsrfFilter().doFilter(x.request, x.response, (req, res) -> servlet.doGet(x.request, x.response));
            assertNotNull(x.session.getAttribute("csrfToken"));
            assertTrue(x.attributes.get("errorMessage").toString().contains("venció o cambió"));
            assertEquals(path.endsWith("personal") ? "/WEB-INF/views/login-personal.jsp" : "/WEB-INF/views/login-paciente.jsp", x.forward);
        }
        var unexpected = new HttpExchange();
        unexpected.method = "GET";
        unexpected.path = "/login/paciente";
        unexpected.parameters.put("error", "<script>prueba</script>");
        new LoginServlet().doGet(unexpected.request, unexpected.response);
        assertNull(unexpected.attributes.get("errorMessage"));
    }

    @Test void loginReplacesSessionAndCsrfToken() throws Exception {
        LoginServlet servlet = new LoginServlet();
        var field = LoginServlet.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(servlet, new AuthService() {
            @Override public Usuario autenticar(String id, String password, String ip) {
                return new Usuario(10,"P","Uno","p@test","","PACIENTE",null,true);
            }
        });
        HttpExchange x = new HttpExchange(); x.path="/login/paciente";
        String oldId=x.session.getId(), oldToken=CsrfToken.getOrCreate(x.session);
        x.parameters.put("csrfToken",oldToken); x.parameters.put("identificador","p@test");
        x.parameters.put("password","prueba");
        new CsrfFilter().doFilter(x.request,x.response,(req,res) -> servlet.doPost(x.request,x.response));
        assertNotEquals(oldId,x.session.getId());
        assertNotEquals(oldToken,x.session.getAttribute("csrfToken"));
        assertEquals(10,x.session.getAttribute("usuarioId"));
        assertFalse(CsrfToken.isValid(x.request), "El token anterior ya no autoriza la nueva sesión");
    }

    @Test void logoutRequiresPostAndValidSessionToken() throws Exception {
        LoginServlet servlet=new LoginServlet(); HttpExchange x=new HttpExchange(); x.path="/logout";
        String id=x.session.getId(); x.method="GET";
        servlet.doGet(x.request,x.response);
        assertEquals(405,x.status); assertEquals(id,x.session.getId());
        x.method="POST";
        new CsrfFilter().doFilter(x.request,x.response,(req,res) -> servlet.doPost(x.request,x.response));
        assertEquals(403,x.status); assertNotNull(x.session);
        x.parameters.put("csrfToken",CsrfToken.getOrCreate(x.session));
        new CsrfFilter().doFilter(x.request,x.response,(req,res) -> servlet.doPost(x.request,x.response));
        assertNull(x.session); assertEquals("/login?msg=sesion_cerrada",x.redirect);
    }
    @Test void logoutConservaElPortalDeCadaRolAntesDeInvalidarLaSesion() throws Exception {
        for (String rol : new String[]{"PACIENTE","MEDICO","PERSONAL_ADMINISTRATIVO","ADMINISTRADOR"}) {
            var x=new HttpExchange();x.path="/logout";
            x.session.setAttribute("usuarioLogueado",new Usuario(10,"Prueba","Local","demo@example.test","",rol,null,true));
            x.parameters.put("portal","personal");
            x.parameters.put("csrfToken",CsrfToken.getOrCreate(x.session));
            new CsrfFilter().doFilter(x.request,x.response,(req,res)->new LoginServlet().doPost(x.request,x.response));
            assertNull(x.session);
            assertEquals(("PACIENTE".equals(rol)?"/login/paciente":"/login/personal")+"?msg=sesion_cerrada",x.redirect);
        }
    }
    @Test void logoutSinSesionSoloAdmiteElDestinoPersonalConocido() throws Exception {
        for (String portal : new String[]{"personal","https://otro.example/","//otro.example/"}) {
            var x=new HttpExchange();x.path="/logout";
            x.parameters.put("portal",portal);
            x.parameters.put("csrfToken",CsrfToken.getOrCreate(x.session));
            new CsrfFilter().doFilter(x.request,x.response,(req,res)->new LoginServlet().doPost(x.request,x.response));
            assertEquals(("personal".equals(portal)?"/login/personal":"/login")+"?msg=sesion_cerrada",x.redirect);
        }
    }
}
