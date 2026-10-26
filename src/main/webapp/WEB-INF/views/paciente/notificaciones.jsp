<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombreCompleto();
    }
    if (nombrePaciente == null) {
        nombrePaciente = "María Pérez";
    }

    String inicialesPaciente = "MP";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    Integer unreadCountObj = (Integer) request.getAttribute("unreadCount");
    int numNotifSinLeer = unreadCountObj != null ? unreadCountObj : 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Notificaciones y Ofertas - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/notificaciones.css">
</head>
<body style="display: flex; height: 100vh; overflow: hidden; background: #f8fafc; font-family: 'Inter', sans-serif;">

    <!-- SIDEBAR INSTITUCIONAL -->
    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clínica Internacional" class="brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div class="brand-text">
                <span class="brand-title">Clínica Internacional</span>
                <span class="brand-subtitle">Portal del paciente</span>
            </div>
        </div>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="home" style="width: 18px; height: 18px;"></i>
                <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="search" style="width: 18px; height: 18px;"></i>
                <span>Buscar cita</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item">
                <i data-lucide="calendar" style="width: 18px; height: 18px;"></i>
                <span>Mis citas</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="nav-item">
                <i data-lucide="clock" style="width: 18px; height: 18px;"></i>
                <span>Lista de espera</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item active">
                <i data-lucide="bell" style="width: 18px; height: 18px;"></i>
                <span>Notificaciones</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px; height: 18px;"></i>
                <span>Perfil</span>
            </a>
        </nav>

        <div class="user-profile">
            <div class="avatar"><%= inicialesPaciente %></div>
            <div class="user-info">
                <span class="user-name"><%= nombrePaciente %></span>
                <span class="user-role">Paciente</span>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-logout" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                <i data-lucide="log-out" style="width: 18px; height: 18px;"></i>
            </button>
