package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import java.io.IOException;
import java.util.LinkedHashMap;

@WebServlet(name="MedicoPacientesServlet", urlPatterns={"/medico/pacientes"})
public final class MedicoPacientesServlet extends MedicoHistorialServlet {
    @Override protected void mostrar(HttpServletRequest request, HttpServletResponse response, Contexto contexto) throws IOException, ServletException {
        var datos = new LinkedHashMap<String,Object>();
        String paciente = request.getParameter("idPaciente");
        if (paciente == null) datos.put("pacientes", historial.pacientes(contexto.medico(), consulta(request), pagina(request)));
        else {
            int id = positivo(paciente);
            var historia = historial.historial(contexto.medico(), id, pagina(request));
            datos.put("paciente", historia.paciente());
            datos.put("atenciones", historia.atenciones());
        }
        if (json(request)) ApiJson.write(response, 200, new ApiResponse<>("PACIENTES_MEDICO_OBTENIDOS", "Consulta completada.", datos));
        else {
            datos.forEach(request::setAttribute);
            request.getRequestDispatcher("/WEB-INF/views/medico/pacientes.jsp").forward(request,response);
        }
    }
}
