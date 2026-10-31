package pe.edu.utp.clinica.eventos;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.function.Consumer;

public final class CanalAgenda implements AsyncListener, WriteListener, AutoCloseable {
    private static final String SINCRONIZAR = "retry: 3000\nevent: sincronizar\ndata: {}\n\n";
    private final AsyncContext contexto;
    private final ServletOutputStream salida;
    private final HttpSession sesion;
    private final int usuario;
    private final Consumer<CanalAgenda> retirar;
    private final ArrayDeque<byte[]> pendientes = new ArrayDeque<>();
    private EventosAgenda.Suscripcion suscripcion;
    private boolean cerrado;
    private boolean iniciado;

    public CanalAgenda(AsyncContext contexto, ServletOutputStream salida, HttpSession sesion,
                       int usuario, Consumer<CanalAgenda> retirar) {
        this.contexto = contexto;
        this.salida = salida;
        this.sesion = sesion;
        this.usuario = usuario;
        this.retirar = retirar;
    }

    public synchronized void iniciar(EventosAgenda.Suscripcion suscripcion, long duracion) {
        if (cerrado) { suscripcion.close(); retirar.accept(this); return; }
        this.suscripcion = suscripcion;
        contexto.addListener(this);
        contexto.setTimeout(duracion);
        salida.setWriteListener(this);
        iniciado = true;
        ofrecer(SINCRONIZAR);
    }

    public void cambio() { ofrecer("event: cambio\ndata: {}\n\n"); }
    public void latido() { ofrecer(": conectado\n\n"); }

    private boolean sesionValida() {
        try {
            return sesion.getAttribute("usuarioLogueado") instanceof Usuario actual
                    && actual.getIdUsuario() == usuario && "PERSONAL_ADMINISTRATIVO".equals(actual.getRol());
        } catch (IllegalStateException e) { return false; }
    }

    private synchronized void ofrecer(String evento) {
        if (cerrado) return;
        if (!sesionValida()) { close(); return; }
        if (pendientes.size() >= 4) {
            pendientes.clear();
            pendientes.add(SINCRONIZAR.getBytes(StandardCharsets.UTF_8));
        } else pendientes.add(evento.getBytes(StandardCharsets.UTF_8));
        escribir();
    }

    private void escribir() {
        try {
            while (iniciado && !cerrado && !pendientes.isEmpty() && salida.isReady()) {
                salida.write(pendientes.removeFirst());
                salida.flush();
            }
        } catch (IOException | IllegalStateException e) { close(); }
    }

    @Override public synchronized void onWritePossible() { escribir(); }
    @Override public void onError(Throwable error) { close(); }
    @Override public void onComplete(AsyncEvent event) { close(); }
    @Override public void onTimeout(AsyncEvent event) { close(); }
    @Override public void onError(AsyncEvent event) { close(); }
    @Override public void onStartAsync(AsyncEvent event) { close(); }

    @Override public synchronized void close() {
        if (cerrado) return;
        cerrado = true;
        pendientes.clear();
        if (suscripcion != null) suscripcion.close();
        retirar.accept(this);
        try { contexto.complete(); }
        catch (IllegalStateException e) { /* El contenedor ya terminó la respuesta. */ }
    }
}