</form>
        </div>
    </aside>

    <!-- ÁREA PRINCIPAL -->
    <main class="main-content" style="flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #f8fafc;">

        <!-- BARRA SUPERIOR -->
        <header class="dash-top-header">
            <div class="breadcrumb-trail">
                <span>Paciente</span>
                <span class="bc-sep">/</span>
                <strong>Notificaciones</strong>
            </div>

            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-avisos-pill">
                <i data-lucide="bell" style="width: 16px; height: 16px; color: #64748b;"></i>
                <span>Avisos</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-avisos-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
        </header>

        <!-- CONTENEDOR CON SCROLL -->
        <div class="notif-scroll-container">

            <!-- ENCABEZADO DE PÁGINA -->
            <div class="notif-header-section">
                <h1 class="notif-main-title">Notificaciones</h1>
                <p class="notif-main-subtitle">Bandeja de notificaciones</p>
            </div>

            <!-- TOAST FEEDBACK ALERT -->
            <c:if test="${not empty toastMsg}">
                <div class="notif-toast-bar ${toastTipo}">
                    <i data-lucide="${toastTipo == 'success' ? 'check-circle' : 'info'}" style="width: 18px; height: 18px;"></i>
                    <span><c:out value="${toastMsg}" /></span>
                </div>
            </c:if>

            <!-- ============================================================== -->
            <!-- 1. BANNER CONDICIONAL: OFERTA ACTIVA DE CUPO RECUPERADO       -->
            <!-- ============================================================== -->
            <c:if test="${not empty ofertaCupo and not ofertaExpirada}">
                <div class="card-oferta-activa" id="cardOfertaActiva">
                    <div class="oferta-top-row">
                        <div class="oferta-header-left">
                            <div class="oferta-icon-circle">
                                <i data-lucide="bell" style="width: 22px; height: 22px;"></i>
                            </div>
                            <div class="oferta-title-group">
                                <span class="badge-urgente-pill">Urgente</span>
                                <h2 class="oferta-title-text">¡Oferta de cupo recuperado disponible!</h2>
                                <span class="oferta-subtitle-text">Oferta activa · pendiente de respuesta</span>
                            </div>
                        </div>

                        <!-- CONTADOR REGRESIVO AMBER -->
                        <div class="timer-countdown-card">
                            <span class="timer-countdown-label">Tiempo restante para confirmar</span>
                            <div class="timer-countdown-digits" id="countdownTimerDigits">${ofertaCupo.tiempoRestante}</div>
                        </div>
                    </div>

                    <!-- 4 TARJETAS DE DETALLE -->
                    <div class="oferta-details-grid">
                        <div class="oferta-col-item">
                            <span class="oferta-col-label">Especialidad médica</span>
                            <span class="oferta-col-val"><c:out value="${ofertaCupo.especialidad}" /></span>
                        </div>
                        <div class="oferta-col-item">
                            <span class="oferta-col-label">Profesional asignado</span>
                            <span class="oferta-col-val"><c:out value="${ofertaCupo.medico}" /></span>
                        </div>
                        <div class="oferta-col-item">
                            <span class="oferta-col-label">Sede y consultorio</span>
                            <span class="oferta-col-val"><c:out value="${ofertaCupo.sede}" /></span>
                        </div>
                        <div class="oferta-col-item">
                            <span class="oferta-col-label">Fecha y hora ofrecida</span>
                            <span class="oferta-col-val"><c:out value="${ofertaCupo.fechaOfrecida}" /></span>
                        </div>
                    </div>

                    <div class="oferta-footnote">
                        <c:out value="${ofertaCupo.origen}" />
                    </div>

                    <!-- BOTONES DE ACCIÓN -->
                    <form method="post" action="${pageContext.request.contextPath}/paciente/notificaciones" style="margin: 0;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="idListaEspera" value="${ofertaCupo.idListaEspera}">
                        <div class="oferta-actions-row">
                            <button type="submit" name="accion" value="rechazar" class="btn-rechazar-oferta">
                                Rechazar oferta (mantener mi posición en lista de espera)
                            </button>
                            <div class="oferta-right-cta">
                                <button type="submit" name="accion" value="aceptar" class="btn-aceptar-oferta">
                                    <i data-lucide="check" style="width: 16px; height: 16px;"></i> Aceptar oferta y reservar este cupo
                                </button>
                                <div class="oferta-legal-note">
                                    Al aceptar, tu registro en la lista de espera se cierra y tu asistencia queda confirmada.
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
            </c:if>

            <!-- ============================================================== -->
            <!-- 2. BANNER CONDICIONAL: OFERTA DE CUPO EXPIRÓ                   -->
            <!-- ============================================================== -->
            <c:if test="${ofertaExpirada}">
                <div class="card-oferta-expirada" id="cardOfertaExpirada">
                    <div class="expirada-header">
                        <div class="expirada-icon-circle">
                            <i data-lucide="clock" style="width: 22px; height: 22px;"></i>
                        </div>
                        <div class="expirada-title-wrap">
                            <h2 class="expirada-title-text">La oferta de cupo expiró</h2>
                            <span class="badge-expirada-pill">Oferta expirada</span>
                        </div>
                    </div>
                    <p class="expirada-desc-text">
                        No respondiste dentro del tiempo límite. El cupo se ofreció a otro paciente.
                    </p>
                    <div class="expirada-info-box">
                        <i data-lucide="info" style="width: 18px; height: 18px; flex-shrink: 0;"></i>
                        <span>Sigues en la lista de espera con tu misma posición.</span>
                    </div>
                    <div>
                        <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="btn-ver-espera-link">
                            <span>Ver mi lista de espera</span>
                            <i data-lucide="chevron-right" style="width: 14px; height: 14px;"></i>
                        </a>
                    </div>
                </div>
            </c:if>

            <!-- ============================================================== -->
            <!-- 3. TABLA: HISTORIAL DE NOTIFICACIONES Y EVENTOS                 -->
            <!-- ============================================================== -->
            <div class="historial-section-header">
                <h3 class="historial-title">Historial de notificaciones y eventos</h3>
                <span class="historial-count-archived"><c:out value="${totalNotificaciones}" /> registros archivados</span>
            </div>

            <div class="table-card-container">
                <table class="notif-table" id="tablaNotificaciones">
                    <thead>
                        <tr>
                            <th style="width: 160px;">Fecha / Hora</th>
                            <th style="width: 190px;">Categoría</th>
                            <th>Detalle del registro</th>
                            <th style="width: 110px; text-align: center;">Estado</th>
                            <th style="width: 110px; text-align: center;">Acción</th>
                        </tr>
                    </thead>
                    <tbody id="notifTableBody">
                        <c:choose>
                            <c:when test="${not empty listaNotificaciones}">
                                <c:forEach var="n" items="${listaNotificaciones}">
                                    <tr class="notif-row-item">
                                        <td>
                                            <div class="td-fecha"><c:out value="${n.fecha}" /></div>
                                            <div class="td-hora"><c:out value="${n.hora}" /></div>
                                        </td>
                                        <td>
                                            <div class="td-categoria"><c:out value="${n.categoria}" /></div>
                                        </td>
                                        <td>
                                            <div class="td-detalle"><c:out value="${n.detalle}" /></div>
                                        </td>
                                        <td style="text-align: center;">
                                            <span class="badge-estado ${n.estado == 'Pendiente' ? 'pendiente' : 'leido'}">
                                                <c:out value="${n.estado}" />
                                            </span>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${n.accion == 'Confirmar'}">
                                                    <form method="POST" action="${pageContext.request.contextPath}/paciente/notificaciones" style="margin:0;">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<input type="hidden" name="action" value="confirmar">
