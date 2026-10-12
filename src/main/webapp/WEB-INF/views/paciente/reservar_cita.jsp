<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null) nombrePaciente = "Eduardo Ayala";
    
    List<Map<String, Object>> listaEspecialidades = (List<Map<String, Object>>) request.getAttribute("listaEspecialidades");

    // Obtener iniciales del paciente
    String inicialesPaciente = "EA";
    if (nombrePaciente != null && !nombrePaciente.isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    pe.edu.utp.clinica.dao.CitaDAO citaDAO_bell = new pe.edu.utp.clinica.dao.CitaDAO();
    boolean tieneNotifSinLeer = usuario != null && citaDAO_bell.contarNotificacionesSinLeer(usuario.getIdUsuario()) > 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Programar Cita - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservar-cita.css">
</head>
<body>

    <!-- SIDEBAR -->
    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="home" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/paciente/panel" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Panel
            </a>
            <a href="${pageContext.request.contextPath}/paciente/medicos" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Médicos
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item active">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="${pageContext.request.contextPath}/paciente/reportes" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Reportes
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
                <i data-lucide="bell" style="width: 18px;"></i> Alertas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi perfil
            </a>
        </nav>

        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials avatar-sm"><%= inicialesPaciente %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombrePaciente %></div>
                <div class="user-role-sm">Paciente</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <!-- MAIN CONTENT -->
    <main class="main-content">
        <div class="page-header">
            <div>
                <h1 class="header-title">Programar Cita</h1>
                <p class="header-subtitle">Completa los pasos para agendar tu consulta médica.</p>
            </div>
            <div style="display: flex; align-items: center;">
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell <%= tieneNotifSinLeer ? "has-unread" : "" %>">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
            </div>
        </div>

        <div class="wizard-container" style="margin: 0 auto;">
            
            <div class="stepper">
                <div class="step active" id="stepper-1"><span class="step-num">1</span> Especialidad</div>
                <div class="step-divider"></div>
                <div class="step" id="stepper-2"><span class="step-num">2</span> Médico</div>
                <div class="step-divider"></div>
                <div class="step" id="stepper-3"><span class="step-num">3</span> Fecha y hora</div>
                <div class="step-divider"></div>
                <div class="step" id="stepper-4"><span class="step-num">4</span> Detalles</div>
                <div class="step-divider"></div>
                <div class="step" id="stepper-5"><span class="step-num">5</span> Confirmar</div>
            </div>

            <div class="card">
                <!-- PASO 1: ESPECIALIDAD -->
                <div id="step1-content">
                    <div class="card-header">
                        <h2 class="card-title">Selecciona una especialidad</h2>
                        <p class="card-subtitle">Elige el área médica de tu consulta.</p>
                    </div>

                    <div class="search-container">
                        <i data-lucide="search" class="search-icon" style="width: 18px;"></i>
                        <input type="text" class="search-input" placeholder="Buscar especialidad..." id="searchInput">
                    </div>

                <div class="specialty-grid">
                    <% if (listaEspecialidades != null && !listaEspecialidades.isEmpty()) {
                        for (Map<String, Object> esp : listaEspecialidades) {
                            int idEsp = (Integer) esp.get("id");
                            String nombreEsp = (String) esp.get("nombre");
                            int totalMed = esp.get("totalMedicos") != null ? (Integer) esp.get("totalMedicos") : 2;
                            
                            String icon = "stethoscope";
                            String lower = nombreEsp.toLowerCase();
                            if (lower.contains("cardio")) icon = "heart";
                            else if (lower.contains("neuro")) icon = "brain";
                            else if (lower.contains("general")) icon = "stethoscope";
                            else if (lower.contains("trauma")) icon = "bone";
                            else if (lower.contains("derma")) icon = "microscope";
                            else if (lower.contains("gineco")) icon = "baby";
                    %>
                    <div class="specialty-item" onclick="selectSpecialty(this, '<%= idEsp %>')">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper"><i data-lucide="<%= icon %>" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name"><%= nombreEsp %></div>
                                <div class="spec-meta"><%= totalMed %> <%= totalMed == 1 ? "médico disponible" : "médicos disponibles" %></div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>
                    <%  }
                    } else { %>
                    <div class="specialty-item" onclick="selectSpecialty(this, 1)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper"><i data-lucide="heart" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Cardiología</div>
                                <div class="spec-meta">2 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>
                    <% } %>
                </div>
                </div> <!-- END STEP 1 -->

                <!-- PASO 2: MEDICO -->
                <div id="step2-content" style="display: none;">
                    <div class="card-header">
                        <h2 class="card-title">Selecciona un médico</h2>
                        <p class="card-subtitle">Elige al profesional de tu preferencia.</p>
                    </div>

                    <div class="search-container">
                        <i data-lucide="search" class="search-icon" style="width: 18px;"></i>
                        <input type="text" class="search-input" placeholder="Buscar médico..." id="searchMedicoInput">
                    </div>

                    <div class="doctor-grid" id="doctorGridContainer">
                        <!-- Llenado por AJAX -->
                    </div>
                </div> <!-- END STEP 2 -->

                <!-- PASO 3: FECHA Y HORA -->
                <div id="step3-content" style="display: none;">
                    <div class="card-header">
                        <h2 class="card-title">Selecciona fecha y hora</h2>
                        <p class="card-subtitle">Elige el día y la hora de atención de tu preferencia.</p>
                    </div>

                    <!-- BARRA ACCESIBLE DE MES Y NAVEGACION -->
                    <div style="background: #ffffff; padding: 16px 20px; border-radius: var(--radius-md); border: 1px solid var(--border-light); margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.02);">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <div style="width: 40px; height: 40px; border-radius: 10px; background: #eff6ff; color: var(--primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                                <i data-lucide="calendar" style="width: 20px; height: 20px;"></i>
                            </div>
                            <div>
                                <div style="font-size: 11px; color: var(--text-gray); text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">Mes de atención</div>
                                <div id="currentMonthYearLabel" style="font-size: 16px; font-weight: 700; color: var(--text-dark);">Octubre 2026</div>
                            </div>
                        </div>

                        <div style="display: flex; align-items: center; gap: 12px;">
                            <!-- Selector de Mes Accesible -->
                            <div style="display: flex; align-items: center; gap: 6px;">
                                <label for="monthFilterSelect" style="font-size: 13px; font-weight: 600; color: var(--text-gray);">Filtrar por mes:</label>
                                <select id="monthFilterSelect" onchange="filterDatesByMonth(this.value)" style="padding: 8px 14px; border: 1.5px solid var(--border-light); border-radius: 20px; background: #ffffff; font-size: 13px; font-weight: 600; color: var(--text-dark); outline: none; cursor: pointer;">
                                    <!-- Llenado dinámicamente -->
                                </select>
                            </div>

                            <!-- Botones de Desplazamiento Lateral -->
                            <div style="display: flex; gap: 6px;">
                                <button type="button" onclick="scrollDates(-1)" style="width: 36px; height: 36px; border-radius: 50%; border: 1.5px solid var(--border-light); background: #ffffff; color: var(--text-dark); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s;" title="Días anteriores">
                                    <i data-lucide="chevron-left" style="width: 18px; height: 18px;"></i>
                                </button>
                                <button type="button" onclick="scrollDates(1)" style="width: 36px; height: 36px; border-radius: 50%; border: 1.5px solid var(--border-light); background: #ffffff; color: var(--text-dark); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s;" title="Días siguientes">
                                    <i data-lucide="chevron-right" style="width: 18px; height: 18px;"></i>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div style="margin-bottom: 12px; font-weight: 600; color: var(--text-dark); font-size: 14px; display: flex; align-items: center; gap: 6px;">
                        <span>Días con citas disponibles:</span>
                    </div>
                    <div class="date-container" id="dateContainer">
                        <!-- Llenado por AJAX -->
                    </div>

                    <div style="margin-top: 24px; margin-bottom: 16px; font-weight: 600; color: var(--text-dark); font-size: 14px; display: flex; align-items: center; gap: 8px;">
                        <i data-lucide="clock" style="width: 18px; color: var(--primary);"></i> Horarios de atención disponibles
                    </div>
                    <div class="time-grid" id="timeGridContainer">
                        <!-- Llenado por AJAX al seleccionar un día -->
                        <p style="color: var(--text-gray); grid-column: 1/-1;">Selecciona una fecha primero para ver los horarios.</p>
                    </div>
                </div> <!-- END STEP 3 -->

                <!-- PASO 4: CONFIRMAR -->
                <div id="step4-content" style="display: none;">
                    <div class="card-header" style="margin-bottom: 24px;">
                        <h2 class="card-title">Confirma tu Cita Médica</h2>
                        <p class="card-subtitle">Verifica los datos seleccionados antes de agendar tu atención.</p>
                    </div>

                    <div style="background: #ffffff; padding: 24px; border-radius: var(--radius-lg); border: 1px solid var(--border-light); box-shadow: 0 4px 16px rgba(0,0,0,0.02); margin-bottom: 24px;">
                        
                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 24px; padding-bottom: 24px; border-bottom: 1px solid var(--border-light);">
                            <!-- Especialidad -->
                            <div style="display: flex; gap: 14px; align-items: center; background: #f8fafc; padding: 14px; border-radius: var(--radius-md); border: 1px solid #f1f5f9;">
                                <div style="width: 42px; height: 42px; border-radius: 10px; background: #eff6ff; color: var(--primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                                    <i data-lucide="stethoscope" style="width: 20px; height: 20px;"></i>
                                </div>
                                <div>
                                    <div style="font-size: 11px; color: var(--text-gray); text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">Especialidad</div>
                                    <div id="resumenEspecialidad" style="font-weight: 700; color: var(--text-dark); font-size: 15px; margin-top: 2px;">...</div>
                                </div>
                            </div>

                            <!-- Médico -->
                            <div style="display: flex; gap: 14px; align-items: center; background: #f8fafc; padding: 14px; border-radius: var(--radius-md); border: 1px solid #f1f5f9;">
                                <div style="width: 42px; height: 42px; border-radius: 10px; background: #eff6ff; color: var(--primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                                    <i data-lucide="user-check" style="width: 20px; height: 20px;"></i>
                                </div>
                                <div>
                                    <div style="font-size: 11px; color: var(--text-gray); text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">Médico Tratante</div>
                                    <div id="resumenMedico" style="font-weight: 700; color: var(--text-dark); font-size: 15px; margin-top: 2px;">...</div>
                                </div>
                            </div>

                            <!-- Fecha -->
                            <div style="display: flex; gap: 14px; align-items: center; background: #f8fafc; padding: 14px; border-radius: var(--radius-md); border: 1px solid #f1f5f9;">
                                <div style="width: 42px; height: 42px; border-radius: 10px; background: #eff6ff; color: var(--primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                                    <i data-lucide="calendar" style="width: 20px; height: 20px;"></i>
                                </div>
                                <div>
                                    <div style="font-size: 11px; color: var(--text-gray); text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">Fecha</div>
                                    <div id="resumenFecha" style="font-weight: 700; color: var(--text-dark); font-size: 15px; margin-top: 2px;">...</div>
                                </div>
                            </div>

                            <!-- Hora -->
                            <div style="display: flex; gap: 14px; align-items: center; background: #f8fafc; padding: 14px; border-radius: var(--radius-md); border: 1px solid #f1f5f9;">
                                <div style="width: 42px; height: 42px; border-radius: 10px; background: #eff6ff; color: var(--primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                                    <i data-lucide="clock" style="width: 20px; height: 20px;"></i>
                                </div>
                                <div>
                                    <div style="font-size: 11px; color: var(--text-gray); text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">Hora</div>
                                    <div id="resumenHora" style="font-weight: 700; color: var(--text-dark); font-size: 15px; margin-top: 2px;">...</div>
                                </div>
                            </div>
                        </div>

                        <!-- Prioridad de Atención -->
                        <div style="background: #ffffff; padding: 20px; border-radius: var(--radius-md); border: 1px solid var(--border-light);">
                            <label style="font-weight: 600; color: var(--text-dark); display: block; margin-bottom: 8px; font-size: 14px;">
                                Nivel de Urgencia / Prioridad de Atención:
                            </label>
                            <select id="urgenciaSelect" style="width: 100%; padding: 12px 16px; border: 1px solid var(--border-light); border-radius: var(--radius-md); background: #ffffff; font-size: 14px; font-weight: 500; color: var(--text-dark); outline: none; cursor: pointer; transition: all 0.2s;">
                                <option value="BAJA">Baja - Control o chequeo de rutina (Sin dolor)</option>
                                <option value="MEDIA">Media - Molestia persistente o dolor tolerable</option>
                                <option value="ALTA">Alta - Sintomatología severa o prioridad clínica</option>
                            </select>
                            <p style="font-size: 12px; color: var(--text-gray); margin-top: 12px; display: flex; align-items: center; gap: 6px;">
                                <i data-lucide="shield-check" style="width: 15px; height: 15px; color: var(--primary);"></i>
                                Tu reserva será confirmada de inmediato en el sistema de la clínica.
                            </p>
                        </div>
                    </div>

                    <form id="formReserva" method="POST" action="${pageContext.request.contextPath}/paciente/reservar" style="display:none;">
                        <input type="hidden" name="idMedico" id="inputMedico">
                        <input type="hidden" name="fecha" id="inputFecha">
                        <input type="hidden" name="hora" id="inputHora">
                        <input type="hidden" name="urgencia" id="inputUrgencia">
                    </form>
                </div> <!-- END STEP 4 -->

                <div class="actions">
                    <button class="btn-back" id="btnBack" style="background: none; border: none; cursor: pointer;">
                        <i data-lucide="arrow-left" style="width: 16px;"></i> Cancelar
                    </button>
                    <button class="btn-continue" id="btnContinue" disabled>
                        <span id="btnContinueText">Continuar</span> <i data-lucide="arrow-right" style="width: 16px;" id="btnContinueIcon"></i>
                    </button>
                </div>
            </div>

        </div>
    </main>

    <script>
        lucide.createIcons();

        let selectedEspecialidad = null;
        let selectedEspecialidadNombre = "${preEspNombre}";
        let selectedMedico = null;
        let selectedMedicoNombre = "${preMedNombre}";
        let selectedFecha = null;
        let selectedHora = null;
        let currentStep = 1;

        function selectSpecialty(element, id) {
            document.querySelectorAll('.specialty-item').forEach(item => item.classList.remove('selected'));
            element.classList.add('selected');
            selectedEspecialidad = id;
            const specElem = element.querySelector('.spec-name');
            if (specElem) selectedEspecialidadNombre = specElem.textContent.trim();

            const btn = document.getElementById('btnContinue');
            btn.classList.add('active');
            btn.disabled = false;
        }

        function selectDoctor(element, id) {
            document.querySelectorAll('.doctor-item').forEach(item => item.classList.remove('selected'));
            element.classList.add('selected');
            selectedMedico = id;
            const docElem = element.querySelector('.doc-name');
            if (docElem) selectedMedicoNombre = docElem.textContent.trim();

            const btn = document.getElementById('btnContinue');
            btn.classList.add('active');
            btn.disabled = false;
        }

        // Búsqueda Especialidad
        document.getElementById('searchInput').addEventListener('input', function(e) {
            const term = e.target.value.toLowerCase();
            document.querySelectorAll('.specialty-item').forEach(item => {
                const name = item.querySelector('.spec-name').textContent.toLowerCase();
                item.style.display = name.includes(term) ? 'flex' : 'none';
            });
        });

        // Búsqueda Médico
        document.getElementById('searchMedicoInput').addEventListener('input', function(e) {
            const term = e.target.value.toLowerCase();
            document.querySelectorAll('.doctor-item').forEach(item => {
                const name = item.querySelector('.doc-name').textContent.toLowerCase();
                item.style.display = name.includes(term) ? 'flex' : 'none';
            });
        });

        // Navegación (Continuar / Atrás)
        document.getElementById('btnContinue').addEventListener('click', function() {
            if (currentStep === 1 && selectedEspecialidad) {
                // Cargar doctores
                const btnText = document.getElementById('btnContinueText');
                const btnIcon = document.getElementById('btnContinueIcon');
                btnText.textContent = "Cargando...";
                btnIcon.style.display = "none";
                this.disabled = true;

                fetch('${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + selectedEspecialidad + '&_t=' + new Date().getTime())
                    .then(response => response.json())
                    .then(data => {
                        renderDoctors(data);
                        // Cambiar UI
                        document.getElementById('step1-content').style.display = 'none';
                        document.getElementById('step2-content').style.display = 'block';
                        document.getElementById('stepper-1').classList.remove('active');
                        document.getElementById('stepper-2').classList.add('active');
                        
                        document.getElementById('btnBack').innerHTML = '<i data-lucide="arrow-left" style="width: 16px;"></i> Atrás';
                        
                        btnText.textContent = "Continuar";
                        btnIcon.style.display = "block";
                        
                        selectedMedico = null;
                        this.classList.remove('active');
                        this.disabled = true;
                        currentStep = 2;
                        lucide.createIcons();
                    })
                    .catch(err => {
                        console.error(err);
                        alert("Error al cargar los médicos.");
                        btnText.textContent = "Continuar";
                        btnIcon.style.display = "block";
                        this.disabled = false;
                    });
            } else if (currentStep === 2 && selectedMedico) {
                // Siguiente paso (Fecha y Hora)
                const btnText = document.getElementById('btnContinueText');
                const btnIcon = document.getElementById('btnContinueIcon');
                btnText.textContent = "Cargando...";
                btnIcon.style.display = "none";
                this.disabled = true;

                fetch('${pageContext.request.contextPath}/api/horarios?idMedico=' + selectedMedico + '&_t=' + new Date().getTime())
                    .then(response => response.json())
                    .then(data => {
                        selectedFecha = null;
                        selectedHora = null;
                        document.getElementById('step2-content').style.display = 'none';
                        document.getElementById('step3-content').style.display = 'block';
                        document.getElementById('stepper-2').classList.remove('active');
                        document.getElementById('stepper-3').classList.add('active');
                        
                        btnText.textContent = "Continuar";
                        btnIcon.style.display = "block";
                        
                        this.classList.remove('active');
                        this.disabled = true;
                        currentStep = 3;

                        renderDates(data);
                    })
                    .catch(err => {
                        console.error(err);
                        alert("Error al cargar las fechas.");
                        btnText.textContent = "Continuar";
                        btnIcon.style.display = "block";
                        this.disabled = false;
                    });
            } else if (currentStep === 3 && selectedFecha && selectedHora) {
                // Siguiente paso (Confirmar)
                document.getElementById('step3-content').style.display = 'none';
                document.getElementById('step4-content').style.display = 'block';
                document.getElementById('stepper-3').classList.remove('active');
                document.getElementById('stepper-4').classList.add('active');
                
                // Llenar resumen
                let medName = selectedMedicoNombre;
                if (!medName || medName === "") {
                    const medicoItem = document.querySelector('.doctor-item.selected .doc-name');
                    medName = medicoItem ? medicoItem.textContent.trim() : "${preMedNombre}";
                }
                if (!medName || medName === "" || medName.includes("null")) medName = "Dr. Médico Especialista";

                let espName = selectedEspecialidadNombre;
                if (!espName || espName === "") {
                    const specItem = document.querySelector('.specialty-item.selected .spec-name');
                    espName = specItem ? specItem.textContent.trim() : "${preEspNombre}";
                }
                if (!espName || espName === "" || espName.includes("null")) espName = "Consulta Médica";

                document.getElementById('resumenMedico').textContent = medName;
                document.getElementById('resumenEspecialidad').textContent = espName;
                document.getElementById('resumenFecha').textContent = selectedFecha;
                document.getElementById('resumenHora').textContent = selectedHora;

                const btnText = document.getElementById('btnContinueText');
                const btnIcon = document.getElementById('btnContinueIcon');
                btnText.textContent = "Confirmar Cita";
                btnIcon.setAttribute('data-lucide', 'check');
                lucide.createIcons();
                currentStep = 4;

            } else if (currentStep === 4) {
                // Enviar formulario
                document.getElementById('inputMedico').value = selectedMedico;
                document.getElementById('inputFecha').value = selectedFecha;
                document.getElementById('inputHora').value = selectedHora;
                document.getElementById('inputUrgencia').value = document.getElementById('urgenciaSelect').value;
                
                const btnText = document.getElementById('btnContinueText');
                btnText.textContent = "Procesando...";
                this.disabled = true;
                
                document.getElementById('formReserva').submit();
            }
        });

        document.getElementById('btnBack').addEventListener('click', function() {
            if (currentStep === 2) {
                document.getElementById('step2-content').style.display = 'none';
                document.getElementById('step1-content').style.display = 'block';
                document.getElementById('stepper-2').classList.remove('active');
                document.getElementById('stepper-1').classList.add('active');
                
                this.innerHTML = '<i data-lucide="arrow-left" style="width: 16px;"></i> Cancelar';
                
                const btn = document.getElementById('btnContinue');
                btn.classList.add('active');
                btn.disabled = false;
                currentStep = 1;
            } else if (currentStep === 3) {
                document.getElementById('step3-content').style.display = 'none';
                document.getElementById('step2-content').style.display = 'block';
                document.getElementById('stepper-3').classList.remove('active');
                document.getElementById('stepper-2').classList.add('active');
                
                this.innerHTML = '<i data-lucide="arrow-left" style="width: 16px;"></i> Cambiar Especialidad';
                
                const docContainer = document.getElementById('doctorGridContainer');
                if (!docContainer.children || docContainer.children.length === 0 || docContainer.querySelector('p')) {
                    const espId = selectedEspecialidad || 1;
                    docContainer.innerHTML = '<p style="color: var(--text-gray); grid-column: 1/-1; text-align: center; padding: 20px;">Cargando médicos...</p>';
                    fetch('${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + espId + '&_t=' + new Date().getTime())
                        .then(r => r.json())
                        .then(data => {
                            renderDoctors(data);
                            lucide.createIcons();
                        });
                }
                
                const btn = document.getElementById('btnContinue');
                btn.classList.add('active');
                btn.disabled = false;
                currentStep = 2;
                lucide.createIcons();
            } else if (currentStep === 4) {
                document.getElementById('step4-content').style.display = 'none';
                document.getElementById('step3-content').style.display = 'block';
                document.getElementById('stepper-4').classList.remove('active');
                document.getElementById('stepper-3').classList.add('active');
                
                const btnText = document.getElementById('btnContinueText');
                const btnIcon = document.getElementById('btnContinueIcon');
                btnText.textContent = "Continuar";
                btnIcon.setAttribute('data-lucide', 'arrow-right');
                lucide.createIcons();
                currentStep = 3;
            } else {
                window.location.href = '${pageContext.request.contextPath}/paciente/dashboard';
            }
        });

        function renderDoctors(doctors) {
            const container = document.getElementById('doctorGridContainer');
            container.innerHTML = '';

            if (!doctors || doctors.length === 0) {
                container.innerHTML = '<p style="color: var(--text-gray); grid-column: 1/-1; text-align: center; padding: 20px;">No hay médicos disponibles para esta especialidad.</p>';
                return;
            }

            doctors.forEach(doc => {
                let rawName = doc.nombreCompleto || doc.nombre || (doc.nombres ? (doc.nombres + ' ' + (doc.apellidos || '')) : '');
                if (!rawName || rawName.trim() === '' || rawName.includes('null')) {
                    rawName = "Dr. Médico Especialista";
                }
                const docName = rawName.trim();
                const idMed = doc.idMedico || doc.id_medico || doc.id || 1;

                const parts = docName.replace(/^Dr\.\s*|Dra\.\s*/i, '').split(' ').filter(Boolean);
                let initials = "DR";
                if (parts.length >= 2) {
                    initials = (parts[0][0] + parts[1][0]).toUpperCase();
                } else if (parts.length === 1) {
                    initials = parts[0].substring(0, 2).toUpperCase();
                }
                
                const div = document.createElement('div');
                div.className = 'doctor-item';
                if (selectedMedico && idMed === selectedMedico) {
                    div.classList.add('selected');
                }
                div.onclick = function() { selectDoctor(this, idMed); };
                
                div.innerHTML = 
                    '<div class="doc-info">' +
                        '<div class="doc-avatar">' + initials + '</div>' +
                        '<div>' +
                            '<div class="doc-name">' + docName + '</div>' +
                            '<div class="doc-cmp" style="color: var(--primary); font-weight: 500;">Especialista Médico</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="radio-circle"></div>';
                container.appendChild(div);
            });
        }

        let allAvailableFechas = [];

        function renderDates(fechas) {
            allAvailableFechas = fechas || [];
            const container = document.getElementById('dateContainer');
            container.innerHTML = '';
            document.getElementById('timeGridContainer').innerHTML = '<p style="color: var(--text-gray); grid-column: 1/-1;">Selecciona una fecha primero para ver los horarios.</p>';

            if (!fechas || fechas.length === 0) {
                container.innerHTML = '<p style="color: var(--text-gray);">El médico no tiene horarios publicados aún.</p>';
                document.getElementById('currentMonthYearLabel').textContent = 'Sin fechas disponibles';
                return;
            }

            const diasSemana = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];
            const mesesLargos = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];

            // 1. Identificar meses únicos para el selector
            const mesesMap = {};
            fechas.forEach(item => {
                let fechaStr = typeof item === 'string' ? item : (item.fecha || item.toString());
                if (!fechaStr) return;
                const parts = fechaStr.split('T')[0].split('-');
                if (parts.length === 3) {
                    const key = parts[0] + '-' + parts[1]; // YYYY-MM
                    const monthIdx = parseInt(parts[1], 10) - 1;
                    mesesMap[key] = mesesLargos[monthIdx] + ' ' + parts[0];
                }
            });

            const monthSelect = document.getElementById('monthFilterSelect');
            monthSelect.innerHTML = '<option value="ALL">Todos los meses</option>';
            Object.keys(mesesMap).forEach(key => {
                const opt = document.createElement('option');
                opt.value = key;
                opt.textContent = mesesMap[key];
                monthSelect.appendChild(opt);
            });

            // 2. Renderizar tarjetas de fecha
            fechas.forEach((item, index) => {
                let fechaStr = typeof item === 'string' ? item : (item.fecha || item.toString());
                if (!fechaStr) return;
                
                const cleanStr = fechaStr.split('T')[0];
                const parts = cleanStr.split('-');
                if (parts.length !== 3) return;
                
                const year = parseInt(parts[0], 10);
                const month = parseInt(parts[1], 10) - 1;
                const day = parseInt(parts[2], 10);
                
                const docDate = new Date(year, month, day);
                const dayNum = day < 10 ? '0' + day : '' + day;
                const dayName = diasSemana[docDate.getDay()].substring(0, 3); // "Mié"
                const monthName = mesesLargos[month]; // "Octubre"
                
                const div = document.createElement('div');
                div.className = 'date-item';
                div.setAttribute('data-month-key', parts[0] + '-' + parts[1]);
                div.onclick = function() { selectDate(this, cleanStr, monthName + ' ' + year); };
                
                div.innerHTML = 
                    '<div class="date-weekday">' + dayName + '</div>' +
                    '<div class="date-num">' + dayNum + '</div>' +
                    '<div class="date-month" style="font-size: 10px;">' + monthName + '</div>';
                container.appendChild(div);

                if (index === 0) {
                    document.getElementById('currentMonthYearLabel').textContent = monthName + ' ' + year;
                }
            });

            // Auto-seleccionar primer día disponible
            const firstDateItem = container.querySelector('.date-item');
            if (firstDateItem) {
                firstDateItem.click();
            }
        }

        function filterDatesByMonth(monthKey) {
            const items = document.querySelectorAll('#dateContainer .date-item');
            let firstMatch = null;
            items.forEach(item => {
                if (monthKey === 'ALL' || item.getAttribute('data-month-key') === monthKey) {
                    item.style.display = 'flex';
                    if (!firstMatch) firstMatch = item;
                } else {
                    item.style.display = 'none';
                }
            });
        }

        function scrollDates(direction) {
            const container = document.getElementById('dateContainer');
            container.scrollBy({ left: direction * 220, behavior: 'smooth' });
        }

        function selectDate(element, fechaStr, monthLabel) {
            document.querySelectorAll('.date-item').forEach(item => item.classList.remove('selected'));
            element.classList.add('selected');
            selectedFecha = fechaStr;
            selectedHora = null;

            if (monthLabel) {
                document.getElementById('currentMonthYearLabel').textContent = monthLabel;
            }

            const btn = document.getElementById('btnContinue');
            btn.classList.remove('active');
            btn.disabled = true;

            const timeContainer = document.getElementById('timeGridContainer');
            timeContainer.innerHTML = '<p style="color: var(--text-gray); grid-column: 1/-1;">Cargando horas de atención...</p>';

            const url = '${pageContext.request.contextPath}/api/horarios?idMedico=' + selectedMedico + '&fecha=' + fechaStr + '&_t=' + new Date().getTime();
            fetch(url)
                .then(response => response.json())
                .then(data => {
                    renderTimes(data);
                })
                .catch(err => {
                    console.error(err);
                    timeContainer.innerHTML = '<p style="color: red; grid-column: 1/-1;">Error al cargar horas.</p>';
                });
        }

        function renderTimes(horas) {
            const container = document.getElementById('timeGridContainer');
            container.innerHTML = '';

            if (!horas || horas.length === 0) {
                container.innerHTML = '<p style="color: var(--text-gray); grid-column: 1/-1;">No hay horas disponibles para este día.</p>';
                return;
            }

            // Comprobar si la fecha seleccionada es el día de hoy
            const hoy = new Date();
            const yearHoy = hoy.getFullYear();
            const monthHoy = (hoy.getMonth() + 1 < 10 ? '0' : '') + (hoy.getMonth() + 1);
            const dayHoy = (hoy.getDate() < 10 ? '0' : '') + hoy.getDate();
            const fechaHoyStr = yearHoy + '-' + monthHoy + '-' + dayHoy;

            const esHoy = selectedFecha === fechaHoyStr;
            const ahoraMinutos = hoy.getHours() * 60 + hoy.getMinutes();

            let primerValido = null;

            horas.forEach(item => {
                const horaStr = typeof item === 'string' ? item : (item.hora || item.toString());
                const div = document.createElement('div');
                div.className = 'time-chip';

                let yaTranscurrio = false;
                if (esHoy) {
                    const partes = horaStr.split(':');
                    if (partes.length >= 2) {
                        const hMin = parseInt(partes[0], 10) * 60 + parseInt(partes[1], 10);
                        if (hMin <= ahoraMinutos) {
                            yaTranscurrio = true;
                        }
                    }
                }

                if (yaTranscurrio) {
                    div.classList.add('disabled');
                    div.title = "Horario no disponible (ya transcurrido)";
                } else {
                    div.onclick = function() { selectTime(this, horaStr); };
                    if (!primerValido) {
                        primerValido = div;
                    }
                }
                div.textContent = horaStr;
                container.appendChild(div);
            });

            // Auto-seleccionar primer horario disponible HABILITADO
            if (primerValido) {
                primerValido.click();
            } else {
                selectedHora = null;
                const btn = document.getElementById('btnContinue');
                btn.classList.remove('active');
                btn.disabled = true;
                if (esHoy) {
                    const msg = document.createElement('p');
                    msg.style.color = '#ef4444';
                    msg.style.gridColumn = '1/-1';
                    msg.style.fontSize = '13px';
                    msg.style.marginTop = '8px';
                    msg.style.fontWeight = '500';
                    msg.textContent = 'No quedan turnos disponibles para el resto del día de hoy. Por favor selecciona otro día.';
                    container.appendChild(msg);
                }
            }
        }

        function selectTime(element, hora) {
            if (element.classList.contains('disabled')) return;
            document.querySelectorAll('.time-chip').forEach(item => item.classList.remove('selected'));
            element.classList.add('selected');
            selectedHora = hora;

            const btn = document.getElementById('btnContinue');
            btn.classList.add('active');
            btn.disabled = false;
        }

        document.addEventListener('DOMContentLoaded', function() {
            const preMed = "${preMedId}";
            const preEsp = "${preEspId}";
            const preMedName = "${preMedNombre}";
            const preEspName = "${preEspNombre}";

            if (preMed && preMed !== "") {
                selectedMedico = parseInt(preMed);
                if (preMedName && preMedName !== "" && !preMedName.includes("null")) {
                    selectedMedicoNombre = preMedName;
                }
                if (preEsp && preEsp !== "") {
                    selectedEspecialidad = parseInt(preEsp);
                } else {
                    selectedEspecialidad = 1;
                }
                if (preEspName && preEspName !== "" && !preEspName.includes("null")) {
                    selectedEspecialidadNombre = preEspName;
                }
                
                // Pre-cargar la lista de médicos de la especialidad por si el usuario hace clic en "Cambiar Médico"
                fetch('${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + selectedEspecialidad + '&_t=' + new Date().getTime())
                    .then(r => r.json())
                    .then(data => renderDoctors(data));

                fetch('${pageContext.request.contextPath}/api/horarios?idMedico=' + selectedMedico + '&_t=' + new Date().getTime())
                    .then(r => r.json())
                    .then(data => {
                        renderDates(data);
                        document.getElementById('step1-content').style.display = 'none';
                        document.getElementById('step2-content').style.display = 'none';
                        document.getElementById('step3-content').style.display = 'block';
                        document.getElementById('stepper-1').classList.remove('active');
                        document.getElementById('stepper-2').classList.remove('active');
                        document.getElementById('stepper-3').classList.add('active');
                        document.getElementById('btnBack').innerHTML = '<i data-lucide="arrow-left" style="width: 16px;"></i> Cambiar Médico';
                        currentStep = 3;
                        lucide.createIcons();
                    });
            }
        });
    </script>
</body>
</html>

