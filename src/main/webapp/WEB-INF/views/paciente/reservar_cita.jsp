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

    List<Map<String, Object>> listaEspecialidades = (List<Map<String, Object>>) request.getAttribute("listaEspecialidades");
    List<Map<String, Object>> listaSedes = (List<Map<String, Object>>) request.getAttribute("listaSedes");

    String preEspId = (String) request.getAttribute("preEspId");
    String preMedId = (String) request.getAttribute("preMedId");
    String preEspNombre = (String) request.getAttribute("preEspNombre");
    String preMedNombre = (String) request.getAttribute("preMedNombre");

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    pe.edu.utp.clinica.dao.CitaDAO citaDAOBell = new pe.edu.utp.clinica.dao.CitaDAO();
    int numNotifSinLeer = request.getAttribute("numNotifSinLeer") != null 
        ? (Integer) request.getAttribute("numNotifSinLeer") 
        : (usuario != null ? citaDAOBell.contarNotificacionesSinLeer(usuario.getIdUsuario()) : 0);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Programar Cita - Portal del Paciente | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/buscar-cita.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/catalogos-api.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orientacion-catalogo.css">
</head>
<body>

    <!-- SIDEBAR INSTITUCIONAL -->
    <% String seccionPaciente = "citas"; %>
    <%@ include file="/WEB-INF/jspf/paciente-sidebar.jspf" %>

    <!-- CONTENIDO PRINCIPAL -->
    <main class="main-content">
        <c:if test="${not empty sessionScope.agendaError}">
            <p role="alert" class="catalogo-estado"><c:out value="${sessionScope.agendaError}"/></p>
            <c:remove var="agendaError" scope="session"/>
        </c:if>


        <!-- HEADER INSTITUCIONAL -->
        <header class="dash-top-header">
            <nav class="breadcrumb-trail">
                <span>Paciente</span> <span class="bc-sep">/</span> <strong>Buscar cita</strong>
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
            <div class="buscar-wrapper">

                <!-- CABECERA DE PÁGINA (TÍTULO, SUBTÍTULO Y BOTÓN VOLVER) -->
                <div class="page-title-row-with-back" id="pageTitleRow">
                    <div>
                        <h1 class="page-main-title" id="pageMainTitle">Programar Cita</h1>
                        <p class="page-main-subtitle" id="pageMainSubtitle">Te ayudamos a encontrar la atención adecuada o puedes reservar directamente con la especialidad que necesitas.</p>
                    </div>
                    <button type="button" id="btnVolverModos" class="btn-back-pill" onclick="volverASeleccionModos()" style="display: none;">
                        <i data-lucide="arrow-left" style="width: 16px; height: 16px;"></i>
                        <span>Volver</span>
                    </button>
                </div>

                <!-- ======================================================= -->
                <!-- PANTALLA 1: ELECCIÓN DE MODO (IA vs DIRECTO)           -->
                <!-- ======================================================= -->
                <div id="viewModeSelector">
                    <div class="mode-selection-grid">
                        
                        <!-- TARJETA 1: ORIENTACIÓN POR ESPECIALIDAD -->
                        <div class="mode-card" onclick="iniciarModoIA()">
                            <div>
                                <div class="mode-card-top-row">
                                    <div class="mode-card-icon">
                                        <i data-lucide="bot" style="width: 26px; height: 26px;"></i>
                                    </div>
                                    <span class="mode-badge-pill badge-pill-mint">
                                        Prototipo · IA opcional
                                    </span>
                                </div>

                                <div class="mode-kicker">ORIENTACIÓN POR ESPECIALIDAD</div>
                                <h2 class="mode-card-title">No sé qué especialidad necesito</h2>
                                <p class="mode-card-desc">
                                    En esta demostración académica puedes probar casos ficticios para sugerir una especialidad del catálogo. La sugerencia no asigna prioridad clínica.
                                </p>
                            </div>

                            <button type="button" class="btn-card-action">
                                <span>Probar un caso ficticio</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
                        </div>

                        <!-- TARJETA 2: RESERVA DIRECTA POR ESPECIALIDAD -->
                        <div class="mode-card" onclick="iniciarModoDirecto()">
                            <div>
                                <div class="mode-card-top-row">
                                    <div class="mode-card-icon">
                                        <i data-lucide="calendar" style="width: 26px; height: 26px;"></i>
                                    </div>
                                    <span class="mode-badge-pill badge-pill-soft">
                                        Reserva directa
                                    </span>
                                </div>

                                <div class="mode-kicker">RESERVA DIRECTA POR ESPECIALIDAD</div>
                                <h2 class="mode-card-title">Ya sé qué especialidad necesito</h2>
                                <p class="mode-card-desc">
                                    Elige directamente la especialidad, el médico de tu preferencia y el horario que mejor se acomode a ti.
                                </p>
                            </div>

                            <button type="button" class="btn-card-action">
                                <span>Ver especialidades</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
                        </div>

                    </div>

                    <!-- DISCLAIMER INFERIOR -->
                    <div class="disclaimer-banner">
                        <i data-lucide="info" style="width: 20px; height: 20px;"></i>
                        <span>La orientación por IA no reemplaza una evaluación médica. Si presentas una emergencia, acude al centro de salud más cercano.</span>
                    </div>
                </div>

                <!-- ======================================================= -->
                <!-- PANEL ASISTENTE DE SÍNTOMAS IA                          -->
                <!-- ======================================================= -->
                <div id="aiTriagePanel" class="ai-triage-box">
                    <div class="ai-triage-header">
                        <div class="ai-triage-header-info">
                            <div class="ai-badge-robot">
                                <i data-lucide="bot" style="width: 24px; height: 24px;"></i>
                            </div>
                            <div>
                                <h3 style="font-size: 17px; font-weight: 700; color: #1e3a8a;">Prototipo de orientación por especialidad</h3>
                                <p style="font-size: 13px; color: #64748b;">Demostración académica: utiliza únicamente casos ficticios, sin nombres, DNI ni datos reales de salud.</p>
                            </div>
                        </div>
                        <button type="button" onclick="cerrarPanelIA()" style="background: none; border: none; cursor: pointer; color: #64748b;" title="Cerrar">
                            <i data-lucide="x" style="width: 20px; height: 20px;"></i>
                        </button>
                    </div>

                    <div style="margin-bottom: 12px;">
                        <label for="aiSintomasInput" style="font-size: 13px; font-weight: 600; color: var(--text-dark); display: block; margin-bottom: 6px;">
                            ¿Qué molestias describe el caso ficticio?
                        </label>
                        <textarea maxlength="1200" oninput="invalidarOrientacion()" id="aiSintomasInput" class="ai-symptom-textarea" placeholder="Describe aquí un caso ficticio. Sin Gemini configurado solo funcionan los ejemplos de demostración."></textarea>
                    </div>

                    <p id="aiAvisoDemo" class="ai-demo-note">Prototipo educativo. Los ejemplos locales son predefinidos y no se presentan como respuestas de Gemini. El texto libre requiere configurar el proveedor.</p>
                    <label class="ai-demo-confirmacion"><input type="checkbox" id="aiCasoFicticio" onchange="invalidarOrientacion()"> Confirmo que este es un caso ficticio sin información personal.</label>
                    <output class="ai-demo-status" id="aiEstadoOrientacion" aria-live="polite"></output>
                    <p class="ai-demo-error" id="aiErrorOrientacion" role="alert" hidden></p>
                    <div class="quick-symptoms-label">Ejemplos ficticios para comprobar el flujo:</div>
                    <div class="quick-chips-wrapper">
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Caso ficticio: picazón y manchas en la piel')">Ejemplo: piel</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Caso ficticio: molestia articular en la rodilla')">Ejemplo: rodilla</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Caso ficticio: acidez y molestia estomacal')">Ejemplo: digestivo</button>
                    </div>

                    <div class="ai-demo-actions">
                        <button type="button" id="btnAnalizarIA" class="btn-card-action btn-card-royal" onclick="ejecutarAnalisisIA()">
                            <i data-lucide="sparkles" style="width: 16px; height: 16px;"></i>
                            <span>Consultar especialidad</span>
                        </button>
                        <button type="button" class="btn-back-pill" onclick="cerrarPanelIA()">Volver</button>
                    </div>

                    <!-- RESULTADO DEL ANÁLISIS DE IA -->
                    <div id="aiResultadoBox" class="ai-response-result">
                        <div style="display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 12px;">
                            <div>
                                <div style="display: flex; gap: 8px; align-items: center; margin-bottom: 6px;">
                                    <span class="mode-badge-pill badge-pill-mint" style="margin-bottom: 0;">
                                        <i data-lucide="sparkles" style="width: 12px; height: 12px;"></i>
                                        Especialidad sugerida: <strong id="aiEspecialidadNombre" style="margin-left: 4px;"></strong>
                                    </span>

                                </div>
                                <h4 style="font-size: 15.5px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px;">Resultado de la demostración</h4>
                                <p id="aiExplicacionTexto" style="font-size: 13.5px; color: #334155; line-height: 1.5;">

                                </p>
                            </div>
                        </div>

                        <div class="ai-demo-actions ai-demo-actions-result">
                            <button type="button" id="aiAplicarSugerencia" class="btn-card-action btn-card-royal" onclick="aplicarRecomendacionIA()">
                                <span>Continuar con esta especialidad</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
                            <button type="button" class="btn-back-pill" onclick="iniciarModoDirecto()">Elegir otra manualmente</button>
                        </div>
                    </div>
                </div>

                <!-- ======================================================= -->
                <!-- WIZARD STEPPER Y FLUJO DE BÚSQUEDA                     -->
                <!-- ======================================================= -->
                <div id="wizardFlowSection" style="display: none;">

                    <!-- STEPPER (Alineado a la izquierda según diseño institucional) -->
                    <div class="wizard-stepper-container">
                        <div class="wiz-step active" id="wizStep1">
                            <div class="wiz-num">1</div>
                            <span>Buscar</span>
                        </div>
                        <div class="wiz-line" id="wizLine1"></div>
                        <div class="wiz-step" id="wizStep2">
                            <div class="wiz-num">2</div>
                            <span>Elegir horario</span>
                        </div>
                        <div class="wiz-line" id="wizLine2"></div>
                        <div class="wiz-step" id="wizStep3">
                            <div class="wiz-num">3</div>
                            <span>Confirmar</span>
                        </div>
                    </div>

                    <!-- PASO 1: BÚSQUEDA DIRECTA -->
                    <div id="step1Content">
                        <div class="card-filter-box">
                            
                            <!-- FILA 1: SEDE Y ESPECIALIDAD -->
                            <div class="filter-row-2col">
                                <div class="filter-field">
                                    <label for="selectSede">Sede *</label>
                                    <select id="selectSede" class="filter-control" onchange="actualizarMedicosPorEspecialidad()">
                                        <option value="" disabled selected>Escoja sede</option>
                                        <% if (listaSedes != null && !listaSedes.isEmpty()) {
                                            for (Map<String, Object> sede : listaSedes) { 
                                        %>
                                                <option value="<%= sede.get("id") %>"><%= sede.get("nombre") %></option>
                                        <%  }
                                        } else { %>
                                            <option value="1">Sede San Borja</option>
                                            <option value="2">Sede Lima</option>
                                            <option value="3">Sede Surco</option>
                                        <% } %>
                                    </select>
                                </div>

                                <div class="filter-field">
                                    <label for="selectEspecialidad">Especialidad *</label>
                                    <select id="selectEspecialidad" class="filter-control" onchange="actualizarEspecialidadYMotivos()">
                                        <option value="" disabled selected>Escoja especialidad</option>
                                        <% if (listaEspecialidades != null && !listaEspecialidades.isEmpty()) {
                                            for (Map<String, Object> esp : listaEspecialidades) { 
                                        %>
                                                <option value="<%= esp.get("id") %>" data-nombre="<%= esp.get("nombre") %>">
                                                    <%= esp.get("nombre") %>
                                                </option>
                                        <%  }
                                        } else { %>
                                            <option value="1" data-nombre="Cardiología">Cardiología</option>
                                            <option value="2" data-nombre="Neurología">Neurología</option>
                                            <option value="3" data-nombre="Medicina General">Medicina General</option>
                                        <% } %>
                                    </select>
                                </div>
                            </div>

                            <!-- FILA 2: MOTIVO DE CONSULTA -->
                            <div class="filter-row-full">
                                <div class="filter-field">
                                    <div class="motivo-label-row">
                                        <label for="selectMotivo">Motivo de consulta (opcional)</label>
                                        <span class="motivo-hint-tag">Opcional · orientativo</span>
                                    </div>
                                    <select id="selectMotivo" class="filter-control">
                                        <option value="">Selecciona un motivo</option>
                                    </select>
                                    <div class="motivo-footer-hint">Los motivos se filtran según la especialidad elegida.</div>
                                </div>
                            </div>

                            <!-- FILA 3: MÉDICO Y FECHA -->
                            <div class="filter-row-2col filter-row-med-date">
                                <div class="filter-field">
                                    <label for="selectMedico">Médico (opcional)</label>
                                    <select id="selectMedico" class="filter-control">
                                        <option value="0">Cualquier médico</option>
                                    </select>
                                </div>

                                <div class="filter-field">
                                    <label for="inputFecha">Fecha</label>
                                    <input type="date" id="inputFecha" class="filter-control filter-control-date">
                                </div>
                            </div>

                            <!-- FILA 4: TURNO -->
                            <div class="turno-inline-row">
                                <span class="turno-inline-label">Turno</span>
                                <div class="turno-pills-wrap">
                                    <button type="button" class="turno-pill selected" onclick="seleccionarTurno(this, 'MANANA')">
                                        <i data-lucide="check" style="width: 14px; height: 14px;"></i>
                                        <span>Mañana</span>
                                    </button>
                                    <button type="button" class="turno-pill" onclick="seleccionarTurno(this, 'TARDE')">
                                        <span>Tarde</span>
                                    </button>
                                    <button type="button" class="turno-pill" onclick="seleccionarTurno(this, 'TODOS')">
                                        <span>Indiferente</span>
                                    </button>
                                </div>
                            </div>

                            <!-- FILA 5: BOTONES DE ACCIÓN -->
                            <div class="filter-actions-bar">
                                <button type="button" class="btn-limpiar-filtros" onclick="limpiarFiltros()">
                                    Limpiar filtros
                                </button>

                                <button type="button" class="btn-buscar-disponibilidad" onclick="buscarHorariosDisponibles()">
                                    <span>Buscar disponibilidad</span>
                                    <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                                </button>
                            </div>

                        </div>

                        <!-- ENLACE DE AYUDA -->
                        <div class="help-callcenter-row">
                            ¿Necesita ayuda? <a href="tel:016160000">Contactar call center citas</a>
                        </div>
                    </div>

                    <!-- PASO 2: ELEGIR HORARIO (RESULTADOS DE CITAS DISPONIBLES) -->
                    <div id="step2Content" style="display: none;">
                        
                        <!-- BARRA DE FILTROS APLICADOS -->
                        <div class="filters-summary-bar">
                            <div class="filters-pills-left">
                                <span class="filters-label-text">Filtros:</span>
                                <span class="filter-pill-tag" id="pillSede">Sede San Borja</span>
                                <span class="filter-pill-tag" id="pillEspecialidad">Cardiología</span>
                                <span class="filter-pill-tag" id="pillTurno">Turno mañana</span>
                            </div>
                            <button type="button" class="btn-modificar-filtros" onclick="volverAPaso1()">
                                <i data-lucide="sliders-horizontal" style="width: 15px; height: 15px;"></i>
                                <span>Modificar filtros</span>
                            </button>
                        </div>

                        <!-- SELECTOR DE SEMANA Y DÍAS -->
                        <div class="week-selector-card">
                            <div class="week-header-row">
                                <span class="week-title-label" id="weekRangeLabel">Semana del 12 al 18 de octubre de 2026</span>
                                <div class="week-nav-btns">
                                    <button type="button" class="btn-week-nav" onclick="navegarSemana(-1)" title="Semana anterior">
                                        <i data-lucide="chevron-left" style="width: 16px; height: 16px;"></i>
                                    </button>
                                    <button type="button" class="btn-week-nav" onclick="navegarSemana(1)" title="Semana siguiente">
                                        <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                                    </button>
                                </div>
                            </div>

                            <div class="week-days-grid" id="weekDaysGrid">
                                <!-- Generado dinámicamente con los 7 días y cupos -->
                            </div>
                        </div>

                        <!-- LISTADO DINÁMICO DE MÉDICOS Y HORARIOS -->
                        <div id="doctorResultsContainer" class="doctor-results-list">
                            <!-- Inyectado por JS con tarjetas de médicos y chips -->
                        </div>

                        <!-- BANNER LISTA DE ESPERA -->
                        <div class="waitlist-callout-card">
                            <div class="waitlist-callout-left">
                                <div class="waitlist-icon-circle">
                                    <i data-lucide="clock" style="width: 20px; height: 20px;"></i>
                                </div>
                                <div>
                                    <div class="waitlist-callout-title">No encontramos más citas disponibles para los criterios seleccionados</div>
                                    <div class="waitlist-callout-sub">Sin costo adicional · confirmación vía SMS / email</div>
                                </div>
                            </div>
                            <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="btn-waitlist-action">
                                Registrarme en lista de espera
                            </a>
                        </div>

                    </div>

                    <!-- PASO 3: CONFIRMACIÓN Y RESERVA (FINALIZAR RESERVA) -->
                    <div id="step3Content" style="display: none;">
                        
                        <!-- BANNER TEMPORAL DE CUPO RESERVADO (5 MINUTOS) -->
                        <div class="reservation-hold-banner">
                            <div class="hold-banner-icon">
                                <i data-lucide="clock" style="width: 22px; height: 22px;"></i>
                            </div>
                            <div>
                                <div class="hold-banner-title">
                                    Cupo reservado temporalmente · Tiempo para completar la reserva: <span id="reservaCountdown">05:00</span>
                                </div>
                                <div class="hold-banner-desc">
                                    Si no terminas a tiempo, el cupo se libera y vuelves a los resultados.
                                </div>
                            </div>
                        </div>

                        <!-- FORMULARIO DE ENVÍO POST -->
                        <form id="formReservaFinal" action="${pageContext.request.contextPath}/paciente/reservar" method="POST">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="idMedico" id="finalIdMedico" value="">
                            <input type="hidden" name="fecha" id="finalFecha" value="">
                            <input type="hidden" name="hora" id="finalHora" value="">
                            <input type="hidden" name="urgencia" id="finalUrgencia" value="MEDIA">

                            <!-- GRID DE DOS TARJETAS -->
                            <div class="finalizar-reserva-grid">
                                
                                <!-- TARJETA IZQUIERDA: RESUMEN DE LA CITA -->
                                <div class="finalizar-card">
                                    <h3 class="finalizar-card-title">Resumen de la cita</h3>
                                    <div class="resumen-filas-list">
                                        <div class="resumen-row-item">
                                            <span class="resumen-row-label">Especialidad</span>
                                            <span class="resumen-row-val" id="resumenEspecialidad">Cardiología</span>
                                        </div>
                                        <div class="resumen-row-item">
                                            <span class="resumen-row-label">Médico / CMP</span>
                                            <span class="resumen-row-val" id="resumenMedicoCMP">—</span>
                                        </div>
                                        <div class="resumen-row-item">
                                            <span class="resumen-row-label">Sede y consultorio</span>
                                            <span class="resumen-row-val" id="resumenSedeConsultorio">Sede San Borja · Consultorio 304</span>
                                        </div>
                                        <div class="resumen-row-item">
                                            <span class="resumen-row-label">Fecha y hora</span>
                                            <span class="resumen-row-val" id="resumenFechaHoraFormateada">Jueves 15 oct 2026 · 09:00</span>
                                        </div>
                                    </div>
                                </div>

                                <!-- TARJETA DERECHA: DATOS DE LA RESERVA -->
                                <div class="finalizar-card">
                                    <h3 class="finalizar-card-title">Datos de la reserva</h3>

                                    <div class="form-group-motivo">
                                        <label for="selectMotivoPaso3" class="finalizar-field-label">Motivo de consulta *</label>
                                        <div class="select-with-icon-wrap">
                                            <select id="selectMotivoPaso3" name="motivo" class="finalizar-select-control" required>
                                                <option value="" disabled selected>Selecciona un motivo</option>
                                            </select>
                                            <i data-lucide="chevron-down" class="select-chevron-icon"></i>
                                        </div>
                                        <div class="motivo-field-footnote">Solo se muestran los motivos de la especialidad elegida.</div>
                                    </div>

                                    <div class="notif-channels-section">
                                        <label class="finalizar-field-label">Recibir los avisos de esta cita por:</label>
                                        <div class="notif-channels-group">
                                            <label class="notif-channel-card active" id="labelNotifSms">
                                                <input type="checkbox" name="notifSms" id="checkNotifSms" checked onchange="toggleNotifChannel('sms')">
                                                <i data-lucide="smartphone" class="notif-icon"></i>
                                                <span class="notif-text">SMS / WhatsApp</span>
                                            </label>
                                            <label class="notif-channel-card" id="labelNotifEmail">
                                                <input type="checkbox" name="notifEmail" id="checkNotifEmail" onchange="toggleNotifChannel('email')">
                                                <i data-lucide="mail" class="notif-icon"></i>
                                                <span class="notif-text">Correo electrónico</span>
                                            </label>
                                        </div>
                                    </div>

                                </div>

                            </div>

                            <!-- ACCIONES INFERIORES: VOLVER A RESULTADOS / FINALIZAR RESERVA -->
                            <div class="finalizar-actions-bar">
                                <button type="button" class="btn-volver-resultados" onclick="volverAPaso2()">
                                    <i data-lucide="chevron-left" style="width: 17px; height: 17px;"></i>
                                    <span>Volver a resultados</span>
                                </button>

                                <button type="submit" class="btn-finalizar-reserva" id="btnConfirmarReserva">
                                    <span>Finalizar reserva</span>
                                    <i data-lucide="check" style="width: 18px; height: 18px;"></i>
                                </button>
                            </div>

                        </form>
                    </div>

                    <!-- PASO 4: ÉXITO - TU CITA ESTÁ RESERVADA -->
                    <div id="stepSuccessContent" style="display: none;">
                        <div class="reserva-success-wrapper">
                            <div class="reserva-success-card">
                                
                                <div class="success-check-circle">
                                    <i data-lucide="check" style="width: 32px; height: 32px; stroke-width: 2.5;"></i>
                                </div>

                                <h2 class="success-main-title">Tu cita está reservada</h2>

                                <div class="success-code-pill">
                                    <span>Código de cita:</span>
                                    <strong id="successCodigoCita">—</strong>
                                </div>

                                <div class="success-details-list">
                                    <div class="success-row-item">
                                        <span class="success-row-label">Especialidad</span>
                                        <span class="success-row-val" id="successEspecialidad">Cardiología</span>
                                    </div>
                                    <div class="success-row-item">
                                        <span class="success-row-label">Médico / CMP</span>
                                        <span class="success-row-val" id="successMedicoCMP">—</span>
                                    </div>
                                    <div class="success-row-item">
                                        <span class="success-row-label">Sede y consultorio</span>
                                        <span class="success-row-val" id="successSedeConsultorio">Sede San Borja · Consultorio 304</span>
                                    </div>
                                    <div class="success-row-item">
                                        <span class="success-row-label">Fecha y hora</span>
                                        <span class="success-row-val" id="successFechaHora">Jueves 15 oct 2026 · 09:00</span>
                                    </div>
                                    <div class="success-row-item">
                                        <span class="success-row-label">Motivo de consulta</span>
                                        <span class="success-row-val" id="successMotivo">Control de presión arterial</span>
                                    </div>
                                </div>

                                <div class="success-whatsapp-banner">
                                    <i data-lucide="smartphone" class="success-whatsapp-icon"></i>
                                    <div>
                                        <div class="success-whatsapp-title">Reserva registrada</div>
                                        <div class="success-whatsapp-desc">
                                            Enviamos el detalle a +51 987 ••• 482 (WhatsApp). 48 h antes te pediremos confirmar tu asistencia.
                                        </div>
                                    </div>
                                </div>

                                <div class="success-actions-row">
                                    <a href="${pageContext.request.contextPath}/paciente/historial" class="btn-success-primary">
                                        <i data-lucide="calendar" style="width: 17px; height: 17px;"></i>
                                        <span>Ir a mis citas</span>
                                    </a>
                                    <a href="${pageContext.request.contextPath}/paciente/dashboard" class="btn-success-secondary">
                                        <i data-lucide="home" style="width: 17px; height: 17px;"></i>
                                        <span>Volver al inicio</span>
                                    </a>
                                </div>

                            </div>
                        </div>
                    </div>

                </div>

            </div>
        </div>

    </main>

    <!-- SCRIPTS Y LÓGICA DE CONTROL -->
    <script src="${pageContext.request.contextPath}/assets/js/catalogos-api.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/reservas-api.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            lucide.createIcons();

            // Configurar fecha mínima de hoy en inputFecha
            const today = new Date().toISOString().split('T')[0];
            const dateInput = document.getElementById('inputFecha');
            if (dateInput) {
                dateInput.min = today;
                dateInput.value = today;
            }

            // Si vino con parámetros preestablecidos (ej. desde Dashboard o tarjeta médica)
            const preEspId = "<%= preEspId != null ? preEspId : "" %>";
            const preMedId = "<%= preMedId != null ? preMedId : "" %>";
            if (preEspId && preEspId.trim() !== '') {
                iniciarModoDirecto();
                const selEsp = document.getElementById('selectEspecialidad');
                if (selEsp) {
                    selEsp.value = preEspId;
                    actualizarMedicosPorEspecialidad(preMedId);
                }
            } else {
                actualizarEspecialidadYMotivos();
            }

            // Listener para motivo en Paso 3
            const selMotP3 = document.getElementById('selectMotivoPaso3');
            if (selMotP3) {
                selMotP3.addEventListener('change', function() {
                    state.motivo = this.value;
                });
            }

            // Envío asíncrono de la reserva para mostrar de inmediato el comprobante Tu cita está reservada
            const formFinal = document.getElementById('formReservaFinal');
            if (formFinal) {
                formFinal.addEventListener('submit', function(e) {
                    e.preventDefault();

                    if (selMotP3 && (!selMotP3.value || selMotP3.value.trim() === '')) {
                        alert('Por favor selecciona un motivo de consulta para tu cita.');
                        selMotP3.focus();
                        return false;
                    }

                    const btnSubmit = document.getElementById('btnConfirmarReserva');
                    if (btnSubmit) {
                        btnSubmit.disabled = true;
                        btnSubmit.innerHTML = '<span>Procesando reserva...</span>';
                    }

                    const formData = new FormData(formFinal);
                    const params = new URLSearchParams(formData);

                    ClinicaReservas.reservar(formFinal.action, params,
                        document.querySelector('meta[name="csrf-token"]').content)
                    .then(data => mostrarPantallaExito(data.codigoCita))
                    .catch(error => {
                        alert(error.message);
                        if (btnSubmit) {
                            btnSubmit.disabled = false;
                            btnSubmit.textContent = 'Finalizar reserva';
                        }
                    });
                });
            }
        });

        // Estado global de la vista
        let state = {
            modo: 'NINGUNO', // 'IA' o 'DIRECTO'
            pasoActual: 1,
            especialidadId: '',
            especialidadNombre: '',
            sedeId: '0',
            sedeNombre: 'Todas las sedes',
            medicoId: '0',
            medicoNombre: '',
            fecha: '',
            hora: '',
            urgencia: 'MEDIA',
            turno: 'TODOS',
            motivo: ''
        };

        // --- NAVEGACIÓN DE MODOS ---
        function iniciarModoIA() {
            state.modo = 'IA';
            state.pasoActual = 1;
            document.getElementById('pageMainTitle').textContent = 'Orientación por especialidad';
            document.getElementById('pageMainSubtitle').textContent = 'Demostración académica: utiliza únicamente casos ficticios, sin nombres, DNI ni datos reales de salud.';
            document.getElementById('viewModeSelector').style.display = 'none';
            document.getElementById('aiTriagePanel').style.display = 'block';
            document.getElementById('wizardFlowSection').style.display = 'none';
            document.getElementById('aiResultadoBox').style.display = 'none';
            const btnVolver = document.getElementById('btnVolverModos');
            if (btnVolver) btnVolver.style.display = 'inline-flex';
            document.getElementById('aiSintomasInput').focus();
            lucide.createIcons();
        }

        function cerrarPanelIA() {
            invalidarOrientacion();
            document.getElementById('aiTriagePanel').style.display = 'none';
            volverASeleccionModos();
        }

        function iniciarModoDirecto(origen = 'DIRECTO') {
            invalidarOrientacion();
            state.modo = origen;
            document.getElementById('pageMainTitle').textContent = 'Buscar cita';
            document.getElementById('pageMainSubtitle').textContent = 'Busca los horarios disponibles según tus preferencias';
            document.getElementById('viewModeSelector').style.display = 'none';
            document.getElementById('aiTriagePanel').style.display = 'none';
            document.getElementById('wizardFlowSection').style.display = 'block';
            const btnVolver = document.getElementById('btnVolverModos');
            if (btnVolver) btnVolver.style.display = 'inline-flex';
            actualizarEspecialidadYMotivos();
            mostrarPaso(1);
            lucide.createIcons();
        }

        function volverASeleccionModos() {
            invalidarOrientacion();
            if (state.pasoActual === 2) {
                volverAPaso1();
                return;
            }
            if (state.pasoActual === 3) {
                volverAPaso2();
                return;
            }
            if (state.modo === 'IA' && document.getElementById('wizardFlowSection').style.display !== 'none') {
                iniciarModoIA();
                return;
            }
            state.modo = 'NINGUNO';
            document.getElementById('pageMainTitle').textContent = 'Programar Cita';
            document.getElementById('pageMainSubtitle').textContent = 'Te ayudamos a encontrar la atención adecuada o puedes reservar directamente con la especialidad que necesitas.';
            document.getElementById('wizardFlowSection').style.display = 'none';
            document.getElementById('aiTriagePanel').style.display = 'none';
            document.getElementById('viewModeSelector').style.display = 'block';
            const btnVolver = document.getElementById('btnVolverModos');
            if (btnVolver) btnVolver.style.display = 'none';
            lucide.createIcons();
        }

        let sugerenciaOrientacion = null;
        let peticionOrientacion = null;

        function invalidarOrientacion() {
            peticionOrientacion?.abort();
            peticionOrientacion = null;
            sugerenciaOrientacion = null;
            document.getElementById('aiResultadoBox').style.display = 'none';
            document.getElementById('aiEstadoOrientacion').textContent = '';
            document.getElementById('aiErrorOrientacion').hidden = true;
            const boton = document.getElementById('btnAnalizarIA');
            boton.disabled = false;
            boton.querySelector('span').textContent = 'Consultar especialidad';
        }

        function agregarSintoma(boton, texto) {
            invalidarOrientacion();
            document.getElementById('aiSintomasInput').value = texto;
        }

        async function ejecutarAnalisisIA() {
            const boton = document.getElementById('btnAnalizarIA');
            if (boton.disabled) return;
            invalidarOrientacion();
            const error = document.getElementById('aiErrorOrientacion');
            const estado = document.getElementById('aiEstadoOrientacion');
            if (!document.getElementById('aiCasoFicticio').checked) {
                error.textContent = 'Confirma que estás usando un caso ficticio, sin datos personales.';
                error.hidden = false;
                return;
            }
            const texto = document.getElementById('aiSintomasInput').value.trim();
            if (texto.length < 12 || texto.length > 1200) {
                error.textContent = 'Escribe un caso ficticio de entre 12 y 1200 caracteres.';
                error.hidden = false;
                return;
            }
            const controller = new AbortController();
            peticionOrientacion = controller;
            const timeout = setTimeout(() => controller.abort(), 15000);
            boton.disabled = true;
            boton.querySelector('span').textContent = 'Consultando…';
            estado.textContent = 'Consultando el catálogo de especialidades…';
            try {
                const respuesta = await fetch('${pageContext.request.contextPath}/paciente/orientacion', {
                    method: 'POST', credentials: 'same-origin', signal: controller.signal,
                    headers: {Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded'},
                    body: new URLSearchParams({relato: texto, casoFicticio: 'true', csrfToken: document.querySelector('meta[name="csrf-token"]').content})
                });
                const contenido = await respuesta.json();
                if (peticionOrientacion !== controller) return;
                if (!respuesta.ok) throw new Error(contenido.message || 'No se pudo obtener la sugerencia.');
                const resultado = contenido.data;
                if (resultado.tipo === 'ACLARAR') { estado.textContent = resultado.explicacion; return; }
                const opciones = [...document.getElementById('selectEspecialidad').options];
                if (resultado.tipo !== 'SUGERENCIA' || !opciones.some(o => o.value === String(resultado.idEspecialidad))) throw new Error('La especialidad no está disponible en este catálogo. Elige manualmente.');
                sugerenciaOrientacion = resultado;
                document.getElementById('aiEspecialidadNombre').textContent = resultado.especialidad;
                document.getElementById('aiExplicacionTexto').textContent = resultado.explicacion;
                estado.textContent = resultado.proveedor === 'EJEMPLO_LOCAL' ? 'Ejemplo local predefinido; Gemini no fue consultado.' : 'Sugerencia demostrativa generada con Gemini y validada contra el catálogo.';
                document.getElementById('aiResultadoBox').style.display = 'block';
            } catch (fallo) {
                if (peticionOrientacion !== controller) return;
                error.textContent = fallo instanceof Error && fallo.name === 'Error' ? fallo.message : 'No se pudo completar la consulta. Puedes elegir la especialidad manualmente.';
                error.hidden = false;
                estado.textContent = '';
            } finally {
                clearTimeout(timeout);
                if (peticionOrientacion === controller) {
                    peticionOrientacion = null;
                    boton.disabled = false;
                    boton.querySelector('span').textContent = 'Consultar especialidad';
                }
            }
        }

        function aplicarRecomendacionIA() {
            if (!sugerenciaOrientacion) return;
            const select = document.getElementById('selectEspecialidad');
            const opcion = [...select.options].find(o => o.value === String(sugerenciaOrientacion.idEspecialidad));
            if (!opcion) { invalidarOrientacion(); return; }
            select.value = opcion.value;
            state.especialidadId = opcion.value;
            state.especialidadNombre = opcion.textContent;
            iniciarModoDirecto('IA');
        }

        // --- CONTROL DEL STEPPER Y PANTALLAS ---
        function mostrarPaso(num) {
            state.pasoActual = num;
            
            const titleRow = document.getElementById('pageTitleRow');
            if (titleRow) titleRow.style.display = 'flex';
            const stepperContainer = document.querySelector('.wizard-stepper-container');
            if (stepperContainer) stepperContainer.style.display = 'flex';
            const stepSuccess = document.getElementById('stepSuccessContent');
            if (stepSuccess) stepSuccess.style.display = 'none';

            document.getElementById('step1Content').style.display = num === 1 ? 'block' : 'none';
            document.getElementById('step2Content').style.display = num === 2 ? 'block' : 'none';
            document.getElementById('step3Content').style.display = num === 3 ? 'block' : 'none';

            const titleEl = document.getElementById('pageMainTitle');
            const subEl = document.getElementById('pageMainSubtitle');
            const btnVolver = document.getElementById('btnVolverModos');

            if (num === 1) {
                if (titleEl) titleEl.textContent = 'Buscar cita';
                if (subEl) subEl.textContent = 'Busca los horarios disponibles según tus preferencias';
                if (btnVolver) btnVolver.style.display = 'inline-flex';
                detenerCountdownReserva();
            } else if (num === 2) {
                if (titleEl) titleEl.textContent = 'Resultados de citas disponibles';
                if (subEl) subEl.textContent = 'Ordenado por horario más próximo';
                if (btnVolver) btnVolver.style.display = 'inline-flex';
                detenerCountdownReserva();

                // Actualizar píldoras de filtros activos
                const pillSede = document.getElementById('pillSede');
                if (pillSede) pillSede.textContent = state.sedeNombre || 'Sede San Borja';
                const pillEsp = document.getElementById('pillEspecialidad');
                if (pillEsp) pillEsp.textContent = state.especialidadNombre || 'Especialidad por confirmar';
                const pillTurno = document.getElementById('pillTurno');
                if (pillTurno) {
                    pillTurno.textContent = state.turno === 'MANANA' ? 'Turno mañana' : (state.turno === 'TARDE' ? 'Turno tarde' : 'Indiferente');
                }

                renderizarSemana();
                void cargarGrillaHorarios();
            } else if (num === 3) {
                if (titleEl) titleEl.textContent = 'Finalizar reserva';
                if (subEl) subEl.textContent = 'Revisa los datos y finaliza antes de que termine el tiempo';
                if (btnVolver) btnVolver.style.display = 'none';
                iniciarCountdownReserva();
            }

            for (let i = 1; i <= 3; i++) {
                const el = document.getElementById('wizStep' + i);
                if (!el) continue;
                const numEl = el.querySelector('.wiz-num');
                if (i < num) {
                    el.className = 'wiz-step completed';
                    if (numEl) numEl.innerHTML = '<i data-lucide="check" style="width: 14px; height: 14px;"></i>';
                } else if (i === num) {
                    el.className = 'wiz-step active';
                    if (numEl) numEl.textContent = i;
                } else {
                    el.className = 'wiz-step';
                    if (numEl) numEl.textContent = i;
                }
            }

            const line1 = document.getElementById('wizLine1');
            const line2 = document.getElementById('wizLine2');
            if (line1) line1.className = num >= 2 ? 'wiz-line active' : 'wiz-line';
            if (line2) line2.className = num >= 3 ? 'wiz-line active' : 'wiz-line';

            lucide.createIcons();
        }

        function volverAPaso1() {
            mostrarPaso(1);
        }

        function volverAPaso2() {
            mostrarPaso(2);
        }

        // --- ACCIONES DE FILTRO ---
        function seleccionarTurno(btn, turno) {
            document.querySelectorAll('.turno-pill').forEach(p => {
                p.classList.remove('selected');
                const ic = p.querySelector('svg, i');
                if (ic) ic.remove();
            });
            btn.classList.add('selected');
            const checkIcon = document.createElement('i');
            checkIcon.setAttribute('data-lucide', 'check');
            checkIcon.style.width = '14px';
            checkIcon.style.height = '14px';
            btn.insertBefore(checkIcon, btn.firstChild);
            lucide.createIcons();
            state.turno = turno;
        }

        function limpiarFiltros() {
            const selSede = document.getElementById('selectSede');
            if (selSede) selSede.selectedIndex = 0;
            const selEsp = document.getElementById('selectEspecialidad');
            if (selEsp) {
                selEsp.selectedIndex = 0;
                actualizarEspecialidadYMotivos();
            }
            const selMed = document.getElementById('selectMedico');
            if (selMed) selMed.innerHTML = '<option value="0">Cualquier médico</option>';
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('inputFecha').value = today;
            const firstPill = document.querySelector('.turno-pill');
            if (firstPill) seleccionarTurno(firstPill, 'MANANA');
        }

        function actualizarEspecialidadYMotivos() {
            const selEsp = document.getElementById('selectEspecialidad');
            const selMotivo = document.getElementById('selectMotivo');
            const selMed = document.getElementById('selectMedico');
            if (!selEsp || !selMotivo) return;

            selMotivo.innerHTML = '<option value="">Selecciona un motivo</option>';

            const espVal = selEsp.value;
            if (!espVal) {
                if (selMed) selMed.innerHTML = '<option value="0">Cualquier médico</option>';
                return;
            }

            const espNombre = selEsp.options[selEsp.selectedIndex] ? selEsp.options[selEsp.selectedIndex].text.toLowerCase() : '';

            let motivos = [];
            if (espNombre.includes('cardio')) {
                motivos = [
                    'Dolor o presión en el pecho',
                    'Control de hipertensión arterial',
                    'Palpitaciones o arritmias',
                    'Evaluación de riesgo quirúrgico',
                    'Chequeo preventivo cardiológico'
                ];
            } else if (espNombre.includes('neuro')) {
                motivos = [
                    'Dolores de cabeza o migraña recurrente',
                    'Episodios de mareo o vértigo',
                    'Trastornos del sueño o insomnio',
                    'Control de neuralgia o ciática'
                ];
            } else if (espNombre.includes('trauma')) {
                motivos = [
                    'Dolor lumbar o en la columna',
                    'Lesión en rodilla o meniscos',
                    'Dolor en hombro o articulaciones',
                    'Evaluación de fractura o esguince'
                ];
            } else if (espNombre.includes('derma')) {
                motivos = [
                    'Alergia, urticaria o picazón en la piel',
                    'Revisión de lunares o manchas',
                    'Acné o rosácea',
                    'Caída de cabello o alopecia'
                ];
            } else if (espNombre.includes('gastro')) {
                motivos = [
                    'Dolor abdominal o cólicos',
                    'Gastritis, acidez o reflujo',
                    'Digestión pesada o náuseas',
                    'Evaluación para endoscopía'
                ];
            } else {
                motivos = [
                    'Consulta médica general',
                    'Chequeo médico preventivo anual',
                    'Revisión de análisis de laboratorio',
                    'Evaluación de fiebre o malestar general'
                ];
            }

            motivos.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m;
                opt.textContent = m;
                selMotivo.appendChild(opt);
            });

            actualizarMedicosPorEspecialidad();
        }

        let medicosRequest = 0;

        async function actualizarMedicosPorEspecialidad(preIdMed) {
            const version = ++medicosRequest;
            const espId = document.getElementById('selectEspecialidad')?.value;
            const sedeId = document.getElementById('selectSede')?.value;
            const select = document.getElementById('selectMedico');
            if (!select) return;
            llenarOpcionesMedicos(select, [], null, 'Consultando médicos…');
            if (!espId || espId === '0') {
                llenarOpcionesMedicos(select, [], null, 'Selecciona una especialidad');
                return;
            }
            try {
                const medicos = await ClinicaCatalogos.medicos('${pageContext.request.contextPath}', espId, sedeId);
                if (version !== medicosRequest) return;
                llenarOpcionesMedicos(select, medicos, preIdMed);
            } catch (error) {
                if (version === medicosRequest) llenarOpcionesMedicos(select, [], null, error.message);
            }
        }

        function llenarOpcionesMedicos(select, medicos, preIdMed, message) {
            select.replaceChildren();
            const placeholder = document.createElement('option');
            placeholder.value = '0';
            placeholder.textContent = message || (medicos.length ? 'Cualquier médico' : 'Sin médicos para estos filtros');
            select.appendChild(placeholder);
            select.disabled = medicos.length === 0;
            medicos.forEach(medico => {
                const option = document.createElement('option');
                option.value = medico.idMedico;
                option.textContent = medico.nombreCompleto || 'Nombre no registrado';
                option.selected = String(medico.idMedico) === String(preIdMed);
                select.appendChild(option);
            });
        }

        // --- NAVEGACIÓN Y BÚSQUEDA DEL PASO 2 ---
        function buscarHorariosDisponibles() {
            const selSede = document.getElementById('selectSede');
            if (!selSede || !selSede.value) {
                alert('Por favor escoja una sede.');
                if (selSede) selSede.focus();
                return;
            }

            const selEsp = document.getElementById('selectEspecialidad');
            if (!selEsp || !selEsp.value) {
                alert('Por favor escoja una especialidad médica.');
                if (selEsp) selEsp.focus();
                return;
            }

            state.sedeId = selSede.value;
            state.sedeNombre = selSede.options[selSede.selectedIndex].text;

            state.especialidadId = selEsp.value;
            state.especialidadNombre = selEsp.options[selEsp.selectedIndex].text;
            
            const selMed = document.getElementById('selectMedico');
            state.medicoId = selMed ? selMed.value : '0';
            state.medicoNombre = selMed ? selMed.options[selMed.selectedIndex].text : 'Cualquier médico';

            const fechaInput = document.getElementById('inputFecha');
            state.fecha = (fechaInput && fechaInput.value) ? fechaInput.value : new Date().toISOString().split('T')[0];
            const selMotivo = document.getElementById('selectMotivo');
            state.motivo = selMotivo ? selMotivo.value : '';

            mostrarPaso(2);
        }

        // --- SELECTOR DE SEMANA Y DÍAS ---
        function parsearFechaLocal(str) {
            if (!str) return new Date();
            if (str instanceof Date) {
                return isNaN(str.getTime()) ? new Date() : str;
            }
            if (typeof str === 'string') {
                const s = str.trim();
                if (s.includes('-')) {
                    const partes = s.split('T')[0].split('-');
                    if (partes.length === 3) {
                        const y = parseInt(partes[0], 10);
                        const m = parseInt(partes[1], 10) - 1;
                        const d = parseInt(partes[2], 10);
                        if (!isNaN(y) && !isNaN(m) && !isNaN(d)) {
                            return new Date(y, m, d, 12, 0, 0);
                        }
                    }
                }
                if (s.includes('/')) {
                    const partes = s.split('/');
                    if (partes.length === 3) {
                        const d = parseInt(partes[0], 10);
                        const m = parseInt(partes[1], 10) - 1;
                        const y = parseInt(partes[2], 10);
                        if (!isNaN(y) && !isNaN(m) && !isNaN(d)) {
                            return new Date(y, m, d, 12, 0, 0);
                        }
                    }
                }
            }
            const dt = new Date(str);
            return isNaN(dt.getTime()) ? new Date() : dt;
        }

        function formatearIsoDate(date) {
            if (!date || isNaN(date.getTime())) date = new Date();
            const y = date.getFullYear();
            const m = String(date.getMonth() + 1).padStart(2, '0');
            const d = String(date.getDate()).padStart(2, '0');
            return y + '-' + m + '-' + d;
        }

        function renderizarSemana() {
            const grid = document.getElementById('weekDaysGrid');
            const label = document.getElementById('weekRangeLabel');
            if (!grid || !label) return;
            grid.replaceChildren();
            const date = parsearFechaLocal(state.fecha);
            state.fecha = formatearIsoDate(date);
            const offset = date.getDay() === 0 ? -6 : 1 - date.getDay();
            const monday = new Date(date.getFullYear(), date.getMonth(), date.getDate() + offset, 12);
            const sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6, 12);
            label.textContent = 'Semana del ' + monday.getDate() + ' al '
                + sunday.toLocaleDateString('es-PE', {day: 'numeric', month: 'long', year: 'numeric'});
            const labels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
            labels.forEach((name, index) => {
                const day = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + index, 12);
                const iso = formatearIsoDate(day);
                const card = document.createElement('div');
                card.className = 'day-card-item' + (iso === state.fecha ? ' active' : '');
                const title = document.createElement('div');
                title.className = 'day-name';
                title.textContent = name;
                const number = document.createElement('div');
                number.className = 'day-number';
                number.textContent = day.getDate();
                const count = document.createElement('div');
                count.className = 'day-cupos';
                count.textContent = 'Consultar';
                card.append(title, number, count);
                card.onclick = () => seleccionarDiaSemana(iso);
                grid.appendChild(card);
            });
        }

        function seleccionarDiaSemana(fechaIso) {
            state.fecha = fechaIso;
            state.hora = '';
            renderizarSemana();
            void cargarGrillaHorarios();
        }

        function navegarSemana(delta) {
            const day = parsearFechaLocal(state.fecha);
            day.setDate(day.getDate() + delta * 7);
            seleccionarDiaSemana(formatearIsoDate(day));
        }

        function formatearCabeceraTurno(fechaIso, turno) {
            const d = parsearFechaLocal(fechaIso);
            const diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
            const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];
            const diaNom = diasSemana[d.getDay()];
            const diaNum = d.getDate();
            const mesNom = meses[d.getMonth()];
            const turnoNom = turno === 'TARDE' ? 'Turno tarde' : (turno === 'MANANA' ? 'Turno mañana' : 'Turno disponible');
            return diaNom + ' ' + diaNum + ' ' + mesNom + ' · ' + turnoNom;
        }

        // --- CARGA Y RENDERIZADO DE MÉDICOS Y HORARIOS ---
        let grillaRequest = 0;

        async function cargarGrillaHorarios() {
            const container = document.getElementById('doctorResultsContainer');
            if (!container) return;
            const version = ++grillaRequest;
            const fecha = state.fecha;
            const turno = state.turno;
            const elegido = state.medicoId;
            state.hora = '';
            const currentBadge = document.querySelector('#weekDaysGrid .day-card-item.active .day-cupos');
            if (currentBadge) currentBadge.textContent = 'Consultando…';
            mostrarEstadoCatalogo(container, 'Consultando médicos y horarios publicados…');
            try {
                const medicos = await ClinicaCatalogos.medicos('${pageContext.request.contextPath}',
                    state.especialidadId, state.sedeId);
                const seleccionados = medicos.filter(medico => !elegido || elegido === '0' || String(medico.idMedico) === String(elegido));
                const agendas = await Promise.all(seleccionados.map(async medico => ({
                    medico, horas: await ClinicaCatalogos.horarios('${pageContext.request.contextPath}', medico.idMedico, fecha)
                })));
                if (version !== grillaRequest) return;
                container.replaceChildren();
                let total = 0;
                agendas.forEach(agenda => {
                    const horas = agenda.horas.filter(hora => coincideTurno(hora, turno));
                    if (horas.length) {
                        total += horas.length;
                        container.appendChild(tarjetaMedico(agenda.medico, horas, fecha, turno));
                    }
                });
                const badge = document.querySelector('#weekDaysGrid .day-card-item.active .day-cupos');
                if (badge) badge.textContent = total + (total === 1 ? ' cupo' : ' cupos');
                if (!total) mostrarEstadoCatalogo(container, 'No hay horarios publicados disponibles para estos filtros y fecha.', true);
                lucide.createIcons();
            } catch (error) {
                if (version === grillaRequest) {
                    if (currentBadge) currentBadge.textContent = 'Sin verificar';
                    mostrarEstadoCatalogo(container, error.message);
                }
            }
        }

        function coincideTurno(hora, turno) {
            const hour = Number(hora.split(':')[0]);
            if (turno === 'MANANA') return hour < 13;
            if (turno === 'TARDE') return hour >= 13;
            return true;
        }

        function mostrarEstadoCatalogo(container, message, empty = false) {
            container.replaceChildren();
            const text = document.createElement('p');
            text.className = 'catalogo-estado';
            text.setAttribute('role', 'alert');
            text.textContent = message;
            container.appendChild(text);
            if (empty) {
                const waiting = document.createElement('a');
                waiting.href = '${pageContext.request.contextPath}/paciente/lista-espera';
                waiting.className = 'btn-waitlist-action';
                waiting.textContent = 'Registrarme en lista de espera';
                container.appendChild(waiting);
            }
        }

        function catalogoElemento(tag, className, text) {
            const element = document.createElement(tag);
            element.className = className;
            if (text !== undefined) element.textContent = text;
            return element;
        }

        function tarjetaMedico(medico, horas, fecha, turno) {
            const id = medico.idMedico;
            const nombre = medico.nombreCompleto || 'Nombre no registrado';
            const cmp = medico.cmp || 'CMP no registrado';
            const card = catalogoElemento('div', 'doctor-schedule-card');
            card.id = 'docScheduleCard_' + id;
            card.setAttribute('data-cmp', cmp);
            card.setAttribute('data-consultorio', 'Consultorio por confirmar');
            const profile = catalogoElemento('div', 'doc-card-profile-col');
            const initials = nombre.split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase();
            profile.appendChild(catalogoElemento('div', 'doc-avatar-circle', initials));
            const info = catalogoElemento('div', 'doc-meta-info');
            info.append(catalogoElemento('h4', 'doc-name', nombre),
                catalogoElemento('div', 'doc-spec-cmp', cmp + ' · ' + state.especialidadNombre),
                catalogoElemento('div', 'doc-location', state.sedeNombre || 'Sede seleccionada'));
            profile.appendChild(info);
            const slots = catalogoElemento('div', 'doc-card-slots-col');
            slots.appendChild(catalogoElemento('div', 'doc-turn-kicker', formatearCabeceraTurno(fecha, turno)));
            const wrapper = catalogoElemento('div', 'doc-slots-wrap');
            wrapper.id = 'slotsWrap_' + id;
            horas.forEach(hora => {
                const button = catalogoElemento('button', 'slot-pill-btn', hora);
                button.type = 'button';
                button.setAttribute('data-med', id);
                button.setAttribute('data-hora', hora);
                button.onclick = () => seleccionarSlot(button, id, nombre, hora);
                wrapper.appendChild(button);
            });
            slots.appendChild(wrapper);
            const actions = catalogoElemento('div', 'doc-card-action-col');
            const reserve = catalogoElemento('button', 'btn-reservar-slot', 'Reservar cita');
            reserve.id = 'btnReservar_' + id;
            reserve.type = 'button';
            reserve.onclick = () => reservarConDoctor(id, nombre);
            actions.appendChild(reserve);
            card.append(profile, slots, actions);
            return card;
        }



        function seleccionarSlot(btn, idMed, nombreMed, hora) {
            // Deseleccionar todos los chips de todos los médicos
            document.querySelectorAll('.slot-pill-btn').forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');

            // Desactivar todos los botones de acción de reserva de todos los médicos
            document.querySelectorAll('.btn-reservar-slot').forEach(b => b.classList.remove('active-card'));
            
            // Activar únicamente el botón de este médico
            const btnCard = document.getElementById('btnReservar_' + idMed);
            if (btnCard) btnCard.classList.add('active-card');

            state.medicoId = idMed;
            state.medicoNombre = nombreMed;
            state.hora = hora;

            const card = document.getElementById('docScheduleCard_' + idMed);
            if (card) {
                state.medicoCmp = card.getAttribute('data-cmp') || '';
                state.consultorio = card.getAttribute('data-consultorio') || '';
            }
        }

        function reservarConDoctor(idMed, nombreMed) {
            const card = document.getElementById('docScheduleCard_' + idMed);
            let slotSeleccionado = card ? card.querySelector('.slot-pill-btn.selected') : null;

            if (!slotSeleccionado) {
                alert('Por favor selecciona primero un horario con ' + nombreMed + '.');
                if (card) {
                    card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                    card.style.transition = 'box-shadow 0.3s, border-color 0.3s';
                    card.style.borderColor = '#0284c7';
                    card.style.boxShadow = '0 0 0 4px rgba(2, 132, 199, 0.25)';
                    setTimeout(() => {
                        card.style.borderColor = '';
                        card.style.boxShadow = '';
                    }, 1400);
                }
                return;
            }

            state.medicoId = idMed;
            state.medicoNombre = nombreMed;
            state.hora = slotSeleccionado.getAttribute('data-hora') || slotSeleccionado.textContent.trim();
            if (card) {
                state.medicoCmp = card.getAttribute('data-cmp') || '';
                state.consultorio = card.getAttribute('data-consultorio') || '';
            }

            avanzarAPaso3();
        }

        function formatearFechaHoraResumen(fechaIso, hora) {
            const d = parsearFechaLocal(fechaIso);
            const diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
            const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];
            const diaNom = diasSemana[d.getDay()];
            const diaNum = d.getDate();
            const mesNom = meses[d.getMonth()];
            const anio = d.getFullYear();
            const horaTexto = hora ? (' · ' + hora) : '';
            return diaNom + ' ' + diaNum + ' ' + mesNom + ' ' + anio + horaTexto;
        }

        function poblarMotivosPaso3() {
            const selPaso3 = document.getElementById('selectMotivoPaso3');
            if (!selPaso3) return;

            selPaso3.innerHTML = '<option value="" disabled selected>Selecciona un motivo</option>';

            const selMotivoPaso1 = document.getElementById('selectMotivo');
            if (selMotivoPaso1 && selMotivoPaso1.options.length > 1) {
                for (let i = 1; i < selMotivoPaso1.options.length; i++) {
                    const opt = document.createElement('option');
                    opt.value = selMotivoPaso1.options[i].value;
                    opt.textContent = selMotivoPaso1.options[i].text;
                    selPaso3.appendChild(opt);
                }
            } else {
                const esp = (state.especialidadNombre || '').toLowerCase();
                let motivos = [];
                if (esp.includes('cardio')) {
                    motivos = [
                        'Dolor o presión en el pecho',
                        'Control de hipertensión arterial',
                        'Palpitaciones o arritmias',
                        'Evaluación de riesgo quirúrgico',
                        'Chequeo preventivo cardiológico'
                    ];
                } else if (esp.includes('neuro')) {
                    motivos = [
                        'Dolores de cabeza o migraña recurrente',
                        'Episodios de mareo o vértigo',
                        'Trastornos del sueño o insomnio',
                        'Control de neuralgia o ciática'
                    ];
                } else if (esp.includes('trauma')) {
                    motivos = [
                        'Dolor lumbar o en la columna',
                        'Lesión en rodilla o meniscos',
                        'Dolor en hombro o articulaciones',
                        'Evaluación de fractura o esguince'
                    ];
                } else if (esp.includes('derma')) {
                    motivos = [
                        'Alergia, urticaria o picazón en la piel',
                        'Revisión de lunares o manchas',
                        'Acné o rosácea',
                        'Caída de cabello o alopecia'
                    ];
                } else if (esp.includes('gastro')) {
                    motivos = [
                        'Dolor abdominal o cólicos',
                        'Gastritis, acidez o reflujo',
                        'Digestión pesada o náuseas',
                        'Evaluación para endoscopía'
                    ];
                } else {
                    motivos = [
                        'Consulta médica general',
                        'Chequeo médico preventivo anual',
                        'Revisión de análisis de laboratorio',
                        'Evaluación de fiebre o malestar general'
                    ];
                }
                motivos.forEach(m => {
                    const opt = document.createElement('option');
                    opt.value = m;
                    opt.textContent = m;
                    selPaso3.appendChild(opt);
                });
            }

            if (state.motivo) {
                selPaso3.value = state.motivo;
            }
        }

        function toggleNotifChannel(tipo) {
            const chk = document.getElementById(tipo === 'sms' ? 'checkNotifSms' : 'checkNotifEmail');
            const lbl = document.getElementById(tipo === 'sms' ? 'labelNotifSms' : 'labelNotifEmail');
            if (!chk || !lbl) return;
            if (chk.checked) {
                lbl.classList.add('active');
            } else {
                lbl.classList.remove('active');
            }
        }

        let countdownInterval = null;
        let countdownRemainingSeconds = 300;

        function iniciarCountdownReserva() {
            detenerCountdownReserva();
            countdownRemainingSeconds = 300;
            actualizarTextoCountdown();
            countdownInterval = setInterval(() => {
                countdownRemainingSeconds--;
                if (countdownRemainingSeconds <= 0) {
                    detenerCountdownReserva();
                    alert('El tiempo límite para completar la reserva (5 minutos) ha expirado. El cupo ha sido liberado.');
                    volverAPaso2();
                } else {
                    actualizarTextoCountdown();
                }
            }, 1000);
        }

        function detenerCountdownReserva() {
            if (countdownInterval) {
                clearInterval(countdownInterval);
                countdownInterval = null;
            }
        }

        function actualizarTextoCountdown() {
            const el = document.getElementById('reservaCountdown');
            if (!el) return;
            const mins = Math.floor(countdownRemainingSeconds / 60);
            const secs = countdownRemainingSeconds % 60;
            el.textContent = (mins < 10 ? '0' : '') + mins + ':' + (secs < 10 ? '0' : '') + secs;
        }

        function avanzarAPaso3() {
            if (!state.medicoId || !state.hora) {
                alert('Por favor selecciona un horario de atención disponible.');
                return;
            }

            const elEsp = document.getElementById('resumenEspecialidad');
            if (elEsp) elEsp.textContent = state.especialidadNombre || 'Especialidad por confirmar';

            const elMed = document.getElementById('resumenMedicoCMP');
            if (elMed) {
                const rawCmp = state.medicoCmp || '';
                const cmpNum = rawCmp.replace(/CMP[-:\s]*/i, '').trim();
                elMed.textContent = state.medicoNombre + (cmpNum ? ' · ' + cmpNum : '');
            }

            const elSede = document.getElementById('resumenSedeConsultorio');
            if (elSede) {
                const sedeNom = (state.sedeNombre && state.sedeNombre !== '0' && state.sedeNombre !== 'Todas las sedes') ? state.sedeNombre : 'Sede San Borja';
                const consNom = state.consultorio || 'Consultorio por confirmar';
                elSede.textContent = sedeNom + ' · ' + consNom;
            }

            const elFecha = document.getElementById('resumenFechaHoraFormateada');
            if (elFecha) {
                elFecha.textContent = formatearFechaHoraResumen(state.fecha, state.hora);
            }

            poblarMotivosPaso3();

            const fIdMed = document.getElementById('finalIdMedico');
            if (fIdMed) fIdMed.value = state.medicoId;
            const fFecha = document.getElementById('finalFecha');
            if (fFecha) fFecha.value = state.fecha;
            const fHora = document.getElementById('finalHora');
            if (fHora) fHora.value = state.hora;
            const fUrg = document.getElementById('finalUrgencia');
            if (fUrg) fUrg.value = state.urgencia || 'MEDIA';

            mostrarPaso(3);
        }

        function mostrarPantallaExito(codigoCita) {
            detenerCountdownReserva();

            // Ocultar cabeceras y stepper para dejar ÚNICAMENTE la tarjeta centrada
            const stepper = document.querySelector('.wizard-stepper-container');
            if (stepper) stepper.style.display = 'none';

            const titleRow = document.querySelector('.page-title-row-with-back');
            if (titleRow) titleRow.style.display = 'none';

            const pageTitleRowId = document.getElementById('pageTitleRow');
            if (pageTitleRowId) pageTitleRowId.style.display = 'none';

            const viewMode = document.getElementById('viewModeSelector');
            if (viewMode) viewMode.style.display = 'none';

            // Ocultar pasos anteriores y mostrar éxito
            const p1 = document.getElementById('step1Content');
            if (p1) p1.style.display = 'none';
            const p2 = document.getElementById('step2Content');
            if (p2) p2.style.display = 'none';
            const p3 = document.getElementById('step3Content');
            if (p3) p3.style.display = 'none';

            const stepSuccess = document.getElementById('stepSuccessContent');
            if (stepSuccess) stepSuccess.style.display = 'block';

            // Llenar datos en el comprobante
            const elCod = document.getElementById('successCodigoCita');
            if (elCod) elCod.textContent = codigoCita;

            const elEsp = document.getElementById('successEspecialidad');
            if (elEsp) elEsp.textContent = state.especialidadNombre || 'Especialidad por confirmar';

            const elMed = document.getElementById('successMedicoCMP');
            const resMed = document.getElementById('resumenMedicoCMP');
            if (elMed) elMed.textContent = resMed ? resMed.textContent : state.medicoNombre;

            const elSede = document.getElementById('successSedeConsultorio');
            const resSede = document.getElementById('resumenSedeConsultorio');
            if (elSede) elSede.textContent = resSede ? resSede.textContent : 'Sede San Borja · Consultorio 304';

            const elFecha = document.getElementById('successFechaHora');
            const resFecha = document.getElementById('resumenFechaHoraFormateada');
            if (elFecha) elFecha.textContent = resFecha ? resFecha.textContent : formatearFechaHoraResumen(state.fecha, state.hora);

            const elMot = document.getElementById('successMotivo');
            const selMotP3 = document.getElementById('selectMotivoPaso3');
            if (elMot) {
                const motVal = (selMotP3 && selMotP3.value) ? selMotP3.options[selMotP3.selectedIndex]?.text : state.motivo;
                elMot.textContent = motVal || 'Control de presión arterial';
            }

            lucide.createIcons();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }
    </script>
</body>
</html>