<input type="hidden" name="id" value="${n.idNotificacion}">
<button type="submit" class="btn-tb-confirmar" title="Confirmar asistencia">
                                                        Confirmar
                                                    </button></form>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="button" class="btn-tb-ver" onclick="abrirModalDetalle('${n.idNotificacion}', '${n.categoria}', '${n.detalle}', '${n.fecha} ${n.hora}')" title="Ver detalle">
                                                        Ver
                                                    </button>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="5" style="text-align: center; padding: 40px; color: #94a3b8;">
                                        No hay notificaciones registradas en tu bandeja.
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <!-- CONTROLES DE PAGINACIÓN -->
            <div class="pagination-row">
                <button type="button" class="btn-page-nav" id="btnPrevPage">&lsaquo; Anterior</button>
                <button type="button" class="btn-page-nav" id="btnNextPage">Siguiente &rsaquo;</button>
            </div>

        </div>
    </main>

    <!-- MODAL DETALLE DE NOTIFICACIÓN -->
    <div class="modal-overlay" id="modalDetalleNotif">
        <div class="modal-content-card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                <span id="modalNotifCat" style="font-size: 13px; font-weight: 700; color: #0284c7; text-transform: uppercase;"></span>
                <button type="button" onclick="cerrarModalDetalle()" style="background: none; border: none; cursor: pointer; color: #94a3b8;">
                    <i data-lucide="x" style="width: 18px; height: 18px;"></i>
                </button>
            </div>
            <div id="modalNotifFecha" style="font-size: 12px; color: #64748b; margin-bottom: 12px;"></div>
            <div id="modalNotifMensaje" style="font-size: 14px; color: #1e293b; line-height: 1.5; margin-bottom: 24px;"></div>
            <div style="display: flex; justify-content: flex-end; gap: 10px;">
                <button type="button" onclick="cerrarModalDetalle()" style="background: #ffffff; border: 1px solid #cbd5e1; padding: 8px 18px; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; color: #475569;">
                    Cerrar
                </button>
                <form method="POST" action="${pageContext.request.contextPath}/paciente/notificaciones">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<input type="hidden" name="action" value="marcarLeida">
<input type="hidden" name="id" id="modalNotifId">
<button type="submit" id="modalNotifMarcarLeidaBtn" style="background: #0284c7; color: #ffffff; padding: 8px 18px; border-radius: 6px; font-size: 13px; font-weight: 600; text-decoration: none; display: inline-block;">
                    Marcar como leído
                </button></form>
            </div>
        </div>
    </div>

    <!-- SCRIPTS DE INTERACCIÓN -->
    <script>
        lucide.createIcons();

        // 1. TEMPORIZADOR REGRESIVO INTERACTIVO
        (function initCountdownTimer() {
            var timerEl = document.getElementById('countdownTimerDigits');
            if (!timerEl) return;

            var totalSeconds = ${not empty ofertaCupo.tiempoRestanteSegundos ? ofertaCupo.tiempoRestanteSegundos : 872};

            function updateDisplay() {
                if (totalSeconds <= 0) {
                    timerEl.textContent = '00:00 min';
                    // Redirigir a estado expirado
                    window.location.href = '${pageContext.request.contextPath}/paciente/notificaciones?estado=expirada';
                    return;
                }
                var m = Math.floor(totalSeconds / 60);
                var s = totalSeconds % 60;
                timerEl.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s + ' min';
                totalSeconds--;
            }

            updateDisplay();
            var interval = setInterval(updateDisplay, 1000);
        })();

        // 2. PAGINACIÓN DEL HISTORIAL (CLIENT-SIDE)
        (function initPagination() {
            var rows = document.querySelectorAll('.notif-row-item');
            var rowsPerPage = 5;
            var totalPages = Math.ceil(rows.length / rowsPerPage) || 1;
            var currentPage = 1;

            var btnPrev = document.getElementById('btnPrevPage');
            var btnNext = document.getElementById('btnNextPage');

            function showPage(page) {
                currentPage = page;
                var start = (page - 1) * rowsPerPage;
                var end = start + rowsPerPage;

                rows.forEach(function(row, idx) {
                    if (idx >= start && idx < end) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                });

                if (btnPrev) btnPrev.disabled = (currentPage === 1);
                if (btnNext) btnNext.disabled = (currentPage >= totalPages);
            }

            if (btnPrev) {
                btnPrev.addEventListener('click', function() {
                    if (currentPage > 1) showPage(currentPage - 1);
                });
            }

            if (btnNext) {
                btnNext.addEventListener('click', function() {
                    if (currentPage < totalPages) showPage(currentPage + 1);
                });
            }

            if (rows.length > 0) {
                showPage(1);
            }
        })();

        // 3. MODAL DE DETALLE
        function abrirModalDetalle(id, cat, msg, fecha) {
            document.getElementById('modalNotifCat').textContent = cat || 'Detalle de notificación';
            document.getElementById('modalNotifFecha').textContent = fecha || '';
            document.getElementById('modalNotifMensaje').textContent = msg || '';
            document.getElementById('modalNotifId').value = id;
            document.getElementById('modalDetalleNotif').style.display = 'flex';
        }

        function cerrarModalDetalle() {
            document.getElementById('modalDetalleNotif').style.display = 'none';
        }

        window.onclick = function(event) {
            var modal = document.getElementById('modalDetalleNotif');
            if (event.target === modal) {
                cerrarModalDetalle();
            }
        };
    </script>
</body>
</html>
