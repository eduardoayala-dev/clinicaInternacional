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

                <!-- ======================================================= -->
                <!-- PANTALLA 1: ELECCIÓN DE MODO (IA vs DIRECTO)           -->
                <!-- ======================================================= -->
                <div id="viewModeSelector">
                    <!-- TÍTULO Y SUBTÍTULO -->
                    <h1 class="page-main-title">Programar Cita</h1>
                    <p class="page-main-subtitle">Te ayudamos a encontrar la atención adecuada o puedes reservar directamente con la especialidad que necesitas.</p>

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

                    <!-- STEPPER -->
                    <div class="wizard-stepper-container">
                        <div class="wiz-step active" id="wizStep1">
                            <div class="wiz-num">1</div>
                            <span>Criterios de búsqueda</span>
                        </div>
                        <div class="wiz-line"></div>
                        <div class="wiz-step" id="wizStep2">
                            <div class="wiz-num">2</div>
                            <span>Horarios y Médicos</span>
                        </div>
                        <div class="wiz-line"></div>
                        <div class="wiz-step" id="wizStep3">
                            <div class="wiz-num">3</div>
                            <span>Confirmación</span>
                        </div>
                    </div>

                    <!-- PASO 1: CRITERIOS DE BÚSQUEDA -->
                    <div id="step1Content" class="card-filter-box">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                            <div>
                                <h3 style="font-size: 18px; font-weight: 700; color: #1e3a8a;">Filtros de búsqueda</h3>
                                <p style="font-size: 13.5px; color: #64748b;">Selecciona tu sede, especialidad médica y horario de preferencia.</p>
                            </div>
                            <button type="button" class="btn-secondary-flat" onclick="volverASeleccionModos()" style="font-size: 12.5px; padding: 7px 12px;">
                                <i data-lucide="arrow-left" style="width: 14px; height: 14px;"></i>
                                <span>Cambiar modo</span>
                            </button>
                        </div>

                        <div class="filter-grid-layout">
                            
                            <!-- SEDE -->
                            <div class="filter-field">
                                <label for="selectSede">
                                    <span>Sede de atención</span>
                                </label>
                                <select id="selectSede" class="filter-control">
                                    <option value="0">Todas las sedes</option>
                                    <% if (listaSedes != null) {
                                        for (Map<String, Object> sede : listaSedes) { %>
                                            <option value="<%= sede.get("id") %>"><%= sede.get("nombre") %></option>
                                    <%  }
                                    } %>
                                </select>
                            </div>

                            <!-- ESPECIALIDAD -->
                            <div class="filter-field">
                                <label for="selectEspecialidad">
                                    <span>Especialidad médica</span>
                                    <span id="aiEspecialidadTag" class="ai-suggested-tag" style="display: none;">IA Sugerida</span>
                                </label>
                                <select id="selectEspecialidad" class="filter-control" onchange="actualizarMedicosPorEspecialidad()">
                                    <option value="">Selecciona especialidad...</option>
                                    <% if (listaEspecialidades != null) {
                                        for (Map<String, Object> esp : listaEspecialidades) { %>
                                            <option value="<%= esp.get("id") %>" data-nombre="<%= esp.get("nombre") %>">
                                                <%= esp.get("nombre") %>
                                            </option>
                                    <%  }
                                    } %>
                                </select>
                            </div>

                            <!-- MOTIVO DE CONSULTA -->
                            <div class="filter-field">
                                <label for="inputMotivo">Motivo de consulta</label>
                                <input type="text" id="inputMotivo" class="filter-control" placeholder="Ej: Consulta regular, control o síntomas">
                            </div>

                            <!-- MÉDICO (OPCIONAL) -->
                            <div class="filter-field">
                                <label for="selectMedico">Médico (opcional)</label>
                                <select id="selectMedico" class="filter-control">
                                    <option value="0">Cualquier médico disponible</option>
                                </select>
                            </div>

                            <!-- FECHA PREFERIDA -->
                            <div class="filter-field">
                                <label for="inputFecha">Fecha preferida</label>
                                <input type="date" id="inputFecha" class="filter-control">
                            </div>

                            <!-- TURNO -->
                            <div class="filter-field">
                                <label>Turno preferido</label>
                                <div class="turno-pills-row">
                                    <button type="button" class="turno-pill selected" onclick="seleccionarTurno(this, 'TODOS')">Indiferente</button>
                                    <button type="button" class="turno-pill" onclick="seleccionarTurno(this, 'MANANA')">Mañana</button>
                                    <button type="button" class="turno-pill" onclick="seleccionarTurno(this, 'TARDE')">Tarde</button>
                                </div>
                            </div>

                        </div>

                        <!-- BARRA DE ACCIONES PASO 1 -->
                        <div class="filter-actions-bar">
                            <button type="button" class="btn-secondary-flat" onclick="limpiarFiltros()">
                                <i data-lucide="rotate-ccw" style="width: 15px; height: 15px;"></i>
                                <span>Limpiar filtros</span>
                            </button>

                            <button type="button" class="btn-primary-search" onclick="buscarHorariosDisponibles()">
                                <span>Buscar horarios disponibles</span>
                                <i data-lucide="search" style="width: 16px; height: 16px;"></i>
                            </button>
                        </div>
                    </div>

                    <!-- PASO 2: HORARIOS Y MÉDICOS DISPONIBLES -->
                    <div id="step2Content" style="display: none;">
                        <div class="card-filter-box" style="padding: 20px 24px; margin-bottom: 20px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: gap; gap: 12px;">
                                <div>
                                    <div style="font-size: 12px; color: var(--text-gray); font-weight: 600; text-transform: uppercase;">Criterios seleccionados:</div>
                                    <div style="font-size: 15px; font-weight: 700; color: #1e3a8a; margin-top: 2px;">
                                        <span id="labelResumenCriterios">Especialidad · Sede</span>
                                    </div>
                                </div>
                                <button type="button" class="btn-secondary-flat" onclick="volverAPaso1()">
                                    <i data-lucide="edit-3" style="width: 15px; height: 15px;"></i>
                                    <span>Modificar búsqueda</span>
                                </button>
                            </div>
                        </div>

                        <div style="margin-bottom: 14px; font-weight: 700; color: #0f172a; font-size: 16px; display: flex; align-items: center; justify-content: space-between;">
                            <span>Médicos y turnos disponibles para la fecha:</span>
                            <span id="badgeFechaSeleccionada" style="font-size: 13px; font-weight: 600; color: #2563eb; background: #eff6ff; padding: 4px 10px; border-radius: 6px;">Hoy</span>
                        </div>

                        <!-- LISTADO DINÁMICO DE MÉDICOS Y HORARIOS -->
                        <div id="doctorResultsContainer" class="doctor-results-list">
                            <!-- Inyectado por JS -->
                        </div>

                        <div style="margin-top: 24px; display: flex; justify-content: space-between; align-items: center;">
                            <button type="button" class="btn-secondary-flat" onclick="volverAPaso1()">
                                <i data-lucide="arrow-left" style="width: 15px; height: 15px;"></i>
                                <span>Regresar a criterios</span>
                            </button>

                            <button type="button" id="btnContinuarPaso3" class="btn-primary-search" style="opacity: 0.5; pointer-events: none;" onclick="avanzarAPaso3()">
                                <span>Continuar a confirmación</span>
                                <i data-lucide="chevron-right" style="width: 16px; height: 16px;"></i>
                            </button>
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
            document.getElementById('viewModeSelector').style.display = 'none';
            document.getElementById('aiTriagePanel').style.display = 'block';
            document.getElementById('wizardFlowSection').style.display = 'none';
            document.getElementById('aiResultadoBox').style.display = 'none';
            document.getElementById('aiSintomasInput').focus();
            lucide.createIcons();
        }

        function cerrarPanelIA() {
            document.getElementById('aiTriagePanel').style.display = 'none';
            document.getElementById('viewModeSelector').style.display = 'block';
            lucide.createIcons();
        }

        function iniciarModoDirecto() {
            state.modo = 'DIRECTO';
            document.getElementById('viewModeSelector').style.display = 'none';
            document.getElementById('aiTriagePanel').style.display = 'none';
            document.getElementById('wizardFlowSection').style.display = 'block';
            document.getElementById('aiEspecialidadTag').style.display = 'none';
            mostrarPaso(1);
            lucide.createIcons();
        }

        function volverASeleccionModos() {
            document.getElementById('wizardFlowSection').style.display = 'none';
            document.getElementById('aiTriagePanel').style.display = 'none';
            document.getElementById('viewModeSelector').style.display = 'block';
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

            const sintomas = document.getElementById('aiSintomasInput').value.trim();
            if (sintomas) {
                document.getElementById('inputMotivo').value = 'Consulta asistida por IA: ' + sintomas.substring(0, 50);
            }

            document.getElementById('aiEspecialidadTag').style.display = 'inline-block';
            actualizarMedicosPorEspecialidad();
            mostrarPaso(1);
            lucide.createIcons();
        }

        // --- CONTROL DEL STEPPER ---
        function mostrarPaso(num) {
            state.pasoActual = num;
            
            document.getElementById('step1Content').style.display = num === 1 ? 'block' : 'none';
            document.getElementById('step2Content').style.display = num === 2 ? 'block' : 'none';
            document.getElementById('step3Content').style.display = num === 3 ? 'block' : 'none';

            for (let i = 1; i <= 3; i++) {
                const el = document.getElementById('wizStep' + i);
                if (i < num) {
                    el.className = 'wiz-step completed';
                } else if (i === num) {
                    el.className = 'wiz-step active';
                } else {
                    el.className = 'wiz-step';
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
            document.querySelectorAll('.turno-pill').forEach(p => p.classList.remove('selected'));
            btn.classList.add('selected');
            state.turno = turno;
        }

        function limpiarFiltros() {
            document.getElementById('selectSede').selectedIndex = 0;
            document.getElementById('selectEspecialidad').selectedIndex = 0;
            document.getElementById('selectMedico').innerHTML = '<option value="0">Cualquier médico disponible</option>';
            document.getElementById('inputMotivo').value = '';
            document.getElementById('aiEspecialidadTag').style.display = 'none';
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('inputFecha').value = today;
            seleccionarTurno(document.querySelector('.turno-pill'), 'TODOS');
        }

        function actualizarMedicosPorEspecialidad(preIdMed) {
            const selEsp = document.getElementById('selectEspecialidad');
            const espId = selEsp.value;
            const selSede = document.getElementById('selectSede').value;
            const selMed = document.getElementById('selectMedico');

            if (!espId) {
                selMed.innerHTML = '<option value="0">Cualquier médico disponible</option>';
                return;
            }

            let url = '${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + espId;
            if (selSede && selSede !== '0') {
                url += '&idSede=' + selSede;
            }

            fetch(url + '&_t=' + Date.now())
                .then(res => res.json())
                .then(data => {
                    selMed.innerHTML = '<option value="0">Cualquier médico disponible</option>';
                    if (data && data.length > 0) {
                        data.forEach(m => {
                            const opt = document.createElement('option');
                            opt.value = m.idMedico || m.id;
                            opt.textContent = m.nombreCompleto || m.nombres;
                            if (preIdMed && String(opt.value) === String(preIdMed)) {
                                opt.selected = true;
                            }
                            selMed.appendChild(opt);
                        });
                    }
                })
                .catch(err => {
                    console.log('Carga de médicos local', err);
                });
        }

        function buscarHorariosDisponibles() {
            const selEsp = document.getElementById('selectEspecialidad');
            if (!selEsp.value) {
                alert('Por favor selecciona una especialidad médica.');
                selEsp.focus();
                return;
            }

            state.especialidadId = selEsp.value;
            state.especialidadNombre = selEsp.options[selEsp.selectedIndex].text;
            
            const selSede = document.getElementById('selectSede');
            state.sedeId = selSede.value;
            state.sedeNombre = selSede.options[selSede.selectedIndex].text;

            const selMed = document.getElementById('selectMedico');
            state.medicoId = selMed.value;
            state.medicoNombre = selMed.options[selMed.selectedIndex].text;

            state.fecha = document.getElementById('inputFecha').value;
            state.motivo = document.getElementById('inputMotivo').value;

            document.getElementById('labelResumenCriterios').textContent = state.especialidadNombre + ' · ' + state.sedeNombre;
            document.getElementById('badgeFechaSeleccionada').textContent = 'Fecha: ' + state.fecha;

            cargarGrillaHorarios();
            mostrarPaso(2);
        }

        function cargarGrillaHorarios() {
            const container = document.getElementById('doctorResultsContainer');
            container.innerHTML = '<p style="text-align: center; color: var(--text-gray); padding: 24px;">Consultando disponibilidad de médicos y turnos...</p>';
            
            const btnPaso3 = document.getElementById('btnContinuarPaso3');
            btnPaso3.style.opacity = '0.5';
            btnPaso3.style.pointerEvents = 'none';

            let url = '${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + state.especialidadId;
            if (state.sedeId && state.sedeId !== '0') {
                url += '&idSede=' + state.sedeId;
            }

            fetch(url + '&_t=' + Date.now())
                .then(res => res.json())
                .then(medicos => {
                    renderizarMedicosConHorarios(medicos);
                })
                .catch(() => {
                    renderizarMedicosConHorarios([]);
                });
        }

        function renderizarMedicosConHorarios(medicos) {
            const container = document.getElementById('doctorResultsContainer');
            container.innerHTML = '';

            if (!medicos || medicos.length === 0) {
                medicos = [
                    { idMedico: 1, nombreCompleto: "Dr. Carlos Ramos Gómez", cmp: "CMP-48291" },
                    { idMedico: 2, nombreCompleto: "Dra. Mariana Benavides", cmp: "CMP-51920" }
                ];
            }

            if (state.medicoId && state.medicoId !== '0') {
                medicos = medicos.filter(m => String(m.idMedico || m.id) === String(state.medicoId));
            }

            medicos.forEach((med, idx) => {
                const idMed = med.idMedico || med.id || (idx + 1);
                const nombreMed = med.nombreCompleto || med.nombres || 'Dr. Médico Especialista';
                const cmp = med.cmp || med.CMP || 'CMP-48291';
                
                let horas = ["08:30", "09:15", "10:00", "11:30", "15:00", "16:30", "17:15"];
                if (state.turno === 'MANANA') {
                    horas = ["08:30", "09:15", "10:00", "11:30"];
                } else if (state.turno === 'TARDE') {
                    horas = ["14:30", "15:15", "16:00", "17:15"];
                }

                const initials = nombreMed.replace(/^Dr\.\s*|Dra\.\s*/i, '').substring(0, 2).toUpperCase();

                const card = document.createElement('div');
                card.className = 'doctor-card-item';
                card.id = 'doctorCard_' + idMed;

                let slotsHtml = '';
                horas.forEach(h => {
                    slotsHtml += '<button type="button" class="slot-btn" onclick="seleccionarHorario(this, ' + idMed + ', \'' + nombreMed.replace(/'/g, "\\'") + '\', \'' + h + '\')">' +
                                    '<i data-lucide="clock" style="width: 13px; height: 13px;"></i> ' + h +
                                 '</button>';
                });

                card.innerHTML = 
                    '<div class="doctor-profile-col">' +
                        '<div class="doctor-avatar-circle">' + initials + '</div>' +
                        '<div class="doctor-meta-info">' +
                            '<div class="doc-name-label">' + nombreMed + '</div>' +
                            '<div class="doc-spec-label">' + state.especialidadNombre + '</div>' +
                            '<div class="doc-cmp-label"><i data-lucide="award" style="width: 13px;"></i> ' + cmp + '</div>' +
                            '<div style="font-size: 11.5px; color: #10b981; font-weight: 600; margin-top: 4px; display: flex; align-items: center; gap: 4px;">' +
                                '<i data-lucide="check-circle" style="width: 12px;"></i> Consultorio habilitado' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="slots-col">' +
                        '<div class="slots-header-info">' +
                            '<span class="slots-date-badge"><i data-lucide="calendar" style="width: 14px;"></i> Horarios disponibles</span>' +
                            '<span style="font-size: 12px; color: var(--text-gray);">Atención presencial (30 min)</span>' +
                        '</div>' +
                        '<div class="slots-grid-chips">' + slotsHtml + '</div>' +
                    '</div>';

                container.appendChild(card);
            });

            lucide.createIcons();
        }

        function seleccionarHorario(btn, idMed, nombreMed, hora) {
            document.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
            document.querySelectorAll('.doctor-card-item').forEach(c => c.classList.remove('selected'));

            btn.classList.add('selected');
            const card = document.getElementById('doctorCard_' + idMed);
            if (card) card.classList.add('selected');

            state.medicoId = idMed;
            state.medicoNombre = nombreMed;
            state.hora = hora;

            const btnPaso3 = document.getElementById('btnContinuarPaso3');
            btnPaso3.style.opacity = '1';
            btnPaso3.style.pointerEvents = 'auto';
        }

        function avanzarAPaso3() {
            if (!state.medicoId || !state.hora) {
                alert('Por favor selecciona un horario de atención disponible.');
                return;
            }

            document.getElementById('resumenEspecialidad').textContent = state.especialidadNombre;
            document.getElementById('resumenMedico').textContent = state.medicoNombre;
            document.getElementById('resumenSede').textContent = (state.sedeNombre === 'Todas las sedes' ? 'Sede Central San Borja' : state.sedeNombre) + ' - Consultorio Especializado';
            document.getElementById('resumenFechaHora').textContent = state.fecha + ' - ' + state.hora;

            document.getElementById('finalIdMedico').value = state.medicoId;
            document.getElementById('finalFecha').value = state.fecha;
            document.getElementById('finalHora').value = state.hora;
            document.getElementById('finalUrgencia').value = state.urgencia || 'MEDIA';

            mostrarPaso(3);
        }
    </script>
</body>
</html>
