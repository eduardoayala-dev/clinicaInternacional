package pe.edu.utp.clinica.servlet;

import pe.edu.utp.clinica.dao.MedicoHistorialDAO;
import pe.edu.utp.clinica.dao.HistorialMedicoRepository;
import jakarta.servlet.http.HttpServletRequest;

abstract class MedicoHistorialServlet extends MedicoPortalServlet {
    protected transient HistorialMedicoRepository historial;
    protected HistorialMedicoRepository crearHistorialDAO() { return new MedicoHistorialDAO(); }
    @Override public void init() { super.init(); historial = crearHistorialDAO(); }
    protected static int pagina(HttpServletRequest request) {
        String valor = request.getParameter("pagina");
        return valor == null ? 1 : positivo(valor);
    }
    protected static String consulta(HttpServletRequest request) {
        String valor = request.getParameter("q");
        return valor == null ? "" : valor.strip();
    }
}
