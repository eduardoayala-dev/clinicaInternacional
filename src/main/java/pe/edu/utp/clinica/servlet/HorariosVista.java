package pe.edu.utp.clinica.servlet;

import jakarta.servlet.http.HttpServletRequest;
import pe.edu.utp.clinica.model.medico.Pagina;
import pe.edu.utp.clinica.model.medico.PropuestaHorario;
import java.util.List;

final class HorariosVista {
    private HorariosVista() {}
    static Pagina<PropuestaHorario> paginar(HttpServletRequest request, List<PropuestaHorario> propuestas) {
        int paginas = Math.max(1, (propuestas.size()+5)/6);
        int pagina = 1;
        try { pagina = Integer.parseInt(request.getParameter("pagina")); }
        catch (NumberFormatException ignored) { pagina = 1; }
        pagina = Math.max(1, Math.min(pagina, paginas));
        int inicio = (pagina-1)*6;
        var datos = new Pagina<>(propuestas.subList(inicio,Math.min(inicio+6,propuestas.size())),pagina,6,pagina<paginas);
        request.setAttribute("propuestas", datos.items());
        request.setAttribute("paginaHorarios", pagina);
        request.setAttribute("paginasHorarios", paginas);
        return datos;
    }
}
