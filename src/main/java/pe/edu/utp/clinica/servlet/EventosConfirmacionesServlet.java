package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.auth.GestorAutenticacion;
import pe.edu.utp.clinica.auth.SesionAutenticada;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.ConfirmacionAsistenciaDAO;
import pe.edu.utp.clinica.eventos.CanalAgenda;
import pe.edu.utp.clinica.eventos.EventosAgenda;
import pe.edu.utp.clinica.model.Usuario;
import pe.edu.utp.clinica.util.TransporteHttp;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebServlet(urlPatterns = "/admin/eventos-confirmaciones", asyncSupported = true)
public final class EventosConfirmacionesServlet extends HttpServlet {
    private final transient Set<CanalAgenda> canales = ConcurrentHashMap.newKeySet();
    private transient ScheduledExecutorService pulsos;
    @SuppressWarnings("java:S2654")
    @Override public void init() {
        pulsos = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "eventos-confirmaciones");
            hilo.setDaemon(true);
            return hilo;
        });
        pulsos.scheduleAtFixedRate(() -> canales.forEach(CanalAgenda::latido), 10, 10, TimeUnit.SECONDS);
    }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        var sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute("usuarioLogueado") instanceof Usuario usuario)
                || !"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol())) {
            TransporteHttp.error(response, 403);
            return;
        }
        try { conectar(request, response, usuario); }
        catch (AgendaException e) { TransporteHttp.error(response, e.status()); }
        catch (IOException | IllegalStateException e) { TransporteHttp.error(response, 503); }
    }

    private void conectar(HttpServletRequest request, HttpServletResponse response, Usuario usuario) throws IOException {
        int sede = new ConfirmacionAsistenciaDAO().sedeDelPersonal(usuario.getIdUsuario());
        if (!request.isAsyncSupported()) { TransporteHttp.error(response, 503); return; }
        long duracion = duracion(request);
        if (duracion <= 0) { TransporteHttp.error(response, 401); return; }
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Accel-Buffering", "no");
        var salida = response.getOutputStream();
        var contexto = request.startAsync();
        var canal = new CanalAgenda(contexto, salida, request.getSession(false),
                usuario.getIdUsuario(), canales::remove);
        var suscripcion = EventosAgenda.INSTANCIA.suscribir(sede, usuario.getIdUsuario(), canal::cambio);
        if (suscripcion == null) {
            response.setStatus(429);
            response.setHeader("Retry-After", "15");
            contexto.complete();
            return;
        }
        canales.add(canal);
        try { canal.iniciar(suscripcion, duracion); }
        catch (IllegalStateException e) { canal.close(); throw e; }
    }

    private static long duracion(HttpServletRequest request) {
        if (request.getAttribute(GestorAutenticacion.SESION) instanceof SesionAutenticada autenticada) {
            return Math.min(45000, Duration.between(Instant.now(), autenticada.vence()).toMillis());
        }
        return 45000;
    }

    @Override public void destroy() {
        if (pulsos != null) pulsos.shutdownNow();
        canales.forEach(CanalAgenda::close);
    }
}
