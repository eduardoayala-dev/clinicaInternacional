package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.PropuestaHorarioDAO;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@WebServlet(name="MedicoHorariosServlet", urlPatterns="/medico/horarios")
public final class MedicoHorariosServlet extends MedicoPortalServlet {
    private transient PropuestaHorarioDAO propuestas;
    @Override public void init() { super.init(); propuestas = new PropuestaHorarioDAO(); }
    @Override protected String metodosPermitidos() { return "GET, POST"; }

    @Override protected void mostrar(HttpServletRequest request, HttpServletResponse response, Contexto contexto) throws IOException, ServletException {
        var lista = HorariosVista.paginar(request, propuestas.delMedico(contexto.usuario().getIdUsuario()));
        if (json(request)) ApiJson.write(response, 200, new ApiResponse<>("PROPUESTAS_OBTENIDAS", "Consulta completada.", lista));
        else {
            request.setAttribute("hoy", LocalDate.now(reloj()));
            request.getRequestDispatcher("/WEB-INF/views/medico/horarios.jsp").forward(request, response);
        }
    }

    @Override protected void ejecutar(HttpServletRequest request, HttpServletResponse response, Contexto contexto) throws IOException {
        try {
            int dias = 0;
            String[] seleccion = request.getParameterValues("dias");
            if (seleccion != null) for (String dia : seleccion) {
                int numero = Integer.parseInt(dia);
                if (numero < 1 || numero > 7) throw new NumberFormatException();
                dias |= 1 << (numero - 1);
            }
            int id = propuestas.proponer(contexto.usuario().getIdUsuario(), dias,
                    LocalDate.parse(request.getParameter("desde")), LocalDate.parse(request.getParameter("hasta")),
                    LocalTime.parse(request.getParameter("inicio")), LocalTime.parse(request.getParameter("fin")),
                    Integer.parseInt(request.getParameter("minutos")));
            if (json(request)) ApiJson.write(response, 201, new ApiResponse<>("PROPUESTA_CREADA", "Propuesta enviada a admisión.", Map.of("idPropuesta", id)));
            else {
                request.getSession().setAttribute("horariosMensaje", "Propuesta enviada. Los cupos se publicarán cuando admisión la apruebe.");
                response.sendRedirect(request.getContextPath()+"/medico/horarios");
            }
        } catch (java.time.DateTimeException | IllegalArgumentException | NullPointerException e) {
            fallo(request, response, new AgendaException(400, "PROPUESTA_INVALIDA", "Completa días, fechas, horas y duración válidos."));
        } catch (AgendaException e) { fallo(request, response, e); }
    }

    private static void fallo(HttpServletRequest request, HttpServletResponse response, AgendaException e) throws IOException {
        if (json(request)) ApiJson.write(response, e.status(), new ApiResponse<>(e.code(), e.getMessage(), null));
        else {
            request.getSession().setAttribute("horariosError", e.getMessage());
            response.sendRedirect(request.getContextPath()+"/medico/horarios");
        }
    }
}
