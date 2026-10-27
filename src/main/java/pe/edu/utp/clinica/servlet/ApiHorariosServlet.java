package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import pe.edu.utp.clinica.api.ApiException;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.service.CatalogoService;

@WebServlet(name = "ApiHorariosServlet", urlPatterns = "/api/horarios")
public class ApiHorariosServlet extends CatalogoApiServlet {
    private transient CatalogoService catalogos;

    public ApiHorariosServlet() {}
    ApiHorariosServlet(CatalogoService catalogos) { this.catalogos = catalogos; }

    @Override public void init() { catalogos = new CatalogoService(); }

    @Override protected ApiResponse<?> consultar(HttpServletRequest request) throws ApiException {
        return catalogos.horarios(request.getParameter("idMedico"), request.getParameter("fecha"));
    }
}
