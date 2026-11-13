package pe.edu.utp.clinica.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiException;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.CatalogoDatosException;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class CatalogoApiServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CatalogoApiServlet.class.getName());
    @Override
    protected final void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            response.setHeader("Allow", "GET");
            ApiJson.write(response, 405, new ApiResponse<>("METODO_NO_PERMITIDO", "Utiliza GET para consultar el catálogo.", null));
            return;
        }
        ApiResponse<?> result;
        try {
            result = consultar(request);
        } catch (ApiException e) {
            ApiJson.write(response, e.status(), new ApiResponse<>(e.code(), e.getMessage(), null));
            return;
        } catch (CatalogoDatosException e) {
            LOGGER.warning(() -> "Catálogo temporalmente no disponible: " + e.getClass().getSimpleName());
            ApiJson.write(response, 503, new ApiResponse<>("CATALOGO_NO_DISPONIBLE", "No se pudo consultar el catálogo. Inténtalo más tarde.", null));
            return;
        } catch (RuntimeException e) {
            LOGGER.warning(() -> "Error interno al consultar catálogo: " + e.getClass().getSimpleName());
            ApiJson.write(response, 500, new ApiResponse<>("ERROR_INTERNO", "No fue posible completar la consulta.", null));
            return;
        }
        ApiJson.write(response, 200, result);
    }

    protected abstract ApiResponse<?> consultar(HttpServletRequest request) throws ApiException;
}
