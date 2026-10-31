<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) {
        nombrePaciente = "Paciente";
    }

    String primerNombre = "Paciente";
    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        primerNombre = partes[0];
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    pe.edu.utp.clinica.dao.CitaDAO citaDAOBell = new pe.edu.utp.clinica.dao.CitaDAO();
    int numNotifSinLeer = usuario != null ? citaDAOBell.contarNotificacionesSinLeer(usuario.getIdUsuario()) : 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inicio - Portal del Paciente | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard-paciente.css">
</head>
<body>

    <!-- SIDEBAR -->
    <% String seccionPaciente = "inicio"; %>
    <%@ include file="/WEB-INF/jspf/paciente-sidebar.jspf" %>

    <!-- CONTENIDO PRINCIPAL -->
    <main class="main-content">

        <!-- BARRA SUPERIOR (HEADER BLANCO INSTITUCIONAL) -->
        <header class="dash-top-header">
            <nav class="breadcrumb-trail">
                <span>Paciente</span> <span class="bc-sep">/</span> <strong>Inicio</strong>
            </nav>

            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-avisos-pill">
                <i data-lucide="bell" style="width: 16px; height: 16px;"></i>
                <span>Avisos</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-avisos-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
        </header>

        <!-- ÁREA CON SCROLL PARA EL CONTENIDO DEL DASHBOARD -->
        <div class="dash-scroll-container">

            <!-- ENCABEZADO DE SALUDO -->
            <section class="greeting-section">
                <h1 class="greeting-title">Hola, <%= primerNombre %></h1>
                <p class="greeting-subtitle">Resumen de tus citas, ofertas y lista de espera</p>
            </section>

            <!-- BANNER CONDICIONAL: OFERTA DE CUPO ANTICIPADO -->
            <c:if test="${not empty ofertaCupo}">
                <div class="banner-cupo-oferta">
                    <div class="banner-cupo-left">
                    <div class="banner-cupo-icon">
                        <i data-lucide="bell" style="width: 22px; height: 22px;"></i>
                    </div>
                    <div class="banner-cupo-body">
                        <div class="banner-cupo-heading">
                            <span class="badge-cupo-tag">Oferta de cupo</span>
                            <span class="banner-cupo-title">¡Cupo anticipado disponible por cancelación!</span>
                        </div>
                        <div class="banner-cupo-meta">
                            <span class="banner-meta-field">
                                Especialidad / médico: <strong><c:out value="${ofertaCupo.especialidad}" /> · <c:out value="${ofertaCupo.medico}" /></strong>
                            </span>
                            <span class="banner-meta-field">
                                Nueva fecha ofrecida: <strong><c:out value="${ofertaCupo.fechaOfrecida}" /></strong>
                            </span>
                            <span class="banner-meta-field">
                                Caducidad de la oferta: <span class="timer-amber"><c:out value="${ofertaCupo.tiempoRestante}" /></span>
                            </span>
                        </div>
                    </div>
                </div>

                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-revisar-oferta">
                    <span>Revisar oferta en notificaciones</span>
                    <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                </a>
            </div>
        </c:if>

        <!-- GRID DE 2 TARJETAS (PRÓXIMA CITA / LISTA DE ESPERA) -->
        <div class="cards-summary-grid">

            <!-- TARJETA 1: PRÓXIMA CITA -->
            <div class="dash-summary-card">
                <div>
                    <div class="summary-card-header">
                        <h2 class="summary-card-title">Próxima cita</h2>
                        <c:choose>
                            <c:when test="${not empty proximaCita}">
                                <span class="status-badge-confirmed">Cita programada</span>
                            </c:when>
                            <c:otherwise>
                                <span class="status-badge-confirmed" style="background:#f1f5f9;color:#64748b;">Sin citas</span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <c:choose>
                        <c:when test="${not empty proximaCita}">
                            <div class="card-info-rows">
                                <div class="card-info-row">
                                    <span class="row-label">Fecha y hora designada</span>
                                    <span class="row-val"><c:out value="${proximaCita.fechaHora}" /></span>
                                </div>
                                <div class="card-info-row">
                                    <span class="row-label">Especialidad</span>
                                    <span class="row-val"><c:out value="${proximaCita.especialidad}" /></span>
                                </div>
                                <div class="card-info-row">
                                    <span class="row-label">Profesional</span>
                                    <span class="row-val"><c:out value="${proximaCita.medico}" /></span>
                                </div>
                                <div class="card-info-row">
                                    <span class="row-label">Ubicación clínica</span>
                                    <span class="row-val"><c:out value="${proximaCita.ubicacion}" /></span>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="card-empty-state">
                                <i data-lucide="calendar-check" style="width: 38px; height: 38px; color: #94a3b8;"></i>
                                <span>No tienes citas médicas programadas próximamente.</span>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <c:choose>
                    <c:when test="${not empty proximaCita}">
                        <a href="${pageContext.request.contextPath}/paciente/historial" class="summary-card-link">
                            <span>Ver detalle de cita</span>
                            <i data-lucide="chevron-right" style="width: 15px; height: 15px;"></i>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/paciente/citas" class="summary-card-link">
                            <span>Buscar y agendar una cita</span>
                            <i data-lucide="chevron-right" style="width: 15px; height: 15px;"></i>
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- TARJETA 2: LISTA DE ESPERA -->
            <div class="dash-summary-card">
                <div>
                    <div class="summary-card-header">
                        <h2 class="summary-card-title">Lista de espera</h2>
                        <c:choose>
                            <c:when test="${not empty estadoListaEspera}">
                                <span class="status-badge-waiting">Registro activo</span>
                            </c:when>
                            <c:otherwise>
                                <span class="status-badge-waiting" style="background:#f1f5f9;color:#64748b;">Inactivo</span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <c:choose>
                        <c:when test="${not empty estadoListaEspera}">
                            <div class="card-info-rows">
                                <div class="card-info-row">
                                    <span class="row-label">Estado actual en fila</span>
                                    <span class="row-val"><c:out value="${estadoListaEspera.estado}" /></span>
                                </div>
                                <div class="card-info-row">
                                    <span class="row-label">Especialidad solicitada</span>
                                    <span class="row-val"><c:out value="${estadoListaEspera.especialidad}" /></span>
                                </div>
                                <div class="card-info-row">
                                    <span class="row-label">Sede asignada</span>
                                    <span class="row-val"><c:out value="${estadoListaEspera.sede}" /></span>
                                </div>
                                <div class="card-info-row">
                                    <span class="row-label">Preferencias registradas</span>
                                    <span class="row-val"><c:out value="${estadoListaEspera.preferencias}" /></span>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="card-empty-state">
                                <i data-lucide="clock" style="width: 38px; height: 38px; color: #94a3b8;"></i>
                                <span>No te encuentras en lista de espera actualmente.</span>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="summary-card-link">
                    <span>Ver estado en lista de espera</span>
                    <i data-lucide="chevron-right" style="width: 15px; height: 15px;"></i>
                </a>
            </div>

        </div>

        <!-- SECCIÓN DE ACCIONES RÁPIDAS -->
        <section class="quick-actions-section">
            <h2 class="quick-section-title">Acciones rápidas</h2>
            <div class="quick-actions-grid">

                <a href="${pageContext.request.contextPath}/paciente/citas" class="quick-action-card">
                    <div class="quick-icon-wrapper">
                        <i data-lucide="search" style="width: 20px; height: 20px;"></i>
                    </div>
                    <div class="quick-card-texts">
                        <span class="quick-card-title">Buscar y agendar nueva cita</span>
                        <span class="quick-card-desc">Explorar especialidades y profesionales</span>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/paciente/historial" class="quick-action-card">
                    <div class="quick-icon-wrapper">
                        <i data-lucide="calendar" style="width: 20px; height: 20px;"></i>
                    </div>
                    <div class="quick-card-texts">
                        <span class="quick-card-title">Mis citas programadas</span>
                        <span class="quick-card-desc">Historial y consultas activas</span>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="quick-action-card">
                    <div class="quick-icon-wrapper">
                        <i data-lucide="bell" style="width: 20px; height: 20px;"></i>
                    </div>
                    <div class="quick-card-texts">
                        <span class="quick-card-title">Bandeja de avisos (<%= numNotifSinLeer %>)</span>
                        <span class="quick-card-desc">Ofertas y alertas pendientes</span>
                    </div>
                </a>

            </div>
        </section>

        </div> <!-- /.dash-scroll-container -->

    </main>

    <script>
        lucide.createIcons();
    </script>

</body>
</html>
