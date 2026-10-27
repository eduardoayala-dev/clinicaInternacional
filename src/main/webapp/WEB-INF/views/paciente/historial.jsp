<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) nombrePaciente = "Paciente";

    List<Map<String, Object>> citasActivas = (List<Map<String, Object>>) request.getAttribute("citasActivas");
    List<Map<String, Object>> historialPasadas = (List<Map<String, Object>>) request.getAttribute("historialPasadas");
    List<Map<String, Object>> citasCanceladas = (List<Map<String, Object>>) request.getAttribute("citasCanceladas");

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    int numNotifSinLeer = request.getAttribute("numNotifSinLeer") != null ? (Integer) request.getAttribute("numNotifSinLeer") : 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mis Citas - Portal del Paciente | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/mis-citas.css">
</head>
<body>

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
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item active">
                <i data-lucide="calendar" style="width: 18px; height: 18px;"></i>
                <span>Mis citas</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="nav-item">
                <i data-lucide="clock" style="width: 18px; height: 18px;"></i>
                <span>Lista de espera</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
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

    <!-- CONTENIDO PRINCIPAL -->
    <main class="main-content">
        <c:if test="${not empty sessionScope.agendaError}">
            <p role="alert" class="catalogo-estado"><c:out value="${sessionScope.agendaError}"/></p>
            <c:remove var="agendaError" scope="session"/>
        </c:if>


        <!-- BARRA SUPERIOR (HEADER BLANCO INSTITUCIONAL) -->
        <header class="dash-top-header">
            <nav class="breadcrumb-trail">
                <span>Paciente</span> <span class="bc-sep">/</span> <strong>Mis citas</strong>
            </nav>

            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-avisos-pill">
                <i data-lucide="bell" style="width: 16px; height: 16px;"></i>
                <span>Avisos</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-avisos-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
        </header>

        <!-- CONTENEDOR CON SCROLL -->
        <div class="citas-scroll-container">

            <!-- ENCABEZADO DE SECCIÓN -->
            <section class="page-title-section">
                <h1 class="citas-title">Mis citas</h1>
                <p class="citas-subtitle">Tus citas médicas programadas</p>
            </section>

            <% if ("ok".equals(request.getParameter("reprogramada"))) { %>
                <div style="background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 12px; padding: 14px 20px; margin-bottom: 20px; display: flex; align-items: center; gap: 12px; color: #065f46;">
                    <i data-lucide="check-circle" style="width: 20px; height: 20px; color: #10b981; flex-shrink: 0;"></i>
                    <span style="font-size: 14px; font-weight: 500;"><strong>¡Cita reprogramada con éxito!</strong> Tu nuevo horario ha sido agendado y la cita anterior se liberó en el sistema.</span>
                </div>
            <% } %>

            <!-- TABS / PESTAÑAS DE FILTRO -->
            <div class="citas-tabs-nav">
                <button type="button" class="citas-tab-btn active" id="tabBtnActivas" onclick="cambiarPestana('activas')">
                    Activas / próximas (<%= citasActivas != null ? citasActivas.size() : 0 %>)
                </button>
                <button type="button" class="citas-tab-btn" id="tabBtnPasadas" onclick="cambiarPestana('pasadas')">
                    Historial de citas pasadas
                </button>
                <button type="button" class="citas-tab-btn" id="tabBtnCanceladas" onclick="cambiarPestana('canceladas')">
                    Canceladas y reprogramadas (<%= citasCanceladas != null ? citasCanceladas.size() : 0 %>)
                </button>
            </div>

            <!-- CONTENEDOR DE LA TABLA: PESTAÑA 1 - ACTIVAS / PRÓXIMAS -->
            <div id="panelActivas" class="tab-panel">
                <div class="table-card-container">
                    <% if (citasActivas != null && !citasActivas.isEmpty()) { %>
                        <table class="citas-table">
                            <thead>
                                <tr>
                                    <th>Fecha y hora</th>
                                    <th>Médico</th>
                                    <th>Especialidad</th>
                                    <th>Sede</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Map<String, Object> c : citasActivas) { 
                                    String estado = (String) c.get("estado");
                                    boolean esConfirmada = "CONFIRMADA".equalsIgnoreCase(estado);
                                    boolean esPendiente = "PENDIENTE".equalsIgnoreCase(estado) || "PENDIENTE_CONFIRMACION".equalsIgnoreCase(estado);
                                %>
                                    <tr>
                                        <td>
                                            <span class="cell-primary"><%= c.get("fechaFormateada") %></span>
                                            <span class="cell-secondary"><%= c.get("horaFormateada") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("medico") %></span>
                                            <span class="cell-secondary"><%= c.get("cmp") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("especialidad") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("sede") %></span>
                                            <span class="cell-secondary"><%= c.get("consultorio") %></span>
                                        </td>
                                        <td>
                                            <% if (esConfirmada) { %>
                                                <span class="badge-status confirmada">Confirmada · Web</span>
                                            <% } else if (esPendiente) { %>
                                                <span class="badge-status pendiente">Pendiente de confirmar</span>
                                                <span class="cell-secondary">Plazo: 24h antes</span>
                                                <form method="POST" action="${pageContext.request.contextPath}/paciente/historial" style="margin-top: 4px;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="confirmar">
                                                    <input type="hidden" name="idCita" value="<%= c.get("idCita") %>">
                                                    <button type="submit" class="btn-confirmar-asistencia">Confirmar asistencia</button>
                                                </form>
                                            <% } else { %>
                                                <span class="badge-status confirmada"><%= estado %></span>
                                            <% } %>
                                        </td>
                                        <td class="actions-cell">
                                            <button type="button" class="action-link" onclick="abrirModalDetalle('<%= c.get("id") %>', '<%= c.get("medico") %>', '<%= c.get("especialidad") %>', '<%= c.get("fechaFormateada") %> <%= c.get("horaFormateada") %>', '<%= c.get("sede") %>', '<%= c.get("consultorio") %>')">Ver detalle</button>
                                            <% if (esConfirmada || esPendiente) { %>
                                                <a href="${pageContext.request.contextPath}/paciente/reprogramar?idCita=<%= c.get("idCita") %>" class="action-link">Reprogramar</a>
                                                <button type="button" class="action-link danger btn-trigger-cancelar"
                                                        data-id="<%= c.get("idCita") %>"
                                                        data-especialidad="<%= c.get("especialidad") %>"
                                                        data-medico="<%= c.get("medico") %>"
                                                        data-fechahora="<%= c.get("fechaModal") != null ? c.get("fechaModal") : (c.get("fechaFormateada") + " · " + c.get("horaFormateada")) %>"
                                                        data-sede="<%= c.get("sede") %> · <%= c.get("consultorio") %>">Cancelar</button>
                                            <% } %>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <div class="table-empty-state">
                            <i data-lucide="calendar-check" style="width: 44px; height: 44px;"></i>
                            <div class="empty-title">No tienes citas médicas programadas próximamente</div>
                            <div class="empty-desc">Reserva una consulta con nuestros especialistas de forma rápida y 100% digital.</div>
                            <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-nueva-cita-solid" style="margin-top: 8px;">
                                <i data-lucide="plus" style="width: 16px; height: 16px;"></i>
                                <span>Buscar y agendar una cita</span>
                            </a>
                        </div>
                    <% } %>
                </div>
            </div>

            <!-- CONTENEDOR DE LA TABLA: PESTAÑA 2 - HISTORIAL PASADAS -->
            <div id="panelPasadas" class="tab-panel" style="display: none;">
                <div class="table-card-container">
                    <% if (historialPasadas != null && !historialPasadas.isEmpty()) { %>
                        <table class="citas-table">
                            <thead>
                                <tr>
                                    <th>Fecha y hora</th>
                                    <th>Médico</th>
                                    <th>Especialidad</th>
                                    <th>Sede</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Map<String, Object> c : historialPasadas) { %>
                                    <tr>
                                        <td>
                                            <span class="cell-primary"><%= c.get("fechaFormateada") %></span>
                                            <span class="cell-secondary"><%= c.get("horaFormateada") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("medico") %></span>
                                            <span class="cell-secondary"><%= c.get("cmp") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("especialidad") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("sede") %></span>
                                            <span class="cell-secondary"><%= c.get("consultorio") %></span>
                                        </td>
                                        <td>
                                            <span class="badge-status completada">Atendida</span>
                                        </td>
                                        <td class="actions-cell">
                                            <button type="button" class="action-link" onclick="abrirModalDetalle('<%= c.get("id") %>', '<%= c.get("medico") %>', '<%= c.get("especialidad") %>', '<%= c.get("fechaFormateada") %> <%= c.get("horaFormateada") %>', '<%= c.get("sede") %>', '<%= c.get("consultorio") %>')">Ver constancia</button>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <div class="table-empty-state">
                            <i data-lucide="clock" style="width: 44px; height: 44px;"></i>
                            <div class="empty-title">Sin historial de citas pasadas</div>
                            <div class="empty-desc">Las consultas médicas que concluyas se archivarán en esta sección para tu revisión.</div>
                        </div>
                    <% } %>
                </div>
            </div>

            <!-- CONTENEDOR DE LA TABLA: PESTAÑA 3 - CANCELADAS -->
            <div id="panelCanceladas" class="tab-panel" style="display: none;">
                <div class="table-card-container">
                    <% if (citasCanceladas != null && !citasCanceladas.isEmpty()) { %>
                        <table class="citas-table">
                            <thead>
                                <tr>
                                    <th>Fecha y hora</th>
                                    <th>Médico</th>
                                    <th>Especialidad</th>
                                    <th>Sede</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Map<String, Object> c : citasCanceladas) { 
                                    String estCanc = (String) c.get("estado");
                                    boolean esReprog = "REPROGRAMADA".equalsIgnoreCase(estCanc);
                                %>
                                    <tr>
                                        <td>
                                            <span class="cell-primary"><%= c.get("fechaFormateada") %></span>
                                            <span class="cell-secondary"><%= c.get("horaFormateada") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("medico") %></span>
                                            <span class="cell-secondary"><%= c.get("cmp") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("especialidad") %></span>
                                        </td>
                                        <td>
                                            <span class="cell-primary"><%= c.get("sede") %></span>
                                            <span class="cell-secondary"><%= c.get("consultorio") %></span>
                                        </td>
                                        <td>
                                            <% if (esReprog) { %>
                                                <span class="badge-status" style="background-color: #ede9fe; color: #5b21b6; border: 1px solid #ddd6fe; font-weight: 600;">Reprogramada</span>
                                            <% } else { %>
                                                <span class="badge-status cancelada">Cancelada</span>
                                            <% } %>
                                        </td>
                                        <td class="actions-cell">
                                            <button type="button" class="action-link" onclick="abrirModalDetalle('<%= c.get("id") %>', '<%= c.get("medico") %>', '<%= c.get("especialidad") %>', '<%= c.get("fechaFormateada") %> <%= c.get("horaFormateada") %>', '<%= c.get("sede") %>', '<%= c.get("consultorio") %>')">Ver detalle</button>
                                            <a href="${pageContext.request.contextPath}/paciente/citas" class="action-link">Reagendar</a>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } else { %>
                        <div class="table-empty-state">
                            <i data-lucide="check-circle" style="width: 44px; height: 44px;"></i>
                            <div class="empty-title">No tienes citas canceladas</div>
                            <div class="empty-desc">No registras ninguna cancelación en tu cuenta.</div>
                        </div>
                    <% } %>
                </div>
            </div>

            <!-- TARJETAS INFORMATIVAS INFERIORES -->
            <div class="info-cards-grid">
                <div class="info-banner-card">
                    <div class="info-banner-icon">
                        <i data-lucide="info" style="width: 18px; height: 18px;"></i>
                    </div>
                    <div class="info-banner-body">
                        <span class="info-banner-title">Recomendación para el ingreso al consultorio</span>
                        <span class="info-banner-desc">Preséntate 30 minutos antes de la hora asignada con tu DNI físico o credencial digital oficial.</span>
                    </div>
                </div>

                <div class="info-banner-card">
                    <div class="info-banner-icon">
                        <i data-lucide="file-text" style="width: 18px; height: 18px;"></i>
                    </div>
                    <div class="info-banner-body">
                        <span class="info-banner-title">Documentación requerida</span>
                        <span class="info-banner-desc">No olvides tu documento de identidad: lo necesitas para registrar tu llegada en admisión.</span>
                    </div>
                </div>
            </div>

            <!-- BARRA DE ACCIÓN INFERIOR -->
            <div class="citas-footer-bar">
                <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-nueva-cita-solid">
                    <i data-lucide="plus" style="width: 18px; height: 18px;"></i>
                    <span>Buscar y programar nueva cita</span>
                </a>

                <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="link-espera-foot">
                    <span>Ver mis solicitudes en lista de espera</span>
                    <i data-lucide="arrow-right" style="width: 16px; height: 16px;"></i>
                </a>
            </div>

        </div> <!-- /.citas-scroll-container -->

    </main>

    <!-- MODAL DETALLE DE CITA -->
    <div id="modalDetalleCita" style="display: none; position: fixed; inset: 0; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(2px); z-index: 50; align-items: center; justify-content: center;">
        <div style="background: #ffffff; border-radius: 16px; width: 90%; max-width: 480px; padding: 28px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #e2e8f0; padding-bottom: 14px;">
                <h3 style="font-size: 17px; font-weight: 700; color: #0f172a;" id="modalIdCita">Detalle de la Cita</h3>
                <button type="button" onclick="cerrarModalDetalle()" style="background: none; border: none; font-size: 20px; color: #64748b; cursor: pointer;">&times;</button>
            </div>
            <div style="display: flex; flex-direction: column; gap: 14px; font-size: 13.5px;">
                <div>
                    <span style="color: #64748b; display: block; font-size: 12px;">Especialidad / Médico:</span>
                    <strong style="color: #0f172a;" id="modalDetalleMedico">--</strong>
                </div>
                <div>
                    <span style="color: #64748b; display: block; font-size: 12px;">Fecha y hora designada:</span>
                    <strong style="color: #0f172a;" id="modalDetalleFecha">--</strong>
                </div>
                <div>
                    <span style="color: #64748b; display: block; font-size: 12px;">Sede y consultorio:</span>
                    <strong style="color: #0f172a;" id="modalDetalleSede">--</strong>
                </div>
                <div>
                    <span style="color: #64748b; display: block; font-size: 12px;">Modalidad de atención:</span>
                    <strong style="color: #0284c7;">Presencial · Clínica Internacional</strong>
                </div>
            </div>
            <div style="margin-top: 24px; text-align: right;">
                <button type="button" onclick="cerrarModalDetalle()" style="background: #0284c7; color: #ffffff; border: none; padding: 9px 22px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer;">Entendido</button>
            </div>
        </div>
    </div>

    <!-- MODAL CANCELAR CITA -->
    <div id="modalCancelarCita" class="modal-cancelar-backdrop" style="display: none;">
        <div class="modal-cancelar-card">
            <h2 class="modal-cancelar-title">¿Cancelar tu cita?</h2>
            
            <div class="modal-cancelar-detalles">
                <div class="modal-detalle-row">
                    <span class="modal-detalle-label">Especialidad</span>
                    <span class="modal-detalle-val" id="modalCancelarEspecialidad">Cardiología</span>
                </div>
                <div class="modal-detalle-row">
                    <span class="modal-detalle-label">Médico</span>
                    <span class="modal-detalle-val" id="modalCancelarMedico">Dr. Carlos Mendoza</span>
                </div>
                <div class="modal-detalle-row">
                    <span class="modal-detalle-label">Fecha y hora</span>
                    <span class="modal-detalle-val" id="modalCancelarFechaHora">Lun 12 oct 2026 · 08:30</span>
                </div>
                <div class="modal-detalle-row">
                    <span class="modal-detalle-label">Sede</span>
                    <span class="modal-detalle-val" id="modalCancelarSede">Sede Lima Norte · Consultorio 114</span>
                </div>
            </div>

            <form id="formCancelarCita" method="POST" action="${pageContext.request.contextPath}/paciente/historial">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="cancelar">
                <input type="hidden" name="idCita" id="modalCancelarIdCita" value="">

                <div class="modal-cancelar-field">
                    <label for="selectMotivoCancelacion" class="modal-cancelar-label">Motivo de cancelación (opcional)</label>
                    <div class="select-wrapper">
                        <select id="selectMotivoCancelacion" name="motivo" class="modal-cancelar-select">
                            <option value="" selected>Selecciona un motivo</option>
                            <option value="Cambio de planes">Cambio de planes</option>
                            <option value="Imprevisto laboral o personal">Imprevisto laboral o personal</option>
                            <option value="Problemas de salud / Emergencia">Problemas de salud / Emergencia</option>
                            <option value="Prefiero otra fecha u horario">Prefiero otra fecha u horario</option>
                            <option value="Ya no requiero la consulta médica">Ya no requiero la consulta médica</option>
                            <option value="Otro motivo">Otro motivo</option>
                        </select>
                        <i data-lucide="chevron-down" class="select-arrow-icon"></i>
                    </div>
                </div>

                <div class="modal-cancelar-alert">
                    <i data-lucide="alert-triangle" class="modal-alert-icon"></i>
                    <div class="modal-alert-content">
                        <strong class="modal-alert-title">Importante</strong>
                        <p class="modal-alert-text">Si cancelas, el cupo se libera y puede ofrecerse a otro paciente.</p>
                    </div>
                </div>

                <div class="modal-cancelar-actions">
                    <button type="button" class="btn-cancelar-cancel" onclick="cerrarModalCancelar()">No, mantener cita</button>
                    <button type="submit" class="btn-cancelar-confirm">Sí, cancelar cita</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        lucide.createIcons();

        function cambiarPestana(tipo) {
            document.querySelectorAll('.citas-tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(p => p.style.display = 'none');

            if (tipo === 'activas') {
                document.getElementById('tabBtnActivas').classList.add('active');
                document.getElementById('panelActivas').style.display = 'block';
            } else if (tipo === 'pasadas') {
                document.getElementById('tabBtnPasadas').classList.add('active');
                document.getElementById('panelPasadas').style.display = 'block';
            } else if (tipo === 'canceladas') {
                document.getElementById('tabBtnCanceladas').classList.add('active');
                document.getElementById('panelCanceladas').style.display = 'block';
            }
        }

        function abrirModalDetalle(id, medico, especialidad, fecha, sede, consultorio) {
            document.getElementById('modalIdCita').innerText = 'Detalle de la Cita (' + id + ')';
            document.getElementById('modalDetalleMedico').innerText = especialidad + ' · ' + medico;
            document.getElementById('modalDetalleFecha').innerText = fecha;
            document.getElementById('modalDetalleSede').innerText = sede + ' · ' + consultorio;
            const m = document.getElementById('modalDetalleCita');
            m.style.display = 'flex';
        }

        function cerrarModalDetalle() {
            document.getElementById('modalDetalleCita').style.display = 'none';
        }

        function abrirModalCancelar(id, especialidad, medico, fechaHora, sede) {
            document.getElementById('modalCancelarIdCita').value = id || '';
            document.getElementById('modalCancelarEspecialidad').innerText = especialidad || '--';
            document.getElementById('modalCancelarMedico').innerText = medico || '--';
            document.getElementById('modalCancelarFechaHora').innerText = fechaHora || '--';
            document.getElementById('modalCancelarSede').innerText = sede || '--';
            const sel = document.getElementById('selectMotivoCancelacion');
            if (sel) sel.selectedIndex = 0;
            const m = document.getElementById('modalCancelarCita');
            m.style.display = 'flex';
            lucide.createIcons();
        }

        function cerrarModalCancelar() {
            const m = document.getElementById('modalCancelarCita');
            if (m) m.style.display = 'none';
        }

        document.addEventListener('click', function(e) {
            const btn = e.target.closest('.btn-trigger-cancelar');
            if (btn) {
                abrirModalCancelar(
                    btn.dataset.id,
                    btn.dataset.especialidad,
                    btn.dataset.medico,
                    btn.dataset.fechahora,
                    btn.dataset.sede
                );
            }
        });

        document.getElementById('modalCancelarCita')?.addEventListener('click', function(e) {
            if (e.target === this) {
                cerrarModalCancelar();
            }
        });
    </script>

</body>
</html>
