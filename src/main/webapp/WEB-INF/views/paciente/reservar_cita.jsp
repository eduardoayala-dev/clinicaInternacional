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

    int numNotifSinLeer = request.getAttribute("numNotifSinLeer") != null ? (Integer) request.getAttribute("numNotifSinLeer") : 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Programar Cita - Portal del Paciente | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/buscar-cita.css">
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
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item active">
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
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout" title="Cerrar sesión">
                <i data-lucide="log-out" style="width: 18px; height: 18px;"></i>
            </a>
        </div>
    </aside>

    <!-- CONTENIDO PRINCIPAL -->
    <main class="main-content">

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
                <div class="page-title-row-with-back">
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
                        
                        <!-- TARJETA 1: ORIENTACIÓN MÉDICA POR IA -->
                        <div class="mode-card" onclick="iniciarModoIA()">
                            <div>
                                <div class="mode-card-top-row">
                                    <div class="mode-card-icon">
                                        <i data-lucide="bot" style="width: 26px; height: 26px;"></i>
                                    </div>
                                    <span class="mode-badge-pill badge-pill-mint">
                                        Nuevo · Asistido por IA
                                    </span>
                                </div>

                                <div class="mode-kicker">ORIENTACIÓN MÉDICA POR IA</div>
                                <h2 class="mode-card-title">No sé qué especialidad necesito</h2>
                                <p class="mode-card-desc">
                                    Describe lo que sientes. Nuestra IA analizará tus síntomas para sugerirte el especialista adecuado según disponibilidad, con validación de nuestro equipo médico si es necesario.
                                </p>
                            </div>

                            <button type="button" class="btn-card-action">
                                <span>Describir síntomas</span>
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
                                <h3 style="font-size: 17px; font-weight: 700; color: #1e3a8a;">Asistente de Orientación Clínica por IA</h3>
                                <p style="font-size: 13px; color: #64748b;">Describe tus síntomas para orientarte con la especialidad médica más adecuada.</p>
                            </div>
                        </div>
                        <button type="button" onclick="cerrarPanelIA()" style="background: none; border: none; cursor: pointer; color: #64748b;" title="Cerrar">
                            <i data-lucide="x" style="width: 20px; height: 20px;"></i>
                        </button>
                    </div>

                    <div style="margin-bottom: 12px;">
                        <label for="aiSintomasInput" style="font-size: 13px; font-weight: 600; color: var(--text-dark); display: block; margin-bottom: 6px;">
                            ¿Qué síntomas o molestias presentas?
                        </label>
                        <textarea id="aiSintomasInput" class="ai-symptom-textarea" placeholder="Ej: Tengo dolor de cabeza punzante desde hace 2 días, mareos ligeros al levantarme y molestia con la luz..."></textarea>
                    </div>

                    <div class="quick-symptoms-label">O selecciona síntomas frecuentes:</div>
                    <div class="quick-chips-wrapper">
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Dolor de cabeza y mareos')">🤕 Dolor de cabeza y mareos</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Opresión en el pecho y palpitaciones')">❤️ Molestia en el pecho</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Dolor articular o lesión en rodilla/espalda')">🦴 Articulaciones o columna</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Erupción o manchas en la piel')">🧴 Alergia o piel</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Dolor estomacal y acidez')">🍏 Estómago o digestivo</button>
                        <button type="button" class="symptom-chip" onclick="agregarSintoma(this, 'Fiebre persistente y malestar general')">🌡️ Fiebre y malestar</button>
                    </div>

                    <div style="display: flex; gap: 12px; align-items: center;">
                        <button type="button" id="btnAnalizarIA" class="btn-card-action btn-card-royal" onclick="ejecutarAnalisisIA()">
                            <i data-lucide="sparkles" style="width: 16px; height: 16px;"></i>
                            <span>Analizar síntomas con IA</span>
                        </button>
                        <button type="button" class="btn-secondary-flat" onclick="cerrarPanelIA()">Volver</button>
                    </div>

                    <!-- RESULTADO DEL ANÁLISIS DE IA -->
                    <div id="aiResultadoBox" class="ai-response-result">
                        <div style="display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 12px;">
                            <div>
                                <div style="display: flex; gap: 8px; align-items: center; margin-bottom: 6px;">
                                    <span class="mode-badge-pill badge-pill-mint" style="margin-bottom: 0;">
                                        <i data-lucide="sparkles" style="width: 12px; height: 12px;"></i>
                                        Especialidad sugerida: <strong id="aiEspecialidadNombre" style="margin-left: 4px;">Neurología</strong>
                                    </span>
                                    <span id="aiPrioridadBadge" class="mode-badge-pill" style="background: #fef3c7; color: #b45309; border: 1px solid #fde68a;">
                                        Prioridad: Moderada
                                    </span>
                                </div>
                                <h4 style="font-size: 15.5px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px;">Orientación preliminar</h4>
                                <p id="aiExplicacionTexto" style="font-size: 13.5px; color: #334155; line-height: 1.5;">
                                    Los síntomas ingresados coinciden con el área de Neurología para evaluar posibles cuadros de cefalea o migraña.
                                </p>
                            </div>
                        </div>

                        <div style="padding-top: 14px; border-top: 1px solid #dcfce7; display: flex; gap: 12px; align-items: center;">
                            <button type="button" class="btn-primary-search" onclick="aplicarRecomendacionIA()">
                                <span>Continuar con esta especialidad</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
                            <button type="button" class="btn-secondary-flat" onclick="iniciarModoDirecto()">Elegir otra manualmente</button>
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
                        <div class="wiz-line"></div>
                        <div class="wiz-step" id="wizStep2">
                            <div class="wiz-num">2</div>
                            <span>Elegir horario</span>
                        </div>
                        <div class="wiz-line"></div>
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

                    <!-- PASO 3: CONFIRMACIÓN Y RESERVA -->
                    <div id="step3Content" style="display: none;">
                        <div class="confirmation-card">
                            <div style="margin-bottom: 22px; padding-bottom: 16px; border-bottom: 1px solid #f1f5f9;">
                                <h3 style="font-size: 20px; font-weight: 700; color: #1e3a8a; margin-bottom: 4px;">Confirmar Reserva de Cita</h3>
                                <p style="font-size: 13.5px; color: #64748b;">Por favor, revisa todos los datos antes de agendar tu cita médica en la Clínica Internacional.</p>
                            </div>

                            <div class="confirm-details-grid">
                                
                                <div class="confirm-item-box">
                                    <div class="confirm-item-icon">
                                        <i data-lucide="stethoscope" style="width: 22px; height: 22px;"></i>
                                    </div>
                                    <div>
                                        <div class="confirm-item-label">Especialidad</div>
                                        <div class="confirm-item-val" id="resumenEspecialidad">Cardiología</div>
                                    </div>
                                </div>

                                <div class="confirm-item-box">
                                    <div class="confirm-item-icon">
                                        <i data-lucide="user-check" style="width: 22px; height: 22px;"></i>
                                    </div>
                                    <div>
                                        <div class="confirm-item-label">Médico Tratante</div>
                                        <div class="confirm-item-val" id="resumenMedico">Dr. Carlos Ramos</div>
                                    </div>
                                </div>

                                <div class="confirm-item-box">
                                    <div class="confirm-item-icon">
                                        <i data-lucide="building" style="width: 22px; height: 22px;"></i>
                                    </div>
                                    <div>
                                        <div class="confirm-item-label">Sede y Consultorio</div>
                                        <div class="confirm-item-val" id="resumenSede">Sede Lima - Consultorio 302</div>
                                    </div>
                                </div>

                                <div class="confirm-item-box">
                                    <div class="confirm-item-icon">
                                        <i data-lucide="calendar" style="width: 22px; height: 22px;"></i>
                                    </div>
                                    <div>
                                        <div class="confirm-item-label">Fecha y Hora</div>
                                        <div class="confirm-item-val" id="resumenFechaHora">15/10/2026 - 10:30 AM</div>
                                    </div>
                                </div>

                            </div>

                            <div class="clinical-notice-box">
                                <strong>Recomendación importante:</strong> Preséntate con 15 minutos de anticipación en el módulo de recepción con tu documento de identidad (DNI o Carné de Extranjería). En caso de no poder asistir, recuerda cancelar con anticipación desde tu panel de citas.
                            </div>

                            <!-- FORMULARIO DE ENVÍO POST -->
                            <form id="formReservaFinal" action="${pageContext.request.contextPath}/paciente/reservar" method="POST">
                                <input type="hidden" name="idMedico" id="finalIdMedico" value="">
                                <input type="hidden" name="fecha" id="finalFecha" value="">
                                <input type="hidden" name="hora" id="finalHora" value="">
                                <input type="hidden" name="urgencia" id="finalUrgencia" value="MEDIA">

                                <div style="display: flex; justify-content: space-between; align-items: center; padding-top: 18px; border-top: 1px solid #f1f5f9;">
                                    <button type="button" class="btn-secondary-flat" onclick="volverAPaso2()">
                                        <i data-lucide="arrow-left" style="width: 15px; height: 15px;"></i>
                                        <span>Cambiar horario</span>
                                    </button>

                                    <button type="submit" class="btn-primary-search" id="btnConfirmarReserva">
                                        <i data-lucide="check-circle" style="width: 16px; height: 16px;"></i>
                                        <span>Agendar y Confirmar Cita</span>
                                    </button>
                                </div>
                            </form>

                        </div>
                    </div>

                </div>

            </div>
        </div>

    </main>

    <!-- SCRIPTS Y LÓGICA DE CONTROL -->
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
            document.getElementById('pageMainTitle').textContent = 'Orientación médica por IA';
            document.getElementById('pageMainSubtitle').textContent = 'Describe tus síntomas para orientarte con la especialidad médica más adecuada.';
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
            document.getElementById('aiTriagePanel').style.display = 'none';
            volverASeleccionModos();
        }

        function iniciarModoDirecto() {
            state.modo = 'DIRECTO';
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
            if (state.pasoActual === 2) {
                volverAPaso1();
                return;
            }
            if (state.pasoActual === 3) {
                volverAPaso2();
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

        // --- ASISTENTE IA DE SÍNTOMAS ---
        function agregarSintoma(btn, texto) {
            const textarea = document.getElementById('aiSintomasInput');
            if (textarea.value.trim() === '') {
                textarea.value = texto;
            } else if (!textarea.value.includes(texto)) {
                textarea.value += ', ' + texto;
            }
            btn.classList.add('selected');
        }

        function ejecutarAnalisisIA() {
            const texto = document.getElementById('aiSintomasInput').value.trim().toLowerCase();
            if (!texto) {
                alert('Por favor escribe tus síntomas o selecciona alguno de los sugeridos.');
                document.getElementById('aiSintomasInput').focus();
                return;
            }

            const btn = document.getElementById('btnAnalizarIA');
            btn.innerHTML = '<i data-lucide="loader" style="width: 16px; height: 16px; animation: spin 1s linear infinite;"></i> <span>Analizando con IA...</span>';
            btn.disabled = true;

            setTimeout(() => {
                let recomendacion = clasificarSintomasIA(texto);

                document.getElementById('aiEspecialidadNombre').textContent = recomendacion.especialidad;
                document.getElementById('aiExplicacionTexto').textContent = recomendacion.explicacion;
                
                const badgePrioridad = document.getElementById('aiPrioridadBadge');
                badgePrioridad.textContent = 'Prioridad: ' + recomendacion.prioridad;
                if (recomendacion.prioridad === 'Alta') {
                    badgePrioridad.style.background = '#fee2e2';
                    badgePrioridad.style.color = '#b91c1c';
                    badgePrioridad.style.borderColor = '#fca5a5';
                } else if (recomendacion.prioridad === 'Media' || recomendacion.prioridad === 'Moderada') {
                    badgePrioridad.style.background = '#fef3c7';
                    badgePrioridad.style.color = '#b45309';
                    badgePrioridad.style.borderColor = '#fde68a';
                } else {
                    badgePrioridad.style.background = '#dcfce7';
                    badgePrioridad.style.color = '#15803d';
                    badgePrioridad.style.borderColor = '#86efac';
                }

                state.especialidadNombre = recomendacion.especialidad;
                state.especialidadId = recomendacion.idEsp;
                state.urgencia = recomendacion.prioridad.toUpperCase();

                document.getElementById('aiResultadoBox').style.display = 'block';
                btn.innerHTML = '<i data-lucide="sparkles" style="width: 16px; height: 16px;"></i> <span>Analizar síntomas con IA</span>';
                btn.disabled = false;
                lucide.createIcons();
            }, 550);
        }

        function clasificarSintomasIA(texto) {
            if (texto.includes('pecho') || texto.includes('cardio') || texto.includes('presion') || texto.includes('palpitacion') || texto.includes('corazon')) {
                return {
                    especialidad: 'Cardiología',
                    idEsp: '1',
                    prioridad: 'Alta',
                    explicacion: 'Se reportan síntomas de molestia torácica o palpitaciones. Recomendamos evaluación especializada con Cardiología.'
                };
            } else if (texto.includes('cabeza') || texto.includes('mareo') || texto.includes('migra') || texto.includes('vertigo') || texto.includes('neurol')) {
                return {
                    especialidad: 'Neurología',
                    idEsp: '2',
                    prioridad: 'Media',
                    explicacion: 'Los episodios de cefalea y mareos corresponden al área de Neurología para un diagnóstico certero.'
                };
            } else if (texto.includes('rodilla') || texto.includes('hueso') || texto.includes('fractura') || texto.includes('espalda') || texto.includes('lumbar') || texto.includes('articular')) {
                return {
                    especialidad: 'Traumatología',
                    idEsp: '4',
                    prioridad: 'Media',
                    explicacion: 'Las molestias osteoarticulares deben ser valoradas por Traumatología para determinar tratamiento o exámenes auxiliares.'
                };
            } else if (texto.includes('piel') || texto.includes('roncha') || texto.includes('mancha') || texto.includes('picaz') || texto.includes('acne')) {
                return {
                    especialidad: 'Dermatología',
                    idEsp: '5',
                    prioridad: 'Baja',
                    explicacion: 'Las manifestaciones cutáneas sugieren consulta dermatológica para indicación de terapia tópica.'
                };
            } else if (texto.includes('estomago') || texto.includes('gast') || texto.includes('ardor') || texto.includes('reflujo') || texto.includes('vomito')) {
                return {
                    especialidad: 'Gastroenterología',
                    idEsp: '6',
                    prioridad: 'Media',
                    explicacion: 'Los síntomas gástricos corresponden a Gastroenterología para evaluar reflujo o mucosa estomacal.'
                };
            } else {
                return {
                    especialidad: 'Medicina General',
                    idEsp: '3',
                    prioridad: 'Media',
                    explicacion: 'Para evaluación médica inicial y descarte, Medicina General brindará un diagnóstico y plan de atención.'
                };
            }
        }

        function aplicarRecomendacionIA() {
            document.getElementById('aiTriagePanel').style.display = 'none';
            document.getElementById('wizardFlowSection').style.display = 'block';
            document.getElementById('pageMainTitle').textContent = 'Buscar cita';
            document.getElementById('pageMainSubtitle').textContent = 'Busca los horarios disponibles según tus preferencias';
            const btnVolver = document.getElementById('btnVolverModos');
            if (btnVolver) btnVolver.style.display = 'inline-flex';
            
            const selectEsp = document.getElementById('selectEspecialidad');
            if (selectEsp) {
                let found = false;
                for (let i = 0; i < selectEsp.options.length; i++) {
                    const opt = selectEsp.options[i];
                    if (opt.text.toLowerCase().includes(state.especialidadNombre.toLowerCase())) {
                        selectEsp.selectedIndex = i;
                        state.especialidadId = opt.value;
                        found = true;
                        break;
                    }
                }
                if (!found && selectEsp.options.length > 1) {
                    selectEsp.selectedIndex = 1;
                    state.especialidadId = selectEsp.value;
                }
            }

            actualizarMedicosPorEspecialidad();
            mostrarPaso(1);
            lucide.createIcons();
        }

        // --- CONTROL DEL STEPPER Y PANTALLAS ---
        function mostrarPaso(num) {
            state.pasoActual = num;
            
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
            } else if (num === 2) {
                if (titleEl) titleEl.textContent = 'Resultados de citas disponibles';
                if (subEl) subEl.textContent = 'Ordenado por horario más próximo';
                if (btnVolver) btnVolver.style.display = 'inline-flex';

                // Actualizar píldoras de filtros activos
                const pillSede = document.getElementById('pillSede');
                if (pillSede) pillSede.textContent = state.sedeNombre || 'Sede San Borja';
                const pillEsp = document.getElementById('pillEspecialidad');
                if (pillEsp) pillEsp.textContent = state.especialidadNombre || 'Cardiología';
                const pillTurno = document.getElementById('pillTurno');
                if (pillTurno) {
                    pillTurno.textContent = state.turno === 'MANANA' ? 'Turno mañana' : (state.turno === 'TARDE' ? 'Turno tarde' : 'Indiferente');
                }

                const cupos = renderizarSemana();
                if (cupos === 0) {
                    renderizarSinCupos();
                } else {
                    cargarGrillaHorarios();
                }
            } else if (num === 3) {
                if (titleEl) titleEl.textContent = 'Confirmar cita';
                if (subEl) subEl.textContent = 'Revisa los detalles y confirma tu atención médica';
                if (btnVolver) btnVolver.style.display = 'inline-flex';
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
            cachedMedicos = null;
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

        function normalizarNombreMedico(rawNombre) {
            if (!rawNombre) return 'Dr. Médico Especialista';
            let n = rawNombre.trim();
            if (n.startsWith('Dr.') || n.startsWith('Dra.')) return n;
            const nLower = n.toLowerCase();
            const esDra = nLower.startsWith('elena') || nLower.startsWith('andrea') || 
                          nLower.startsWith('mariana') || nLower.startsWith('marina') || 
                          nLower.startsWith('maria') || nLower.startsWith('valeria') || 
                          nLower.startsWith('claudia') || nLower.startsWith('rosa') || 
                          nLower.startsWith('patricia') || nLower.startsWith('beatriz') ||
                          nLower.startsWith('sofia') || nLower.startsWith('lucia') ||
                          nLower.startsWith('carmen');
            return (esDra ? 'Dra. ' : 'Dr. ') + n;
        }

        function actualizarMedicosPorEspecialidad(preIdMed) {
            const selEsp = document.getElementById('selectEspecialidad');
            const espId = selEsp ? selEsp.value : '';
            const selSede = document.getElementById('selectSede') ? document.getElementById('selectSede').value : '';
            const selMed = document.getElementById('selectMedico');

            if (!espId) {
                if (selMed) selMed.innerHTML = '<option value="0">Cualquier médico</option>';
                return;
            }

            let url = '${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + espId;
            if (selSede && selSede !== '0') {
                url += '&idSede=' + selSede;
            }

            fetch(url + '&_t=' + Date.now())
                .then(res => res.json())
                .then(data => {
                    if (selMed) {
                        if (!data || data.length === 0) {
                            fetch('${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + espId + '&_t=' + Date.now())
                                .then(r => r.json())
                                .then(fallbackData => {
                                    llenarOpcionesMedicos(selMed, fallbackData, preIdMed);
                                })
                                .catch(() => {
                                    llenarOpcionesMedicos(selMed, [], preIdMed);
                                });
                        } else {
                            llenarOpcionesMedicos(selMed, data, preIdMed);
                        }
                    }
                })
                .catch(err => {
                    console.log('Carga de médicos local', err);
                    if (selMed) {
                        llenarOpcionesMedicos(selMed, [], preIdMed);
                    }
                });
        }

        function llenarOpcionesMedicos(selMed, data, preIdMed) {
            selMed.innerHTML = '<option value="0">Cualquier médico</option>';
            if (!data || data.length === 0) {
                data = [
                    { idMedico: 1, nombreCompleto: "Dr. Fernando Solís" },
                    { idMedico: 2, nombreCompleto: "Dra. Andrea Morales" },
                    { idMedico: 3, nombreCompleto: "Dr. Carlos Mendoza Valdivia" }
                ];
            }
            data.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.idMedico || m.id;
                const nombre = m.nombreCompleto || m.nombres || '';
                opt.textContent = normalizarNombreMedico(nombre);
                if (preIdMed && String(opt.value) === String(preIdMed)) {
                    opt.selected = true;
                }
                selMed.appendChild(opt);
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

            cachedMedicos = null;
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
            if (!grid || !label) return 0;

            grid.innerHTML = '';

            let baseDate = parsearFechaLocal(state.fecha);
            // Sincronizar state.fecha al formato canónico ISO
            state.fecha = formatearIsoDate(baseDate);

            let dayOfWeek = baseDate.getDay();
            let diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
            
            let monday = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate() + diffToMonday, 12, 0, 0);
            let sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6, 12, 0, 0);

            const meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
            const mesNombre = meses[sunday.getMonth()];
            label.textContent = 'Semana del ' + monday.getDate() + ' al ' + sunday.getDate() + ' de ' + mesNombre + ' de ' + sunday.getFullYear();

            const nombresDias = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
            const cuposSimulados = [4, 0, 3, 6, 1, 2, 0];

            let cuposSeleccionados = 0;

            for (let i = 0; i < 7; i++) {
                let d = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + i, 12, 0, 0);
                const isoDate = formatearIsoDate(d);

                const esActivo = isoDate === state.fecha;
                const esDomingo = i === 6;
                const cupos = esDomingo ? 0 : cuposSimulados[i];

                if (esActivo) {
                    cuposSeleccionados = cupos;
                }

                let cuposTexto = cupos + ' cupos';
                let cuposClass = 'day-cupos';
                if (esDomingo) {
                    cuposTexto = 'No atención';
                    cuposClass = 'day-cupos none';
                } else if (cupos === 0) {
                    cuposTexto = 'Sin cupos';
                    cuposClass = 'day-cupos none';
                } else if (cupos === 1) {
                    cuposTexto = '1 cupo';
                }

                const card = document.createElement('div');
                card.className = 'day-card-item' + (esActivo ? ' active' : '') + (esDomingo ? ' disabled' : '');
                card.innerHTML = 
                    '<div class="day-name">' + nombresDias[i] + '</div>' +
                    '<div class="day-number">' + d.getDate() + '</div>' +
                    '<div class="' + cuposClass + '">' + cuposTexto + '</div>';

                if (!esDomingo) {
                    card.onclick = function() {
                        seleccionarDiaSemana(isoDate, cupos);
                    };
                }

                grid.appendChild(card);
            }

            return cuposSeleccionados;
        }

        function seleccionarDiaSemana(fechaIso, cupos) {
            state.fecha = fechaIso;
            state.hora = '';
            state.medicoId = '0';
            renderizarSemana();
            if (cupos === 0) {
                renderizarSinCupos();
            } else {
                cargarGrillaHorarios();
            }
        }

        function navegarSemana(delta) {
            let cur = parsearFechaLocal(state.fecha);
            cur.setDate(cur.getDate() + (delta * 7));
            state.fecha = formatearIsoDate(cur);
            state.hora = '';
            state.medicoId = '0';
            const cupos = renderizarSemana();
            if (cupos === 0) {
                renderizarSinCupos();
            } else {
                cargarGrillaHorarios();
            }
        }

        function renderizarSinCupos() {
            const container = document.getElementById('doctorResultsContainer');
            if (!container) return;

            const headerTurnoTexto = formatearCabeceraTurno(state.fecha, state.turno);
            state.hora = '';
            state.medicoId = '0';

            container.innerHTML = 
                '<div style="background: #ffffff; border: 1.5px dashed #cbd5e1; border-radius: 16px; padding: 48px 24px; text-align: center; margin-bottom: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);">' +
                    '<div style="width: 58px; height: 58px; border-radius: 50%; background: #fef2f2; color: #ef4444; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 16px;">' +
                        '<i data-lucide="calendar-x" style="width: 30px; height: 30px;"></i>' +
                    '</div>' +
                    '<h3 style="font-size: 18px; font-weight: 700; color: #0f172a; margin-bottom: 8px;">No hay citas disponibles para este día</h3>' +
                    '<p style="font-size: 14px; color: #64748b; max-width: 520px; margin: 0 auto 20px auto; line-height: 1.5;">' +
                        'Para el <strong>' + headerTurnoTexto + '</strong> no quedan cupos de atención médica libres. Por favor selecciona otro día con cupos en el calendario semanal o únete a la lista de espera para avisarte si se libera una cita.' +
                    '</p>' +
                    '<a href="${pageContext.request.contextPath}/paciente/lista-espera" class="btn-waitlist-action" style="display: inline-flex; justify-content: center; margin: 0 auto;">' +
                        '<i data-lucide="clock" style="width: 16px; height: 16px; margin-right: 8px;"></i>' +
                        'Registrarme en lista de espera' +
                    '</a>' +
                '</div>';
            
            lucide.createIcons();
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
        let cachedMedicos = null;

        function generarHorariosDoctor(idMed, fechaIso, turno) {
            if (turno === 'TARDE') {
                return (idMed % 2 === 0) 
                    ? ["14:00", "14:40", "15:20", "16:00", "16:40"]
                    : ["14:20", "15:00", "15:40", "16:20", "17:00"];
            } else if (turno === 'MANANA') {
                return (idMed % 2 === 0)
                    ? ["08:00", "08:40", "09:20", "10:00", "10:40"]
                    : ["08:30", "09:00", "09:40", "10:20", "11:00"];
            } else {
                // Indiferente
                return (idMed % 2 === 0)
                    ? ["08:00", "08:40", "09:20", "14:00", "14:40", "15:20"]
                    : ["08:30", "09:10", "10:00", "14:20", "15:00", "16:00"];
            }
        }

        function actualizarHorariosInPlace(medicos) {
            const headerTurnoTexto = formatearCabeceraTurno(state.fecha, state.turno);
            state.hora = '';
            state.medicoId = '0';

            medicos.forEach(med => {
                const idMed = med.idMedico || med.id;
                const card = document.getElementById('docScheduleCard_' + idMed);
                if (!card) return;

                // Actualizar texto del turno sin saltos de altura ni recarga del contenedor
                const kicker = card.querySelector('.doc-turn-kicker');
                if (kicker) kicker.textContent = headerTurnoTexto;

                // Desactivar botón de reserva
                const btnRes = document.getElementById('btnReservar_' + idMed);
                if (btnRes) btnRes.classList.remove('active-card');

                // Actualizar chips de horarios directamente sin parpadeos
                const wrap = document.getElementById('slotsWrap_' + idMed);
                if (wrap) {
                    const rawName = med.nombreCompleto || med.nombres || '';
                    const nombreMed = normalizarNombreMedico(rawName);
                    const horas = generarHorariosDoctor(idMed, state.fecha, state.turno);

                    wrap.innerHTML = '';
                    horas.forEach(h => {
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'slot-pill-btn';
                        btn.textContent = h;
                        btn.setAttribute('data-med', idMed);
                        btn.setAttribute('data-hora', h);
                        btn.onclick = function() {
                            seleccionarSlot(btn, idMed, nombreMed, h);
                        };
                        wrap.appendChild(btn);
                    });
                }
            });
        }

        function cargarGrillaHorarios() {
            const container = document.getElementById('doctorResultsContainer');
            if (!container) return;

            // Si ya existen tarjetas en el DOM y tenemos la lista de médicos en caché, actualizar in-place
            const existingCards = container.querySelectorAll('.doctor-schedule-card');
            if (existingCards.length > 0 && cachedMedicos && cachedMedicos.length > 0) {
                actualizarHorariosInPlace(cachedMedicos);
                return;
            }

            // Si ya tenemos los médicos en memoria (ej. al volver de un día 'Sin cupos')
            if (cachedMedicos && cachedMedicos.length > 0) {
                renderizarMedicosConHorarios(cachedMedicos);
                return;
            }

            // Carga inicial para la especialidad / sede
            container.innerHTML = '<p style="text-align: center; color: var(--text-gray); padding: 32px;">Consultando disponibilidad de médicos y turnos...</p>';

            let url = '${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + state.especialidadId;
            if (state.sedeId && state.sedeId !== '0') {
                url += '&idSede=' + state.sedeId;
            }

            fetch(url + '&_t=' + Date.now())
                .then(res => res.json())
                .then(medicos => {
                    if (!medicos || medicos.length === 0) {
                        fetch('${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + state.especialidadId + '&_t=' + Date.now())
                            .then(r => r.json())
                            .then(fallbackMedicos => {
                                cachedMedicos = (fallbackMedicos && fallbackMedicos.length > 0) ? fallbackMedicos : [
                                    { idMedico: 1, nombreCompleto: "Dr. Fernando Solís", cmp: "CMP 045218", consultorio: "Consultorio 304" },
                                    { idMedico: 2, nombreCompleto: "Dra. Andrea Morales", cmp: "CMP 051367", consultorio: "Consultorio 306" }
                                ];
                                renderizarMedicosConHorarios(cachedMedicos);
                            })
                            .catch(() => {
                                cachedMedicos = [
                                    { idMedico: 1, nombreCompleto: "Dr. Fernando Solís", cmp: "CMP 045218", consultorio: "Consultorio 304" },
                                    { idMedico: 2, nombreCompleto: "Dra. Andrea Morales", cmp: "CMP 051367", consultorio: "Consultorio 306" }
                                ];
                                renderizarMedicosConHorarios(cachedMedicos);
                            });
                    } else {
                        cachedMedicos = medicos;
                        renderizarMedicosConHorarios(cachedMedicos);
                    }
                })
                .catch(() => {
                    cachedMedicos = [
                        { idMedico: 1, nombreCompleto: "Dr. Fernando Solís", cmp: "CMP 045218", consultorio: "Consultorio 304" },
                        { idMedico: 2, nombreCompleto: "Dra. Andrea Morales", cmp: "CMP 051367", consultorio: "Consultorio 306" }
                    ];
                    renderizarMedicosConHorarios(cachedMedicos);
                });
        }

        function renderizarMedicosConHorarios(medicos) {
            const container = document.getElementById('doctorResultsContainer');
            container.innerHTML = '';

            // Limpiar selección previa
            state.hora = '';

            if (!medicos || medicos.length === 0) {
                medicos = [
                    { idMedico: 1, nombreCompleto: "Dr. Fernando Solís", cmp: "CMP 045218", consultorio: "Consultorio 304" },
                    { idMedico: 2, nombreCompleto: "Dra. Andrea Morales", cmp: "CMP 051367", consultorio: "Consultorio 306" }
                ];
            }

            if (state.medicoId && state.medicoId !== '0') {
                const filtrados = medicos.filter(m => String(m.idMedico || m.id) === String(state.medicoId));
                if (filtrados.length > 0) medicos = filtrados;
            }

            const headerTurnoTexto = formatearCabeceraTurno(state.fecha, state.turno);

            medicos.forEach((med, idx) => {
                const idMed = med.idMedico || med.id || (idx + 1);
                const rawName = med.nombreCompleto || med.nombres || 'Dr. Médico Especialista';
                const nombreMed = normalizarNombreMedico(rawName);
                const cmp = med.cmp || med.CMP || ('CMP 0' + (45000 + idx * 614));
                const consultorio = med.consultorio || ('Consultorio ' + (302 + idx * 2));
                const sedeNombre = state.sedeNombre && state.sedeNombre !== '0' ? state.sedeNombre : 'Sede San Borja';

                // Iniciales limpias
                const nombreLimpio = nombreMed.replace(/^Dr\.\s*|Dra\.\s*/i, '').trim();
                const partes = nombreLimpio.split(/\s+/);
                const initials = (partes.length >= 2 ? partes[0][0] + partes[1][0] : partes[0].substring(0, 2)).toUpperCase();

                // Horarios generados sincrónicamente (sin saltos visuales ni flashes)
                const horas = generarHorariosDoctor(idMed, state.fecha, state.turno);

                const card = document.createElement('div');
                card.className = 'doctor-schedule-card';
                card.id = 'docScheduleCard_' + idMed;

                let slotsHtml = '';
                horas.forEach((h) => {
                    slotsHtml += '<button type="button" class="slot-pill-btn" ' +
                                 'data-med="' + idMed + '" data-hora="' + h + '" ' +
                                 'onclick="seleccionarSlot(this, ' + idMed + ', \'' + nombreMed.replace(/'/g, "\\'") + '\', \'' + h + '\')">' +
                                 h +
                                 '</button>';
                });

                card.innerHTML = 
                    '<div class="doc-card-profile-col">' +
                        '<div class="doc-avatar-circle">' + initials + '</div>' +
                        '<div class="doc-meta-info">' +
                            '<h4 class="doc-name">' + nombreMed + '</h4>' +
                            '<div class="doc-spec-cmp">' + cmp + ' · ' + (state.especialidadNombre || 'Cardiología') + '</div>' +
                            '<div class="doc-location">' +
                                '<i data-lucide="map-pin" style="width: 13px; height: 13px;"></i>' +
                                '<span>' + sedeNombre + ' · ' + consultorio + '</span>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="doc-card-slots-col">' +
                        '<div class="doc-turn-kicker">' + headerTurnoTexto + '</div>' +
                        '<div class="doc-slots-wrap" id="slotsWrap_' + idMed + '">' + slotsHtml + '</div>' +
                    '</div>' +
                    '<div class="doc-card-action-col">' +
                        '<button type="button" class="btn-reservar-slot" id="btnReservar_' + idMed + '" onclick="reservarConDoctor(' + idMed + ', \'' + nombreMed.replace(/'/g, "\\'") + '\')">' +
                            'Reservar cita' +
                        '</button>' +
                    '</div>';

                container.appendChild(card);
            });

            lucide.createIcons();
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

            avanzarAPaso3();
        }

        function avanzarAPaso3() {
            if (!state.medicoId || !state.hora) {
                alert('Por favor selecciona un horario de atención disponible.');
                return;
            }

            document.getElementById('resumenEspecialidad').textContent = state.especialidadNombre || 'Cardiología';
            document.getElementById('resumenMedico').textContent = state.medicoNombre;
            document.getElementById('resumenSede').textContent = (state.sedeNombre === 'Todas las sedes' ? 'Sede Central San Borja' : state.sedeNombre) + ' - Consultorio Especializado';
            document.getElementById('resumenFechaHora').textContent = state.fecha + ' - ' + state.hora;

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
    </script>
</body>
</html>
