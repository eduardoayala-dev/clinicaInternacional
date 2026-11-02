<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario,pe.edu.utp.clinica.dao.EnlaceCitaDAO,pe.edu.utp.clinica.util.EnlaceCita,pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.util.FormatoClinico" %>
<%
    Usuario usuario=(Usuario)session.getAttribute("usuarioLogueado");
    String nombrePaciente=usuario.getNombreCompleto();
    String inicialesPaciente=nombrePaciente.isBlank()?"P":nombrePaciente.substring(0,1).toUpperCase();
    int numNotifSinLeer=new pe.edu.utp.clinica.dao.CitaDAO().contarNotificacionesSinLeer(usuario.getIdUsuario());
    String seccionPaciente="historial";
    var enlace=(EnlaceCita)request.getAttribute("enlaceCita");
    var resumen=(EnlaceCitaDAO.Resumen)request.getAttribute("resumenCita");
    boolean confirmar=enlace.accion()==EnlaceCita.Accion.CONFIRMAR;
    boolean activa=resumen.futura()&&("CONFIRMADA".equals(resumen.estado())||"SOLICITADA".equals(resumen.estado()));
    boolean disponible=activa&&(!confirmar||(!resumen.asistenciaConfirmada()&&!resumen.plazoVencido()));
    String titulo=confirmar?"Confirmar asistencia":"Cancelar cita";
%>
<!DOCTYPE html>
<html lang="es"><head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title><%= titulo %> · Clínica Internacional</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/enlace-cita.css">
    <script src="https://unpkg.com/lucide@latest" defer></script>
</head><body>
    <%@ include file="/WEB-INF/jspf/paciente-sidebar.jspf" %>
    <main class="main-content">
        <header class="dash-top-header"><nav aria-label="Ruta de navegación">Paciente / Mis citas / <%= titulo %></nav></header>
        <section class="cita-enlace" aria-labelledby="tituloEnlace">
            <h1 id="tituloEnlace"><%= titulo %></h1>
            <p>Revisa la cita antes de continuar. Abrir este enlace no modifica tu reserva.</p>
            <div class="cita-enlace-card">
                <h2>Cita CIT-<%= resumen.id() %></h2>
                <dl><div><dt>Fecha y hora</dt><dd><%= Html.texto(FormatoClinico.fechaHora(resumen.fecha())) %></dd></div>
                    <div><dt>Médico</dt><dd><%= Html.texto(resumen.medico()) %></dd></div>
                    <div><dt>Especialidad</dt><dd><%= Html.texto(resumen.especialidad()) %></dd></div>
                    <div><dt>Sede</dt><dd><%= Html.texto(resumen.sede()) %></dd></div>
                    <div><dt>Estado</dt><dd><%= Html.texto(resumen.estado()) %></dd></div></dl>
                <% if(disponible){ %>
                <p><%= confirmar?"Confirmarás que asistirás a esta cita. La reserva mantiene su horario.":"Al cancelar se liberará este horario. Para volver a reservar tendrás que buscar disponibilidad." %></p>
                <form method="POST" action="${pageContext.request.contextPath}/cita">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="cita" value="<%= enlace.cita() %>">
                    <input type="hidden" name="accion" value="<%= confirmar?"confirmar":"cancelar" %>">
                    <button type="submit" class="btn-primary <%= confirmar?"":"cita-enlace-cancelar" %>"><%= confirmar?"Confirmar mi asistencia":"Sí, cancelar cita" %></button>
                </form>
                <% } else { %>
                <output><%= !activa?"Esta cita ya no está disponible para esta acción.":resumen.asistenciaConfirmada()?"Tu asistencia ya está confirmada.":"El plazo para confirmar terminó. Contacta con admisión para coordinar tu cita." %></output>
                <% } %>
            </div>
            <a class="btn-secondary" href="${pageContext.request.contextPath}/paciente/historial">Volver a Mis citas</a>
        </section>
    </main>
    <script>window.addEventListener('load',()=>{if(window.lucide)lucide.createIcons();});</script>
</body></html>

