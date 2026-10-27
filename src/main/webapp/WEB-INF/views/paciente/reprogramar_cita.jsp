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

    Map<String, Object> cita = (Map<String, Object>) request.getAttribute("cita");
    List<Map<String, Object>> listaSedes = (List<Map<String, Object>>) request.getAttribute("listaSedes");
    List<Map<String, Object>> listaEspecialidades = (List<Map<String, Object>>) request.getAttribute("listaEspecialidades");
    List<Map<String, Object>> listaMedicos = (List<Map<String, Object>>) request.getAttribute("listaMedicos");
    List<Map<String, Object>> listaMotivos = (List<Map<String, Object>>) request.getAttribute("listaMotivos");

    int numNotifSinLeer = request.getAttribute("numNotifSinLeer") != null ? (Integer) request.getAttribute("numNotifSinLeer") : 0;

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    // Fecha mínima permitida para reprogramar (desde hoy en adelante)
    java.time.LocalDate hoy = java.time.LocalDate.now();
    String minFecha = hoy.toString();

    // Obtener la fecha, hora y turno exactos de la cita original
    String fechaCita = null;
    String horaCita = null;
    String turnoCita = "manana";
    if (cita != null) {
        if (cita.get("fecha") != null && !((String)cita.get("fecha")).isEmpty()) {
            fechaCita = (String) cita.get("fecha");
        } else if (cita.get("fechaSolo") != null && !((String)cita.get("fechaSolo")).isEmpty()) {
            fechaCita = (String) cita.get("fechaSolo");
        }

        if (cita.get("hora") != null && !((String)cita.get("hora")).isEmpty()) {
            horaCita = (String) cita.get("hora");
        } else if (cita.get("horaSolo") != null && !((String)cita.get("horaSolo")).isEmpty()) {
            horaCita = (String) cita.get("horaSolo");
        } else if (cita.get("horaFormateada") != null && !((String)cita.get("horaFormateada")).isEmpty()) {
            horaCita = (String) cita.get("horaFormateada");
        }

        if (cita.get("turno") != null) {
            turnoCita = (String) cita.get("turno");
        } else if (horaCita != null && horaCita.contains(":")) {
            try {
                int h = Integer.parseInt(horaCita.split(":")[0]);
                turnoCita = (h < 13) ? "manana" : "tarde";
            } catch (Exception ignored) {}
        }
    }

    String defaultFecha = (fechaCita != null) ? fechaCita : minFecha;
    try {
        if (java.time.LocalDate.parse(defaultFecha).isBefore(hoy)) {
            defaultFecha = minFecha;
        }
    } catch (Exception e) {
        defaultFecha = minFecha;
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reprogramar Cita - Portal del Paciente | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/mis-citas.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reprogramar-cita.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/catalogos-api.css">
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


        <!-- HEADER TOP INSTITUCIONAL -->
        <header class="dash-top-header">
            <nav class="breadcrumb-trail">
                <span>Paciente</span> <span class="bc-sep">/</span> 
                <a href="${pageContext.request.contextPath}/paciente/historial" style="color: inherit; text-decoration: none;">Mis citas</a> <span class="bc-sep">/</span> 
                <strong>Reprogramar</strong>
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
        <div class="reprogramar-scroll-container">
            <div class="reprogramar-content-wrapper">

                <!-- ENCABEZADO DE SECCIÓN -->
                <section class="page-title-section">
                    <h1 class="reprogramar-title">Reprogramar cita</h1>
                    <p class="reprogramar-subtitle">Elige un nuevo horario para tu cita</p>
                </section>

                <!-- BANNER DE AVISO EXACTO SEGÚN DISEÑO CLÍNICO -->
                <div class="reprogramar-banner-aviso">
                    <div class="banner-aviso-principal">
                        Estás reprogramando tu cita del <strong><%= cita != null ? cita.get("fechaFormateadaBanner") : "horario actual" %></strong> con la <strong><%= cita != null ? cita.get("medico") : "tu médico" %></strong> (<%= cita != null ? cita.get("especialidad") : "Especialidad" %>, <%= cita != null ? cita.get("sede") : "Sede" %>).
                    </div>
                    <div class="banner-aviso-secundario">
                        Tu cita actual se mantiene hasta que finalices la nueva reserva. Al finalizarla, la cita anterior se cancela y su cupo se libera.
                    </div>
                </div>

                <!-- STEPPER (1 Buscar, 2 Elegir horario, 3 Confirmar) -->
                <div class="reprogramar-stepper">
                    <div class="step-item active" id="stepper1">
                        <span class="step-circle" id="stepperCircle1">1</span>
                        <span>Buscar</span>
                    </div>
                    <div class="step-item" id="stepper2">
                        <span class="step-circle" id="stepperCircle2">2</span>
                        <span>Elegir horario</span>
                    </div>
                    <div class="step-item" id="stepper3">
                        <span class="step-circle" id="stepperCircle3">3</span>
                        <span>Confirmar</span>
                    </div>
                </div>

                <!-- FORMULARIO PRINCIPAL DE REPROGRAMACIÓN -->
                <form id="formReprogramar" method="POST" action="${pageContext.request.contextPath}/paciente/reprogramar">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="idCita" value="<%= cita != null ? cita.get("idCita") : "" %>">
                    <input type="hidden" name="hora" id="inputHoraSeleccionada" value="">
                    <input type="hidden" name="urgencia" value="<%= cita != null ? cita.get("nivelUrgencia") : "BAJA" %>">

                    <!-- ==========================================
                         PASO 1: BÚSQUEDA Y FILTROS
                         ========================================== -->
                    <div class="reprogramar-card" id="stepContent1">
                        <div class="reprog-form-grid">
                            
                            <!-- SEDE -->
                            <div class="form-field-group">
                                <div class="form-label-row">
                                    <label class="form-label" for="selectSede">Sede *</label>
                                </div>
                                <select class="select-control" id="selectSede" name="idSede" onchange="cargarMedicosDinamicos()">
                                    <% if (listaSedes != null) {
                                        for (Map<String, Object> s : listaSedes) {
                                            int sId = (Integer) s.get("id");
                                            boolean sel = (cita != null && cita.get("idSede") != null && ((Integer)cita.get("idSede")) == sId);
                                    %>
                                        <option value="<%= sId %>" <%= sel ? "selected" : "" %>><%= s.get("nombre") %></option>
                                    <%  }
                                    } %>
                                </select>
                            </div>

                            <!-- ESPECIALIDAD -->
                            <div class="form-field-group">
                                <div class="form-label-row">
                                    <label class="form-label" for="selectEspecialidad">Especialidad *</label>
                                </div>
                                <select class="select-control" id="selectEspecialidad" name="idEspecialidad" onchange="cargarMedicosDinamicos()">
                                    <% if (listaEspecialidades != null) {
                                        for (Map<String, Object> e : listaEspecialidades) {
                                            int eId = (Integer) e.get("id");
                                            boolean sel = (cita != null && cita.get("idEspecialidad") != null && ((Integer)cita.get("idEspecialidad")) == eId);
                                    %>
                                        <option value="<%= eId %>" <%= sel ? "selected" : "" %>><%= e.get("nombre") %></option>
                                    <%  }
                                    } %>
                                </select>
                            </div>

                            <!-- MOTIVO DE CONSULTA (OPCIONAL) -->
                            <div class="form-field-group full-width">
                                <div class="form-label-row">
                                    <label class="form-label" for="selectMotivo">Motivo de consulta (opcional)</label>
                                    <span class="form-label-hint">Opcional · orientativo</span>
                                </div>
                                <select class="select-control" id="selectMotivo" name="idMotivo">
                                    <option value="">Selecciona un motivo</option>
                                    <% if (listaMotivos != null) {
                                        for (Map<String, Object> m : listaMotivos) {
                                            int mId = (Integer) m.get("idMotivo");
                                            boolean sel = (cita != null && cita.get("idMotivo") != null && ((Integer)cita.get("idMotivo")) == mId);
                                    %>
                                        <option value="<%= mId %>" <%= sel ? "selected" : "" %>><%= m.get("descripcion") %></option>
                                    <%  }
                                    } %>
                                </select>
                                <span class="field-helper-note">Los motivos se filtran según la especialidad elegida.</span>
                            </div>

                            <!-- MÉDICO (OPCIONAL) -->
                            <div class="form-field-group">
                                <div class="form-label-row">
                                    <label class="form-label" for="selectMedico">Médico (opcional)</label>
                                </div>
                                <select class="select-control" id="selectMedico" name="idMedico">
                                    <% if (listaMedicos != null && !listaMedicos.isEmpty()) {
                                        for (Map<String, Object> med : listaMedicos) {
                                            int medId = (Integer) med.get("idMedico");
                                            boolean sel = (cita != null && cita.get("idMedico") != null && ((Integer)cita.get("idMedico")) == medId);
                                    %>
                                        <option value="<%= medId %>" <%= sel ? "selected" : "" %>><%= med.get("nombreCompleto") %></option>
                                    <%  }
                                    } else { %>
                                        <option value="">Sin médicos disponibles para estos filtros</option>
                                    <% } %>
                                </select>
                                <output id="medicosEstado" class="catalogo-ayuda" for="selectMedico" aria-live="polite"></output>
                            </div>

                            <!-- FECHA -->
                            <div class="form-field-group">
                                <div class="form-label-row">
                                    <label class="form-label" for="inputFecha">Fecha</label>
                                </div>
                                <div class="date-input-wrapper">
                                    <input type="date" id="inputFecha" name="fecha" value="<%= defaultFecha %>" min="<%= minFecha %>" required>
                                </div>
                            </div>

                            <!-- TURNO PILLS -->
                            <div class="form-field-group full-width">
                                <div class="form-label-row">
                                    <label class="form-label">Turno</label>
                                </div>
                                <div class="turno-pills-row">
                                    <button type="button" class="turno-pill-btn <%= "manana".equals(turnoCita) ? "active" : "" %>" id="btnTurnoManana" onclick="seleccionarTurno('manana')">
                                        <i data-lucide="check" class="turno-check-icon"></i>
                                        <span>Mañana</span>
                                    </button>
                                    <button type="button" class="turno-pill-btn <%= "tarde".equals(turnoCita) ? "active" : "" %>" id="btnTurnoTarde" onclick="seleccionarTurno('tarde')">
                                        <i data-lucide="check" class="turno-check-icon"></i>
                                        <span>Tarde</span>
                                    </button>
                                    <button type="button" class="turno-pill-btn <%= (!"manana".equals(turnoCita) && !"tarde".equals(turnoCita)) ? "active" : "" %>" id="btnTurnoIndiferente" onclick="seleccionarTurno('indiferente')">
                                        <i data-lucide="check" class="turno-check-icon"></i>
                                        <span>Indiferente</span>
                                    </button>
                                </div>
                            </div>

                        </div>

                        <!-- ACCIONES DEL FORMULARIO -->
                        <div class="card-actions-row">
                            <a href="${pageContext.request.contextPath}/paciente/historial" class="btn-cancelar-reprog">
                                Cancelar reprogramación
                            </a>
                            <button type="button" class="btn-buscar-disp" onclick="irAlPaso2()">
                                <span>Buscar disponibilidad</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
                        </div>
                    </div>

                    <!-- ==========================================
                         PASO 2: ELEGIR HORARIO DISPONIBLE
                         ========================================== -->
                    <div class="reprogramar-card horarios-step-container" id="stepContent2">
                        <div class="horarios-doctor-header">
                            <div>
                                <div class="doc-info-title" id="displayDoctorName">Dr. Médico Especialista</div>
                                <div class="doc-info-sub" id="displayEspecialidadSede">Especialidad · Sede</div>
                            </div>
                            <div style="text-align: right;">
                                <div style="font-size: 14px; font-weight: 600; color: #1e3a8a;" id="displaySelectedDate">Fecha seleccionada</div>
                                <div style="font-size: 12.5px; color: #64748b;">Selecciona tu nuevo horario de atención</div>
                            </div>
                        </div>

                        <!-- BANNER INFORMATIVO Y SELECTOR RÁPIDO DE TURNO EN PASO 2 -->
                        <div class="horario-actual-badge" id="bannerHorarioActual" style="display: <%= (horaCita != null && !horaCita.isEmpty()) ? "flex" : "none" %>;">
                            <i data-lucide="info" style="width: 16px; height: 16px; flex-shrink: 0;"></i>
                            <span>Horario actual programado: <strong><%= horaCita != null ? horaCita : "" %> hrs</strong>. Selecciona tu nuevo horario preferido:</span>
                        </div>

                        <div class="horarios-step2-filter-bar">
                            <span class="filter-bar-label">Turno a consultar:</span>
                            <div class="turno-pills-row mini">
                                <button type="button" class="turno-pill-btn <%= (!"manana".equals(turnoCita) && !"tarde".equals(turnoCita)) ? "active" : "" %>" id="btnFiltroStep2Todos" onclick="cambiarFiltroStep2('indiferente')">
                                    <span>Todos</span>
                                </button>
                                <button type="button" class="turno-pill-btn <%= "manana".equals(turnoCita) ? "active" : "" %>" id="btnFiltroStep2Manana" onclick="cambiarFiltroStep2('manana')">
                                    <span>Mañana (08:00 - 12:30)</span>
                                </button>
                                <button type="button" class="turno-pill-btn <%= "tarde".equals(turnoCita) ? "active" : "" %>" id="btnFiltroStep2Tarde" onclick="cambiarFiltroStep2('tarde')">
                                    <span>Tarde (14:00 - 18:30)</span>
                                </button>
                            </div>
                        </div>

                        <!-- HORARIOS MAÑANA -->
                        <div class="horarios-grid-wrapper" id="seccionManana">
                            <div class="horarios-seccion-title">Turno Mañana (08:00 - 12:30)</div>
                            <div class="slots-grid" id="gridSlotsManana">
                                <!-- Generado por JS -->
                            </div>
                        </div>

                        <!-- HORARIOS TARDE -->
                        <div class="horarios-grid-wrapper" id="seccionTarde">
                            <div class="horarios-seccion-title">Turno Tarde (14:00 - 18:30)</div>
                            <div class="slots-grid" id="gridSlotsTarde">
                                <!-- Generado por JS -->
                            </div>
                        </div>

                        <div id="noSlotsMsg" style="display: none; padding: 24px; text-align: center; color: #64748b; background: #f8fafc; border-radius: 8px;">
                            No se encontraron cupos disponibles para el turno y fecha seleccionada. Por favor, intenta con otra fecha o médico.
                        </div>

                        <!-- ACCIONES PASO 2 -->
                        <div class="card-actions-row">
                            <button type="button" class="btn-cancelar-reprog" onclick="volverAlPaso1()">
                                <i data-lucide="chevron-left" style="width: 16px; height: 16px; margin-right: 4px;"></i>
                                Modificar búsqueda
                            </button>
                            <button type="button" class="btn-buscar-disp" id="btnContinuarConfirmacion" disabled style="opacity: 0.6; cursor: not-allowed;" onclick="irAlPaso3()">
                                <span>Continuar a confirmación</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
                        </div>
                    </div>

                    <!-- ==========================================
                         PASO 3: CONFIRMACIÓN CLÍNICA ATÓMICA
                         ========================================== -->
                    <div class="reprogramar-card confirmar-step-container" id="stepContent3">
                        <div style="margin-bottom: 24px;">
                            <h2 style="font-size: 18px; font-weight: 700; color: #0f172a; margin-bottom: 4px;">Verifica y confirma la reprogramación</h2>
                            <p style="font-size: 13.5px; color: #64748b;">Revisa los datos antes de aplicar el cambio definitivo en el sistema.</p>
                        </div>

                        <div class="resumen-comparativo-grid">
                            <!-- CITA ORIGINAL QUE SERÁ LIBERADA -->
                            <div class="comparativa-box original">
                                <span class="comp-tag">Cita actual a liberar</span>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Fecha y hora previa:</div>
                                    <div class="comp-item-value"><%= cita != null ? cita.get("fechaFormateadaBanner") : "--" %></div>
                                </div>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Especialista:</div>
                                    <div class="comp-item-value"><%= cita != null ? cita.get("medico") : "--" %></div>
                                </div>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Especialidad y sede:</div>
                                    <div class="comp-item-value"><%= cita != null ? cita.get("especialidad") : "--" %> · <%= cita != null ? cita.get("sede") : "--" %></div>
                                </div>
                            </div>

                            <!-- NUEVA CITA CONFIRMADA -->
                            <div class="comparativa-box nueva">
                                <span class="comp-tag">Nueva cita a agendar</span>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Nuevo horario seleccionado:</div>
                                    <div class="comp-item-value" id="summaryNuevaFechaHora">--</div>
                                </div>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Especialista:</div>
                                    <div class="comp-item-value" id="summaryNuevoMedico">--</div>
                                </div>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Especialidad y sede:</div>
                                    <div class="comp-item-value" id="summaryNuevaEspSede">--</div>
                                </div>
                                <div class="comp-item-row">
                                    <div class="comp-item-label">Motivo de consulta:</div>
                                    <div class="comp-item-value" id="summaryNuevoMotivo">Consulta general</div>
                                </div>
                            </div>
                        </div>

                        <div class="alerta-liberacion-box">
                            <i data-lucide="alert-circle" style="width: 20px; height: 20px; flex-shrink: 0;"></i>
                            <span><strong>Flujo clínico en tiempo real:</strong> Al confirmar, tu cupo previo quedará liberado automáticamente en el sistema y se confirmará tu nuevo horario médico sin costo adicional.</span>
                        </div>

                        <!-- ACCIONES PASO 3 -->
                        <div class="card-actions-row">
                            <button type="button" class="btn-cancelar-reprog" onclick="volverAlPaso2()">
                                <i data-lucide="chevron-left" style="width: 16px; height: 16px; margin-right: 4px;"></i>
                                Cambiar horario
                            </button>
                            <button type="submit" class="btn-buscar-disp" style="background-color: #16a34a;">
                                <i data-lucide="calendar-check" style="width: 16px; height: 16px;"></i>
                                <span>Confirmar reprogramación</span>
                            </button>
                        </div>
                    </div>

                </form>

                <!-- AYUDA INFERIOR -->
                <div class="footer-help-text">
                    ¿Necesita ayuda? <a href="#">Contactar call center citas</a>
                </div>

            </div>
        </div>

    </main>

    <script src="${pageContext.request.contextPath}/assets/js/catalogos-api.js"></script>
    <script>
        // Inicializar iconos lucide
        lucide.createIcons();

        let turnoActual = '<%= turnoCita %>';
        let horaCitaPrevia = '<%= horaCita != null ? horaCita : "" %>';
        let fechaCitaPrevia = '<%= defaultFecha %>';
        let horaSeleccionada = null;

        function seleccionarTurno(turno) {
            turnoActual = turno;
            const btnM = document.getElementById('btnTurnoManana');
            const btnT = document.getElementById('btnTurnoTarde');
            const btnI = document.getElementById('btnTurnoIndiferente');
            if (btnM) btnM.classList.toggle('active', turno === 'manana');
            if (btnT) btnT.classList.toggle('active', turno === 'tarde');
            if (btnI) btnI.classList.toggle('active', turno === 'indiferente');
            actualizarPillsStep2();
            lucide.createIcons();
        }

        function cambiarFiltroStep2(nuevoTurno) {
            seleccionarTurno(nuevoTurno);
            const selectMed = document.getElementById('selectMedico');
            const medId = selectMed ? selectMed.value : '';
            const fecha = document.getElementById('inputFecha').value;
            void cargarHorariosDisponibles(medId, fecha);
        }

        function actualizarPillsStep2() {
            const btnTodos = document.getElementById('btnFiltroStep2Todos');
            const btnManana = document.getElementById('btnFiltroStep2Manana');
            const btnTarde = document.getElementById('btnFiltroStep2Tarde');
            if (btnTodos) btnTodos.classList.toggle('active', turnoActual === 'indiferente');
            if (btnManana) btnManana.classList.toggle('active', turnoActual === 'manana');
            if (btnTarde) btnTarde.classList.toggle('active', turnoActual === 'tarde');
        }

        let medicosRequest = 0;
        let horariosRequest = 0;

        async function cargarMedicosDinamicos() {
            const version = ++medicosRequest;
            horariosRequest++;
            const espId = document.getElementById('selectEspecialidad').value;
            const sedeId = document.getElementById('selectSede').value;
            const select = document.getElementById('selectMedico');
            const info = document.getElementById('medicosEstado');
            const previous = select.value;
            select.disabled = true;
            select.replaceChildren();
            info.textContent = 'Consultando médicos…';
            try {
                const medicos = await ClinicaCatalogos.medicos('${pageContext.request.contextPath}', espId, sedeId);
                if (version !== medicosRequest) return;
                medicos.forEach(medico => {
                    const option = document.createElement('option');
                    option.value = medico.idMedico;
                    option.textContent = medico.nombreCompleto || 'Nombre no registrado';
                    option.selected = String(medico.idMedico) === previous;
                    select.appendChild(option);
                });
                select.disabled = medicos.length === 0;
                info.textContent = medicos.length ? '' : 'Sin médicos disponibles para estos filtros.';
            } catch (error) {
                if (version === medicosRequest) info.textContent = error.message;
            }
        }

        async function irAlPaso2() {
            const selectMed = document.getElementById('selectMedico');
            const medId = selectMed.value;
            if (selectMed.disabled || !/^[1-9]\d*$/.test(medId)) {
                document.getElementById('medicosEstado').textContent = 'Selecciona un médico disponible antes de continuar.';
                return;
            }
            const dateInput = document.getElementById('inputFecha');
            if (!dateInput.reportValidity()) return;
            const medNombre = selectMed.options[selectMed.selectedIndex] ? selectMed.options[selectMed.selectedIndex].text : 'Dr. Médico';
            
            const selectEsp = document.getElementById('selectEspecialidad');
            const espNombre = selectEsp.options[selectEsp.selectedIndex] ? selectEsp.options[selectEsp.selectedIndex].text : 'Especialidad';

            const selectSede = document.getElementById('selectSede');
            const sedeNombre = selectSede.options[selectSede.selectedIndex] ? selectSede.options[selectSede.selectedIndex].text : 'Sede Lima';

            const fecha = document.getElementById('inputFecha').value;

            document.getElementById('displayDoctorName').textContent = medNombre;
            document.getElementById('displayEspecialidadSede').textContent = espNombre + ' · ' + sedeNombre;
            document.getElementById('displaySelectedDate').textContent = 'Fecha: ' + fecha;

            actualizarPillsStep2();

            // Actualizar stepper
            document.getElementById('stepper1').className = 'step-item completed';
            document.getElementById('stepperCircle1').innerHTML = '<i data-lucide="check" style="width: 14px; height: 14px;"></i>';
            document.getElementById('stepper2').className = 'step-item active';
            document.getElementById('stepperCircle2').textContent = '2';

            // Alternar pantallas
            document.getElementById('stepContent1').style.display = 'none';
            document.getElementById('stepContent2').style.display = 'block';

            // Cargar slots de horarios
            await cargarHorariosDisponibles(medId, fecha);
            lucide.createIcons();
        }

        async function cargarHorariosDisponibles(medId, fecha) {
            const gridManana = document.getElementById('gridSlotsManana');
            const gridTarde = document.getElementById('gridSlotsTarde');
            const seccionManana = document.getElementById('seccionManana');
            const seccionTarde = document.getElementById('seccionTarde');
            const noSlots = document.getElementById('noSlotsMsg');
            const btnContinuar = document.getElementById('btnContinuarConfirmacion');

            gridManana.innerHTML = '';
            gridTarde.innerHTML = '';
            horaSeleccionada = null;
            document.getElementById('inputHoraSeleccionada').value = '';
            btnContinuar.disabled = true;
            btnContinuar.style.opacity = '0.6';
            btnContinuar.style.cursor = 'not-allowed';

            const version = ++horariosRequest;
            const turno = turnoActual;
            seccionManana.style.display = 'none';
            seccionTarde.style.display = 'none';
            noSlots.style.display = 'block';
            noSlots.textContent = 'Consultando horarios publicados…';
            let slots;
            try {
                slots = await ClinicaCatalogos.horarios('${pageContext.request.contextPath}', medId, fecha);
                if (version !== horariosRequest) return;
            } catch (error) {
                if (version === horariosRequest) noSlots.textContent = error.message;
                return;
            }
            noSlots.textContent = 'No hay horarios publicados disponibles para ese día y turno.';

            let countManana = 0;
            let countTarde = 0;

            slots.forEach(h => {
                const [hh] = h.split(':').map(Number);
                const esManana = hh < 13;

                if (turno === 'manana' && !esManana) return;
                if (turno === 'tarde' && esManana) return;

                const btn = document.createElement('button');
                btn.type = 'button';

                // Detectar si coincide con el horario actual de la cita previa en la misma fecha
                const esHorarioActual = (h === horaCitaPrevia && fecha === fechaCitaPrevia);
                if (esHorarioActual) {
                    btn.className = 'slot-btn current';
                    btn.innerHTML = '<span>' + h + '</span><span class="slot-tag-actual">Actual</span>';
                    btn.title = 'Tu horario actual programado';
                    btn.disabled = true;
                } else {
                    btn.className = 'slot-btn';
                    btn.textContent = h;
                    btn.onclick = () => seleccionarSlot(btn, h);
                }

                if (esManana) {
                    gridManana.appendChild(btn);
                    countManana++;
                } else {
                    gridTarde.appendChild(btn);
                    countTarde++;
                }
            });

            seccionManana.style.display = (turno !== 'tarde' && countManana > 0) ? 'block' : 'none';
            seccionTarde.style.display = (turno !== 'manana' && countTarde > 0) ? 'block' : 'none';
            noSlots.style.display = (countManana === 0 && countTarde === 0) ? 'block' : 'none';
        }

        function seleccionarSlot(btn, hora) {
            document.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            horaSeleccionada = hora;
            document.getElementById('inputHoraSeleccionada').value = hora;

            const btnContinuar = document.getElementById('btnContinuarConfirmacion');
            btnContinuar.disabled = false;
            btnContinuar.style.opacity = '1';
            btnContinuar.style.cursor = 'pointer';
        }

        function volverAlPaso1() {
            document.getElementById('stepContent2').style.display = 'none';
            document.getElementById('stepContent1').style.display = 'block';

            document.getElementById('stepper1').className = 'step-item active';
            document.getElementById('stepperCircle1').textContent = '1';
            document.getElementById('stepper2').className = 'step-item';
            document.getElementById('stepperCircle2').textContent = '2';
            lucide.createIcons();
        }

        function irAlPaso3() {
            if (!horaSeleccionada) return;

            const selectMed = document.getElementById('selectMedico');
            const medNombre = selectMed.options[selectMed.selectedIndex] ? selectMed.options[selectMed.selectedIndex].text : 'Dr. Médico';
            
            const selectEsp = document.getElementById('selectEspecialidad');
            const espNombre = selectEsp.options[selectEsp.selectedIndex] ? selectEsp.options[selectEsp.selectedIndex].text : 'Especialidad';

            const selectSede = document.getElementById('selectSede');
            const sedeNombre = selectSede.options[selectSede.selectedIndex] ? selectSede.options[selectSede.selectedIndex].text : 'Sede Lima';

            const selectMot = document.getElementById('selectMotivo');
            const motNombre = selectMot.selectedIndex > 0 ? selectMot.options[selectMot.selectedIndex].text : 'Consulta general orientativa';

            const fecha = document.getElementById('inputFecha').value;

            document.getElementById('summaryNuevaFechaHora').textContent = fecha + ' a las ' + horaSeleccionada + ' hrs';
            document.getElementById('summaryNuevoMedico').textContent = medNombre;
            document.getElementById('summaryNuevaEspSede').textContent = espNombre + ' (' + sedeNombre + ')';
            document.getElementById('summaryNuevoMotivo').textContent = motNombre;

            // Actualizar stepper
            document.getElementById('stepper2').className = 'step-item completed';
            document.getElementById('stepperCircle2').innerHTML = '<i data-lucide="check" style="width: 14px; height: 14px;"></i>';
            document.getElementById('stepper3').className = 'step-item active';
            document.getElementById('stepperCircle3').textContent = '3';

            document.getElementById('stepContent2').style.display = 'none';
            document.getElementById('stepContent3').style.display = 'block';
            lucide.createIcons();
        }

        function volverAlPaso2() {
            document.getElementById('stepContent3').style.display = 'none';
            document.getElementById('stepContent2').style.display = 'block';

            document.getElementById('stepper2').className = 'step-item active';
            document.getElementById('stepperCircle2').textContent = '2';
            document.getElementById('stepper3').className = 'step-item';
            document.getElementById('stepperCircle3').textContent = '3';
            lucide.createIcons();
        }
    </script>
</body>
</html>
