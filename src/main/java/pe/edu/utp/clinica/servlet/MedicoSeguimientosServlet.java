package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name="MedicoSeguimientosServlet", urlPatterns={"/medico/seguimientos"})
public final class MedicoSeguimientosServlet extends MedicoHistorialServlet {
    @Override protected String metodosPermitidos() { return "GET, POST"; }
    @Override protected void mostrar(HttpServletRequest request, HttpServletResponse response, Contexto contexto) throws IOException, ServletException {
        String estado = request.getParameter("estado");
        if (estado == null) estado = "";
        var datos = new LinkedHashMap<String,Object>();
        datos.put("seguimientos", historial.seguimientos(contexto.medico(), consulta(request), estado, pagina(request)));
        String origen = request.getParameter("origen");
        datos.put("seleccion", origen == null ? null : historial.seguimiento(contexto.medico(), positivo(origen)));
        datos.put("controles", origen == null ? List.of() : historial.controles(contexto.medico(), positivo(origen)));
        if (json(request)) ApiJson.write(response, 200, new ApiResponse<>("SEGUIMIENTOS_MEDICO_OBTENIDOS", "Consulta completada.", datos));
        else {
            datos.forEach(request::setAttribute);
            request.getRequestDispatcher("/WEB-INF/views/medico/seguimientos.jsp").forward(request,response);
        }
    }
    @Override protected void ejecutar(HttpServletRequest request, HttpServletResponse response, Contexto contexto) throws IOException {
        if (!"vincular".equals(request.getParameter("accion"))) throw new AgendaException(400,"ACCION_INVALIDA","Selecciona una acción válida.");
        int origen = positivo(request.getParameter("idCitaOrigen"));
        int control = positivo(request.getParameter("idCitaControl"));
        historial.vincular(contexto.medico(), origen, control);
        if (json(request)) ApiJson.write(response, 200, new ApiResponse<>("SEGUIMIENTO_VINCULADO","La reserva se vinculó correctamente.",Map.of("idCitaOrigen",origen,"idCitaControl",control)));
        else response.sendRedirect(request.getContextPath()+"/medico/seguimientos?origen="+origen);
    }
}
