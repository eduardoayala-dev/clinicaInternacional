<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null) nombrePaciente = "Eduardo Ayala";
    
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
    <style>
        :root {
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --primary-light: #eff6ff;
            --bg-page: #f0f8ff;
            --bg-card: #ffffff;
            --text-dark: #0f172a;
            --text-gray: #64748b;
            --text-light: #94a3b8;
            --border-light: #e2e8f0;
            --border-focus: #93c5fd;
            --radius-lg: 16px;
            --radius-md: 12px;
            --sidebar-width: 260px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }

        body {
            background-color: var(--bg-page);
            color: var(--text-dark);
            display: flex;
            height: 100vh;
            overflow: hidden;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        /* SIDEBAR (Identico al dashboard) */
        .sidebar {
            width: var(--sidebar-width);
            background-color: var(--bg-card);
            border-right: 1px solid var(--border-light);
            display: flex;
            flex-direction: column;
            padding: 24px 20px;
            flex-shrink: 0;
            z-index: 10;
        }

        .brand-container {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 40px;
            padding-left: 8px;
        }
        .brand-container img { height: 36px; }

        .nav-menu {
            display: flex;
            flex-direction: column;
            gap: 8px;
            flex: 1;
        }

        .nav-item {
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 12px 16px;
            border-radius: var(--radius-md);
            color: var(--text-gray);
            text-decoration: none;
            font-size: 14px;
            font-weight: 500;
            transition: all 0.2s;
        }

        .nav-item:hover {
            background-color: #f8fafc;
            color: var(--text-dark);
        }

        .nav-item.active {
            background-color: #eff6ff;
            color: var(--primary);
            font-weight: 600;
        }

        .user-profile-bottom {
            margin-top: auto;
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 16px 8px 8px 8px;
            border-top: 1px solid var(--border-light);
            cursor: pointer;
            text-decoration: none;
        }
        
        .avatar-initials {
            display: flex; align-items: center; justify-content: center;
            border-radius: 50%; font-weight: 600; color: var(--primary);
            background-color: #eff6ff;
        }
        .avatar-sm { width: 36px; height: 36px; font-size: 13px; flex-shrink: 0; }

        .user-name-sm { font-size: 13px; font-weight: 600; color: var(--text-dark); line-height: 1.3; }
        .user-role-sm { font-size: 11px; color: var(--text-gray); }

        /* MAIN CONTENT */
        .main-content {
            flex: 1;
            padding: 40px 50px;
            overflow-y: auto;
        }

        /* WIZARD CONTAINER */
        .wizard-container {
            width: 100%;
            max-width: 900px;
        }

        /* HEADER */
        .page-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 32px;
            width: 100%;
        }

        .header-title { 
            font-family: 'Inter', sans-serif;
            font-size: 28px; 
            font-weight: 700; 
            color: var(--text-dark); 
            margin-bottom: 4px; 
            letter-spacing: -0.5px; 
        }
        
        .header-subtitle { 
            font-size: 14px; 
            color: var(--text-gray); 
        }

        .btn-bell {
            width: 44px; height: 44px;
            border-radius: 12px;
            background: #ffffff;
            border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center;
            color: var(--text-gray);
            position: relative;
            text-decoration: none;
            transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }
        .btn-bell::after {
            content: '';
            position: absolute; top: 10px; right: 12px;
            width: 8px; height: 8px; background: #ef4444; border-radius: 50%;
            border: 2px solid #ffffff;
        }

        .btn-primary {
            background-color: var(--primary);
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 30px;
            font-size: 14px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
            box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);
            transition: all 0.2s;
        }
        .btn-primary:hover { background-color: var(--primary-hover); transform: translateY(-1px); box-shadow: 0 6px 8px -1px rgba(37, 99, 235, 0.3); }

        /* STEPPER */
        .stepper {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 16px;
            margin-bottom: 40px;
            flex-wrap: wrap;
        }

        .step {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            font-weight: 600;
            color: var(--text-light);
        }

        .step.active {
            color: var(--primary);
        }

        .step-num {
            font-size: 12px;
        }

        .step-divider {
            height: 1px;
            width: 40px;
            background-color: var(--border-light);
        }

        /* MAIN CARD */
        .card {
            background: var(--bg-card);
            border-radius: var(--radius-lg);
            padding: 40px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.02);
        }

        .card-header {
            margin-bottom: 24px;
        }

        .card-title {
            font-size: 20px;
            font-weight: 700;
            color: var(--text-dark);
            margin-bottom: 6px;
        }

        .card-subtitle {
            font-size: 14px;
            color: var(--text-gray);
        }

        /* SEARCH INPUT */
        .search-container {
            position: relative;
            margin-bottom: 32px;
        }

        .search-icon {
            position: absolute;
            left: 16px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-light);
        }

        .search-input {
            width: 100%;
            background: #f8fafc;
            border: 1px solid var(--border-light);
            border-radius: var(--radius-md);
            padding: 14px 16px 14px 44px;
            font-size: 15px;
            color: var(--text-dark);
            outline: none;
            transition: all 0.2s;
        }

        .search-input:focus {
            border-color: var(--border-focus);
            background: #ffffff;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }

        .search-input::placeholder { color: var(--text-light); }

        /* SPECIALTIES GRID */
        .specialty-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
            gap: 16px;
            margin-bottom: 40px;
        }

        .specialty-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 16px;
            border: 1px solid var(--border-light);
            border-radius: var(--radius-md);
            cursor: pointer;
            transition: all 0.2s;
            background: #ffffff;
        }

        .specialty-item:hover {
            border-color: var(--border-focus);
            background: #f8fafc;
        }

        .specialty-item.selected {
            border-color: var(--primary);
            background: var(--primary-light);
            box-shadow: 0 0 0 1px var(--primary);
        }

        .spec-info {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .spec-icon-wrapper {
            width: 40px; height: 40px;
            border-radius: 10px;
            background: var(--primary-light);
            color: var(--primary);
            display: flex; align-items: center; justify-content: center;
        }

        .spec-name {
            font-size: 15px;
            font-weight: 600;
            color: var(--text-dark);
            margin-bottom: 2px;
        }

        .spec-meta {
            font-size: 12px;
            color: var(--text-gray);
        }

        .radio-circle {
            width: 20px; height: 20px;
            border-radius: 50%;
            border: 2px solid var(--border-light);
            display: flex; align-items: center; justify-content: center;
            transition: all 0.2s;
        }

        .specialty-item.selected .radio-circle {
            border-color: var(--primary);
        }

        .specialty-item.selected .radio-circle::after {
            content: '';
            width: 10px; height: 10px;
            background: var(--primary);
            border-radius: 50%;
        }

        /* DOCTOR CARD */
        .doctor-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 16px;
            margin-bottom: 40px;
        }

        .doctor-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 20px;
            border: 1px solid var(--border-light);
            border-radius: var(--radius-lg);
            cursor: pointer;
            transition: all 0.2s;
            background: #ffffff;
        }

        .doctor-item:hover {
            border-color: var(--border-focus);
            background: #f8fafc;
        }

        .doctor-item.selected {
            border-color: var(--primary);
            background: var(--primary-light);
            box-shadow: 0 0 0 1px var(--primary);
        }

        .doc-info {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .doc-avatar {
            width: 48px; height: 48px;
            border-radius: 50%;
            background: #e2e8f0;
            display: flex; align-items: center; justify-content: center;
            font-weight: 700; color: #475569; font-size: 16px;
        }

        .doc-name {
            font-size: 15px;
            font-weight: 700;
            color: var(--text-dark);
            margin-bottom: 4px;
        }

        .doc-cmp {
            font-size: 12px;
            color: var(--text-gray);
        }

        /* BOTTOM ACTIONS */
        .actions {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 24px;
            border-top: 1px solid var(--border-light);
            padding-top: 24px;
        }

        .btn-back {
            color: var(--text-gray);
            text-decoration: none;
            font-size: 14px;
            font-weight: 600;
            display: flex; align-items: center; gap: 8px;
            transition: color 0.2s;
        }
        .btn-back:hover { color: var(--text-dark); }

        .btn-continue {
            background: #93c5fd;
            color: #ffffff;
            border: none;
            padding: 12px 28px;
            border-radius: 30px;
            font-size: 15px;
            font-weight: 600;
            cursor: not-allowed;
            display: flex; align-items: center; gap: 8px;
            transition: all 0.2s;
        }

        .btn-continue.active {
            background: var(--primary);
            cursor: pointer;
            box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);
        }

        .btn-continue.active:hover {
            background: var(--primary-hover);
            transform: translateY(-1px);
        }

        /* RESPONSIVE */
        @media (max-width: 768px) {
            .card { padding: 24px; }
            .specialty-grid { grid-template-columns: 1fr; }
            .stepper { flex-wrap: wrap; justify-content: flex-start; gap: 8px; }
            .step-divider { display: none; }
            .step { margin-right: 16px; }
            body { flex-direction: column; overflow-y: auto; }
            .sidebar { width: 100%; height: auto; border-right: none; border-bottom: 1px solid var(--border-light); padding: 16px; }
            .main-content { padding: 24px 16px; }
        }
    </style>
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
            <a href="#" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Panel
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Médicos
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item active">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="#" class="nav-item">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="#" class="nav-item">
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
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell">
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
                    
                    <div class="specialty-item" onclick="selectSpecialty(this, 1)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper"><i data-lucide="heart" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Cardiología</div>
                                <div class="spec-meta">3 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>

                    <div class="specialty-item" onclick="selectSpecialty(this, 2)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper" style="color: #8b5cf6; background: #f5f3ff;"><i data-lucide="brain" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Neurología</div>
                                <div class="spec-meta">3 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>

                    <div class="specialty-item" onclick="selectSpecialty(this, 3)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper" style="color: #10b981; background: #ecfdf5;"><i data-lucide="stethoscope" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Medicina General</div>
                                <div class="spec-meta">3 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>

                    <div class="specialty-item" onclick="selectSpecialty(this, 4)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper" style="color: #d97706; background: #fffbeb;"><i data-lucide="bone" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Traumatología</div>
                                <div class="spec-meta">3 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>

                    <div class="specialty-item" onclick="selectSpecialty(this, 5)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper" style="color: #ef4444; background: #fef2f2;"><i data-lucide="microscope" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Dermatología</div>
                                <div class="spec-meta">3 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>

                    <div class="specialty-item" onclick="selectSpecialty(this, 6)">
                        <div class="spec-info">
                            <div class="spec-icon-wrapper" style="color: #ec4899; background: #fdf2f8;"><i data-lucide="baby" style="width: 20px;"></i></div>
                            <div>
                                <div class="spec-name">Ginecología</div>
                                <div class="spec-meta">3 médicos disponibles</div>
                            </div>
                        </div>
                        <div class="radio-circle"></div>
                    </div>

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
        let selectedMedico = null;
        let currentStep = 1;

        function selectSpecialty(element, id) {
            document.querySelectorAll('.specialty-item').forEach(item => item.classList.remove('selected'));
            element.classList.add('selected');
            selectedEspecialidad = id;

            const btn = document.getElementById('btnContinue');
            btn.classList.add('active');
            btn.disabled = false;
        }

        function selectDoctor(element, id) {
            document.querySelectorAll('.doctor-item').forEach(item => item.classList.remove('selected'));
            element.classList.add('selected');
            selectedMedico = id;

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

                fetch('${pageContext.request.contextPath}/api/medicos?idEspecialidad=' + selectedEspecialidad)
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
                        
                        // Disable btn again for step 2 until a doctor is selected
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
                alert("Flujo de paso 3 en construcción. Medico seleccionado: " + selectedMedico);
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
            } else {
                window.location.href = '${pageContext.request.contextPath}/paciente/dashboard';
            }
        });

        function renderDoctors(doctors) {
            const container = document.getElementById('doctorGridContainer');
            container.innerHTML = '';

            if (doctors.length === 0) {
                container.innerHTML = '<p style="color: var(--text-gray); grid-column: 1/-1; text-align: center; padding: 20px;">No hay médicos disponibles para esta especialidad.</p>';
                return;
            }

            doctors.forEach(doc => {
                const initials = doc.nombreCompleto.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
                
                const div = document.createElement('div');
                div.className = 'doctor-item';
                div.onclick = function() { selectDoctor(this, doc.idMedico); };
                
                div.innerHTML = `
                    <div class="doc-info">
                        <div class="doc-avatar">${initials}</div>
                        <div>
                            <div class="doc-name">${doc.nombreCompleto}</div>
                            <div class="doc-cmp">CMP: ${doc.cmp}</div>
                        </div>
                    </div>
                    <div class="radio-circle"></div>
                `;
                container.appendChild(div);
            });
        }
    </script>
</body>
</html>
