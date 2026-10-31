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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/confirmaciones-asistencia.css">
</head>
<body>
<aside class="adm-sidebar" aria-label="Navegación administrativa">
    <a href="${pageContext.request.contextPath}/admin/dashboard" class="adm-brand">
        <div class="adm-brand-logo-wrap"><img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="" class="adm-brand-logo-img"></div>
        <div><div class="adm-brand-name">Clínica Internacional</div><div class="adm-brand-sub">Portal administrativo</div></div>
    </a>
    <nav class="adm-nav" aria-label="Navegación administrativa">
        <a href="${pageContext.request.contextPath}/admin/dashboard" class="adm-nav-item"><i data-lucide="home"></i><span>Inicio</span></a>
        <a href="${pageContext.request.contextPath}/admin/medicos" class="adm-nav-item"><i data-lucide="stethoscope"></i><span>Médicos y especialidades</span></a>
        <a href="${pageContext.request.contextPath}/admin/pacientes" class="adm-nav-item"><i data-lucide="users"></i><span>Pacientes</span></a>
        <a href="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones" class="adm-nav-item active" aria-current="page"><i data-lucide="phone-call"></i><span>Confirmaciones y seguimiento</span></a>
        <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="adm-nav-item"><i data-lucide="refresh-cw"></i><span>Reasignaciones y excepciones</span></a>
        <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="adm-nav-item"><i data-lucide="trending-up"></i><span>Dashboard operativo</span></a>
        <a href="${pageContext.request.contextPath}/cuenta/seguridad" class="adm-nav-item"><i data-lucide="shield-check"></i><span>Seguridad de cuenta</span></a>
    </nav>
    <div class="adm-user-footer"><div class="adm-user-info-link"><div class="adm-avatar"><%= Html.texto(iniciales.toUpperCase()) %></div><div class="adm-user-text"><div class="adm-user-name"><%= Html.texto(nombre) %></div><div class="adm-user-role">Personal administrativo</div></div></div>
        <form method="POST" action="${pageContext.request.contextPath}/logout"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button type="submit" class="adm-logout-btn" aria-label="Cerrar sesión"><i data-lucide="log-out"></i></button></form>
    </div>
</aside>
<div class="adm-main-wrapper">
    <header class="adm-topbar"><div class="adm-breadcrumb">Admisión <span>/ Confirmaciones y seguimiento</span></div></header>
    <main class="adm-content-scroll seguimiento-page">
        <section class="seguimiento-heading"><div><h1 class="adm-page-title">Confirmaciones de asistencia</h1><p class="adm-page-subtitle">Revisa próximas citas y registra el resultado de cada contacto con el paciente.</p></div><a class="seguimiento-refresh" href="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones"><i data-lucide="refresh-cw"></i>Actualizar</a></section>
        <% if (mensaje != null) { %><output class="seguimiento-flash"><i data-lucide="circle-check"></i><span><%= Html.texto(mensaje) %></span></output><% } %>
        <section class="seguimiento-metricas" aria-label="Resumen de seguimiento">
            <article class="seguimiento-metrica"><span>Citas sin confirmar</span><strong><%= Html.texto(pendientes == null ? 0 : pendientes) %></strong><small>En tu sede y con fecha futura</small></article>
            <article class="seguimiento-metrica vencidas"><span>Requieren seguimiento</span><strong><%= Html.texto(vencidas == null ? 0 : vencidas) %></strong><small>El plazo de confirmación venció</small></article>
        </section>
        <aside class="seguimiento-nota" aria-label="Instrucciones de contacto"><i data-lucide="info"></i><p>Registra una llamada o un correo que realizaste. Esta pantalla no envía mensajes. La cita permanece vigente si el paciente no responde; registra “Confirmó” solo cuando el paciente lo indique.</p></aside>
        <section class="seguimiento-tabla-card">
            <div class="seguimiento-tabla-titulo"><div><h2>Próximas citas pendientes</h2><p>Los datos se limitan a la sede de tu perfil.</p></div><span><%= Html.texto(seguimientos == null ? 0 : seguimientos.size()) %> registros</span></div>
            <% if (seguimientos == null || seguimientos.isEmpty()) { %>
                <div class="seguimiento-vacio"><i data-lucide="calendar-check"></i><strong>Todo al día</strong><p>No hay confirmaciones pendientes en tu sede.</p></div>
            <% } else { %>
                <div class="seguimiento-tabla-scroll"><table class="seguimiento-tabla"><thead><tr><th>Paciente</th><th>Cita</th><th>Contacto</th><th>Estado del plazo</th><th>Último intento</th><th>Registrar gestión</th></tr></thead><tbody>
                <% for (ConfirmacionAsistenciaDAO.Seguimiento item : seguimientos) { %>
                    <tr>
                        <td><strong><%= Html.texto(item.paciente()) %></strong><small>DNI <%= Html.texto(item.documento()) %></small></td>
                        <td><strong><%= Html.texto(FormatoClinico.fechaHora(LocalDateTime.parse(item.fechaHora()))) %></strong><small>Reserva <%= Html.texto(item.idCita()) %></small></td>
                        <td><a href="mailto:<%= Html.texto(item.correo()) %>"><%= Html.texto(item.correo()) %></a><small><%= Html.texto(item.telefono()) %></small></td>
                        <td><span class="seguimiento-estado <%= item.vencida() ? "vencida" : "pendiente" %>"><%= item.vencida() ? "Plazo vencido" : "Pendiente" %></span><small><%= item.intentos() %> intentos registrados</small></td>
                        <td><% if (item.ultimoContacto() == null) { %><span class="seguimiento-sin-intento">Sin intentos</span><% } else { %><strong><%= Html.texto(FormatoClinico.fechaHora(LocalDateTime.parse(item.ultimoContacto()))) %></strong><small><%= Html.texto(item.ultimoResultado()) %></small><% } %></td>
                        <td><form class="seguimiento-form" method="POST" action="${pageContext.request.contextPath}/admin/seguimiento-confirmaciones">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="idCita" value="<%= Html.texto(item.idCita()) %>">
                            <label><span>Canal realizado</span><select name="canal" required><option value="" disabled selected>Selecciona</option><option value="LLAMADA">Llamada</option><option value="CORREO">Correo</option></select></label>
                            <label><span>Resultado</span><select name="resultado" required><option value="" disabled selected>Selecciona</option><option value="SIN_RESPUESTA">Sin respuesta</option><option value="CONFIRMA">Confirmó asistencia</option><option value="NO_CONFIRMA">No confirma</option></select></label>
                            <button type="submit">Guardar gestión</button>
                        </form></td>
                    </tr>
                <% } %>
                </tbody></table></div>
            <% } %>
        </section>
    </main>
</div>
<script>if(window.lucide)lucide.createIcons();</script>
</body>
</html>
