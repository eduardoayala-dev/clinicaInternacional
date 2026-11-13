package pe.edu.utp.clinica.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import pe.edu.utp.clinica.api.ApiException;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.service.CatalogoService;

@WebServlet(name = "ApiMedicosServlet", urlPatterns = "/api/medicos")
public class ApiMedicosServlet extends CatalogoApiServlet {
    private transient CatalogoService catalogos;

    public ApiMedicosServlet() {}
    ApiMedicosServlet(CatalogoService catalogos) { this.catalogos = catalogos; }

    @Override public void init() { catalogos = new CatalogoService(); }

    @Override protected ApiResponse<?> consultar(HttpServletRequest request) throws ApiException {
        return catalogos.medicos(request.getParameter("idEspecialidad"), request.getParameter("idSede"));
    }
}
