package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.clinica.model.Usuario;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet(name = "ExportServlet", urlPatterns = {"/admin/export"})
public class ExportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=acceso_denegado");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (!"PERSONAL_ADMINISTRATIVO".equals(usuario.getRol()) && !"ADMINISTRADOR".equals(usuario.getRol())) {
            response.sendRedirect(request.getContextPath() + "/login/personal?error=rol_invalido");
            return;
        }

        String type = request.getParameter("type");
        String format = request.getParameter("format");

        if ("csv".equalsIgnoreCase(format)) {
            exportarCSV(response, type);
        } else {
            exportarPDFHTML(response, type, usuario.getNombres() + " " + usuario.getApellidos());
        }
    }

    private void exportarCSV(HttpServletResponse response, String type) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_admision_" + System.currentTimeMillis() + ".csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("CÓDIGO;HORA;PACIENTE;DNI;SEDE;CONSULTORIO;MÉDICO;ESTADO;NIVEL_RIESGO");
        writer.println("CIT-101;08:30;Jorge Valdivia Núñez;10492837;Sede Norte;Cons. 302;Dr. Carlos Mendoza;EN ESPERA;BAJO");
        writer.println("CIT-102;08:45;Sofía Rivas C.;72910482;Sede Central;Cons. 104;Dra. Elena Rostova;ADMISIÓN;BAJO");
        writer.println("CIT-103;09:00;Juan Carlos Peña;09482711;Sede Norte;Cons. 210;Dr. Fernando Solís;PENDIENTE;MEDIO");
        writer.println("CIT-104;09:15;Rosa Martínez P.;41928301;Sede Sur;Cons. 012;Dr. Roberto Salas;CONFIRMADA;ALTO");
        writer.println("CIT-105;11:30;Beatriz Sandoval Ramos;76128938;Sede Central;Cons. C-008;Dr. Carlos Mendoza;SIN RESPUESTA;ALTO");
        writer.flush();
    }

    private void exportarPDFHTML(HttpServletResponse response, String type, String nombrePersonal) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        out.println("<!DOCTYPE html>");
        out.println("<html lang='es'><head><meta charset='UTF-8'><title>Reporte Oficial de Operación Diaria - Clínica Internacional</title>");
        out.println("<style>");
        out.println("body { font-family: 'Helvetica Neue', Arial, sans-serif; padding: 40px; color: #0f172a; line-height: 1.5; }");
        out.println(".header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #2563eb; padding-bottom: 20px; margin-bottom: 30px; }");
        out.println(".header h1 { margin: 0; font-size: 22px; color: #2563eb; }");
        out.println(".meta { font-size: 12px; color: #64748b; margin-top: 5px; }");
        out.println("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        out.println("th { background: #f8fafc; color: #475569; font-size: 11px; text-transform: uppercase; padding: 10px; border-bottom: 1px solid #cbd5e1; text-align: left; }");
        out.println("td { font-size: 12px; padding: 12px 10px; border-bottom: 1px solid #e2e8f0; }");
        out.println(".badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
        out.println(".badge-green { background: #dcfce7; color: #166534; }");
        out.println(".badge-amber { background: #fef3c7; color: #92400e; }");
        out.println(".badge-red { background: #fee2e2; color: #991b1b; }");
        out.println(".print-btn { background: #2563eb; color: #fff; border: none; padding: 10px 20px; font-size: 14px; font-weight: bold; border-radius: 6px; cursor: pointer; margin-bottom: 20px; }");
        out.println("@media print { .print-btn { display: none; } }");
        out.println("</style></head><body>");

        out.println("<button class='print-btn' onclick='window.print()'>🖨️ Imprimir / Guardar como PDF</button>");

        out.println("<div class='header'>");
        out.println("<div><h1>CLÍNICA INTERNACIONAL — REPORTE DIARIO DE ADMISIÓN</h1>");
        out.println("<div class='meta'>Generado por: " + nombrePersonal + " | Fecha de emisión: " + fechaActual + "</div></div>");
        out.println("<div style='font-size:12px; text-align:right;'><strong>Sede Principal</strong><br>Sistema de Gestión Operativa v1.0</div>");
        out.println("</div>");

        out.println("<h3>Resumen Ejecutivo de Indicadores Operativos</h3>");
        out.println("<table>");
        out.println("<thead><tr><th>Indicador</th><th>Valor Actual</th><th>Meta Institucional</th><th>Estado</th></tr></thead>");
        out.println("<tbody>");
        out.println("<tr><td>Total Citas Programadas</td><td>420 Citas</td><td>400 Citas</td><td><span class='badge badge-green'>ÓPTIMO</span></td></tr>");
        out.println("<tr><td>Disponibilidad de Médicos en Box</td><td>86.4%</td><td>80.0%</td><td><span class='badge badge-green'>ALTO</span></td></tr>");
        out.println("<tr><td>Tasa de Cancelación Ambulatoria</td><td>9.0%</td><td>&lt; 12.0%</td><td><span class='badge badge-green'>EN RANGO</span></td></tr>");
        out.println("<tr><td>Reasignaciones Exitosas por Algoritmo</td><td>84.2%</td><td>&gt; 75.0%</td><td><span class='badge badge-green'>EFICIENTE</span></td></tr>");
        out.println("<tr><td>Tiempo Promedio en Sala de Espera</td><td>31.4 min</td><td>25.0 min</td><td><span class='badge badge-amber'>TOLERANCIA</span></td></tr>");
        out.println("</tbody></table>");

        out.println("<h3 style='margin-top:40px;'>Citas en Seguimiento Prioritario de Contacto Preventivo</h3>");
        out.println("<table>");
        out.println("<thead><tr><th>Hora</th><th>Paciente / HC</th><th>Contacto</th><th>Sede / Consultorio</th><th>Médico</th><th>Riesgo</th><th>Estado Confirmación</th></tr></thead>");
        out.println("<tbody>");
        out.println("<tr><td>11:30</td><td>Beatriz Sandoval Ramos (HC-76120)</td><td>+51 955 889 001</td><td>Sede Central / C-008</td><td>Dr. Carlos Mendoza</td><td><span class='badge badge-red'>ALTO</span></td><td>SIN RESPUESTA</td></tr>");
        out.println("<tr><td>12:45</td><td>Manuel Fuentes Q. (HC-66319)</td><td>+51 988 221 340</td><td>Sede Norte / C-305</td><td>Dra. Valeria Rivas</td><td><span class='badge badge-amber'>MEDIO</span></td><td>MENSAJE ENVIADO</td></tr>");
        out.println("<tr><td>14:00</td><td>Claudia Vega M. (HC-31509)</td><td>+51 911 405 921</td><td>Sede Sur / C-101</td><td>Dr. Roberto Salas</td><td><span class='badge badge-red'>ALTO</span></td><td>REBOTE CONTACTO</td></tr>");
        out.println("<tr><td>15:30</td><td>Jorge Valdivia N. (HC-99201)</td><td>+51 987 654 321</td><td>Sede Norte / C-302</td><td>Dr. Fernando Solís</td><td><span class='badge badge-green'>BAJO</span></td><td>CONFIRMADO WHATSAPP</td></tr>");
        out.println("</tbody></table>");

        out.println("<div style='margin-top:50px; font-size:11px; color:#94a3b8; border-top:1px solid #e2e8f0; padding-top:10px;'>Documento confidencial para uso interno exclusivo del personal administrativo de la Clínica Internacional.</div>");
        out.println("</body></html>");
    }
}
