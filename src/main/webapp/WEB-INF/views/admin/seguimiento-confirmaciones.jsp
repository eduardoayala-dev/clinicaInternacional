<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario,pe.edu.utp.clinica.dao.ConfirmacionAsistenciaDAO,pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.util.FormatoClinico,java.util.List,java.time.LocalDateTime" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombre = usuario == null ? "Admisión" : usuario.getNombres() + " " + usuario.getApellidos();
    String[] nombres = nombre.trim().split("\\s+");
    String iniciales = nombres.length > 1 ? nombres[0].substring(0,1) + nombres[1].substring(0,1) : nombre.substring(0,Math.min(2,nombre.length()));
    List<ConfirmacionAsistenciaDAO.Seguimiento> seguimientos = (List<ConfirmacionAsistenciaDAO.Seguimiento>) request.getAttribute("seguimientos");
    Number pendientes = (Number) request.getAttribute("totalPendientes");
    Number vencidas = (Number) request.getAttribute("totalVencidas");
    String mensaje = (String) request.getAttribute("mensajeFlash");
    int intentosMinimos = ((Number)request.getAttribute("intentosMinimos")).intValue();
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Confirmaciones de asistencia | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:wght@600;700;800&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261102-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/confirmaciones-asistencia.css?v=20261102-1">
</head>
<body>
<%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
<div class="adm-main-wrapper">
    <header class="adm-topbar"><div class="adm-breadcrumb">Admisión <span>/ Confirmaciones y seguimiento</span></div></header>
    <main class="adm-content-scroll seguimiento-page" data-seguimiento-panel>
        <section class="seguimiento-heading"><div><h1 class="adm-page-title">Confirmaciones de asistencia</h1><p class="adm-page-subtitle">Revisa próximas citas y registra el resultado de cada contacto con el paciente.</p></div><a id="seguimientoActualizar" data-eventos-url="${pageContext.request.contextPath}/admin/eventos-confirmaciones" class="seguimiento-refresh" href="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones"><i data-lucide="refresh-cw"></i>Actualizar</a></section>
        <% if (mensaje != null) { %><output class="seguimiento-flash"><i data-lucide="circle-check"></i><span><%= Html.texto(mensaje) %></span></output><% } %>
        <div class="seguimiento-actualizacion-fila"><p id="seguimientoEstado" class="seguimiento-actualizacion">Conectando actualización en vivo…</p><button type="button" id="seguimientoDescartar" class="seguimiento-descartar" hidden>Descartar selección y actualizar</button></div>
        <output id="seguimientoError" class="seguimiento-error" hidden></output>
        <div id="seguimientoDatos" class="seguimiento-datos">
        <section class="seguimiento-metricas" aria-label="Resumen de seguimiento">
            <article class="seguimiento-metrica"><span>Citas sin confirmar</span><strong><%= Html.texto(pendientes == null ? 0 : pendientes) %></strong><small>En tu sede y con fecha futura</small></article>
            <article class="seguimiento-metrica vencidas"><span>Requieren seguimiento</span><strong><%= Html.texto(vencidas == null ? 0 : vencidas) %></strong><small>El plazo de confirmación venció</small></article>
        </section>
        <aside class="seguimiento-nota" aria-label="Instrucciones de contacto"><i data-lucide="info"></i><p>Registra una llamada o un correo que realizaste. Guardar la gestión no envía mensajes. La cita sigue vigente sin respuesta; registra “Confirmó” solo cuando el paciente lo indique. Cancelar por falta de confirmación requiere el plazo vencido y al menos <%= intentosMinimos %> contactos realizados. La cancelación manual puede notificar al paciente.</p></aside>
        <section class="seguimiento-tabla-card" aria-label="Citas pendientes de confirmación">
            <div class="seguimiento-tabla-titulo"><div><h2>Próximas citas pendientes</h2><p>Los datos se limitan a la sede de tu perfil.</p></div><span><%= Html.texto(seguimientos == null ? 0 : seguimientos.size()) %> registros</span></div>
            <% if (seguimientos == null || seguimientos.isEmpty()) { %>
                <div class="seguimiento-vacio"><i data-lucide="calendar-check"></i><strong>Todo al día</strong><p>No hay confirmaciones pendientes en tu sede.</p></div>
            <% } else { %>
                <div class="seguimiento-tabla-scroll"><table class="seguimiento-tabla"><thead><tr><th>Paciente</th><th>Cita</th><th>Contacto</th><th>Estado del plazo</th><th>Último intento</th><th>Registrar gestión</th></tr></thead><tbody>
                <% for (ConfirmacionAsistenciaDAO.Seguimiento item : seguimientos) { %>
                    <tr>
                        <td><strong><%= Html.texto(item.paciente()) %></strong><small>DNI <%= Html.texto(item.documento()) %></small></td>
                        <td><strong><%= Html.texto(FormatoClinico.fechaHora(LocalDateTime.parse(item.fechaHora()))) %></strong><small>Reserva <%= Html.texto(item.idCita()) %></small><a href="${pageContext.request.contextPath}/admin/seguimiento-ia?cita=<%= item.idCita() %>">Ver apoyo de IA</a></td>
                        <td><a href="mailto:<%= Html.texto(item.correo()) %>"><%= Html.texto(item.correo()) %></a><small><%= Html.texto(item.telefono()) %></small></td>
                        <td><span class="seguimiento-estado <%= item.vencida() ? "vencida" : "pendiente" %>"><%= item.vencida() ? "Plazo vencido" : "Pendiente" %></span><small><%= item.intentos() %> intentos registrados</small></td>
                        <td><% if (item.ultimoContacto() == null) { %><span class="seguimiento-sin-intento">Sin intentos</span><% } else { %><strong><%= Html.texto(FormatoClinico.fechaHora(LocalDateTime.parse(item.ultimoContacto()))) %></strong><small><%= Html.texto(item.ultimoResultado()) %></small><% } %></td>
                        <td><form class="seguimiento-form" method="POST" action="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="idCita" value="<%= Html.texto(item.idCita()) %>">
                            <label><span>Canal realizado</span><select name="canal" required><option value="" disabled selected>Selecciona</option><option value="LLAMADA">Llamada</option><option value="CORREO">Correo</option></select></label>
                            <label><span>Resultado</span><select name="resultado" required><option value="" disabled selected>Selecciona</option><option value="SIN_RESPUESTA">Sin respuesta</option><option value="CONFIRMA">Confirmó asistencia</option><option value="NO_CONFIRMA">No confirma</option></select></label>
                            <button type="submit">Guardar gestión</button>
                        </form>
                        <% if(item.vencida() && item.intentos() >= intentosMinimos){ %>
                        <form method="POST" action="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones" onsubmit="return confirm('¿Cancelar esta cita por falta de confirmación? Se liberará el horario.');">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="idCita" value="<%= item.idCita() %>">
                            <input type="hidden" name="accion" value="cancelar_sin_confirmacion">
                            <button type="submit" class="seguimiento-cancelar">Cancelar por falta de confirmación</button>
                        </form>
                        <% } else { %><small>Mínimo <%= intentosMinimos %> contactos y plazo vencido para cancelar por falta de confirmación.</small><% } %>
                        </td>
                    </tr>
                <% } %>
                </tbody></table></div>
            <% } %>
        </section>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/seguimiento-confirmaciones.js?v=20261031-2" defer></script>
<script>if(window.lucide)lucide.createIcons();</script>
</body>
</html>
