<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String inicialesMedico = "CM";
    if (nombreMedico != null && !nombreMedico.trim().isEmpty()) {
        String[] partes = nombreMedico.replace("Dr.", "").replace("Dra.", "").trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesMedico = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesMedico = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    String mensajeExito = (String) request.getAttribute("mensajeExito");
    int idCita = request.getAttribute("idCita") != null ? (Integer) request.getAttribute("idCita") : 1;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Atención de Paciente Actual - Portal del Médico | Clínica Internacional</title>
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
            --radius-lg: 16px;
            --radius-md: 12px;
            --sidebar-width: 260px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background-color: var(--bg-page); color: var(--text-dark); display: flex; height: 100vh; overflow: hidden; }

        .sidebar {
            width: var(--sidebar-width); background-color: var(--bg-card); border-right: 1px solid var(--border-light);
            display: flex; flex-direction: column; padding: 24px 20px; flex-shrink: 0; z-index: 10;
        }
        .brand-container { display: flex; align-items: center; gap: 12px; margin-bottom: 40px; padding-left: 8px; }
        .brand-container img { height: 36px; }
        .nav-menu { display: flex; flex-direction: column; gap: 8px; flex: 1; }
        .nav-item {
            display: flex; align-items: center; gap: 14px; padding: 12px 16px; border-radius: var(--radius-md);
            color: var(--text-gray); text-decoration: none; font-size: 14px; font-weight: 500; transition: all 0.2s;
        }
        .nav-item:hover { background-color: #f8fafc; color: var(--text-dark); }
        .nav-item.active { background-color: #eff6ff; color: var(--primary); font-weight: 600; }
        .user-profile-bottom {
            margin-top: auto; display: flex; align-items: center; gap: 12px; padding: 16px 8px 8px 8px;
            border-top: 1px solid var(--border-light); cursor: pointer; text-decoration: none;
        }
        .avatar-initials {
            display: flex; align-items: center; justify-content: center; border-radius: 50%; font-weight: 600;
            color: var(--primary); background-color: #eff6ff; width: 36px; height: 36px; font-size: 13px; flex-shrink: 0;
        }
        .user-name-sm { font-size: 13px; font-weight: 600; color: var(--text-dark); line-height: 1.3; }
        .user-role-sm { font-size: 11px; color: var(--text-gray); }

        .main-content { flex: 1; padding: 32px 40px; overflow-y: auto; }
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .header-title { font-size: 26px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }

        .alert-success {
            background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        /* CARD CONTAINERS */
        .card-box {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 28px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .card-box-title { font-size: 16px; font-weight: 700; color: var(--text-dark); margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }

        /* GRID PATIENT & APPOINTMENT INFO */
        .patient-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
        .info-group { margin-bottom: 14px; }
        .info-label { font-size: 12px; font-weight: 600; color: var(--text-gray); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
        .info-val {
            font-size: 14px; font-weight: 600; color: var(--text-dark); padding: 10px 14px; background: #f8fafc;
            border-radius: var(--radius-md); border: 1px solid var(--border-light);
        }

        /* ATTENDANCE BUTTONS */
        .attendance-group { display: flex; gap: 16px; margin-bottom: 16px; }
        .btn-att-pill {
            flex: 1; padding: 14px; border-radius: 30px; border: 1px solid var(--border-light); background: #f8fafc;
            font-size: 14px; font-weight: 600; color: var(--text-gray); cursor: pointer; transition: all 0.2s; text-align: center;
        }
        .btn-att-pill.selected { background: var(--primary); color: #ffffff; border-color: var(--primary); box-shadow: 0 4px 12px rgba(37,99,235,0.2); }

        .attendance-confirmed-badge {
            background: #dcfce7; color: #166534; padding: 12px 18px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600; text-align: center;
        }

        /* PRIORITY CARDS */
        .priority-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
        .priority-card {
            border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; cursor: pointer;
            background: #ffffff; transition: all 0.2s;
        }
        .priority-card:hover { border-color: var(--primary); }
        .priority-card.selected { background: var(--primary-light); border-color: var(--primary); }
        .priority-card-title { font-size: 14px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; }
        .priority-card.selected .priority-card-title { color: var(--primary); }
        .priority-card-sub { font-size: 12px; color: var(--text-gray); }

        textarea {
            width: 100%; height: 90px; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; outline: none; transition: all 0.2s; background: #ffffff; font-family: 'Inter', sans-serif;
        }
        textarea:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }

        .btn-save-record {
            background: var(--primary); color: #ffffff; border: none; padding: 14px 32px; border-radius: 30px;
            font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.2);
            display: inline-flex; align-items: center; gap: 8px;
        }
        .btn-save-record:hover { background: var(--primary-hover); transform: translateY(-1px); }

        @media(max-width:900px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .patient-grid, .priority-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Mi Agenda
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="nav-item active">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Atención Paciente
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= inicialesMedico %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombreMedico %></div>
                <div class="user-role-sm">Médico Especialista</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Atención de Pacientes — Cita #CIT-2026-9482</h1>
                <p class="header-subtitle">Registro asistencial y evolución clínica de la consulta ambulatoria activa.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <% if (mensajeExito != null) { %>
            <div class="alert-success">
                <i data-lucide="check-circle" style="width: 20px;"></i> <%= mensajeExito %>
            </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/medico/atencion" method="POST">
            <input type="hidden" name="idCita" value="<%= idCita %>">

            <!-- DATOS DE LA CITA MÉDICA -->
            <div class="card-box">
                <div class="card-box-title">
                    <i data-lucide="clipboard-list" style="width: 20px; color: var(--primary);"></i> Sección 1: Datos de la Cita Médica
                </div>

                <div class="patient-grid">
                    <div>
                        <div class="info-group">
                            <div class="info-label">Paciente / Historia Clínica</div>
                            <div class="info-val">Juan Alberto Pérez Gómez (HC #89281)</div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">Documento y Datos</div>
                            <div class="info-val">DNI 72345678 &nbsp;•&nbsp; 54 Años &nbsp;•&nbsp; Masculino</div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">Cobertura</div>
                            <div class="info-val">Plan EPS Regular — Aseguradora Convenio</div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">Alergias Registradas</div>
                            <div class="info-val" style="color: #166534;">Ninguna conocida</div>
                        </div>
                    </div>

                    <div>
                        <div class="info-group">
                            <div class="info-label">Fecha y Hora Programada</div>
                            <div class="info-val">Jueves 24 de Octubre de 2026 — 09:30 AM</div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">Sede y Consultorio</div>
                            <div class="info-val">Sede Central — Consultorio 302 (Pabellón B)</div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">Médico Tratante</div>
                            <div class="info-val"><%= nombreMedico %> (Cardiología)</div>
                        </div>

                        <div class="info-group">
                            <div class="info-label">Motivo de Consulta</div>
                            <div class="info-val">Chequeo preventivo / Molestia torácica leve al esfuerzo</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- CONTROL DE ASISTENCIA -->
            <div class="card-box">
                <div class="card-box-title">
                    <i data-lucide="user-check" style="width: 20px; color: var(--primary);"></i> Sección 2: Control de Asistencia del Paciente
                </div>

                <div class="attendance-group">
                    <button type="button" class="btn-att-pill selected" id="btnAsistio" onclick="selectAttendance('ASISTIO')">
                        ✓ Marcar Asistió
                    </button>
                    <button type="button" class="btn-att-pill" id="btnNoAsistio" onclick="selectAttendance('NO_ASISTIO')">
                        ✗ Marcar No Asistió
                    </button>
                </div>
                <input type="hidden" name="asistencia" id="asistenciaInput" value="ASISTIO">

                <div class="attendance-confirmed-badge">
                    <i data-lucide="check" style="width: 16px; display: inline; vertical-align: middle; margin-right: 6px;"></i>
                    Asistencia confirmada por Recepción (En consultorio)
                </div>
            </div>

            <!-- REGISTRO DE PRIORIDAD CLÍNICA -->
            <div class="card-box">
                <div class="card-box-title">
                    <i data-lucide="activity" style="width: 20px; color: var(--primary);"></i> Sección 3: Registro de Prioridad Clínica y Próxima Atención
                </div>

                <div style="font-size: 13px; font-weight: 600; color: var(--text-dark); margin-bottom: 12px;">
                    Nivel de Prioridad Clínica para Citas Posteriores:
                </div>

                <div class="priority-grid">
                    <div class="priority-card" onclick="selectPriority(this, 'ALTA')">
                        <div class="priority-card-title">Prioridad Alta</div>
                        <div class="priority-card-sub">Seguimiento urgente o derivación prioritaria</div>
                    </div>

                    <div class="priority-card selected" onclick="selectPriority(this, 'MEDIA')">
                        <div class="priority-card-title">Prioridad Media</div>
                        <div class="priority-card-sub">Control estándar según evolución clínica</div>
                    </div>

                    <div class="priority-card" onclick="selectPriority(this, 'BAJA')">
                        <div class="priority-card-title">Prioridad Baja</div>
                        <div class="priority-card-sub">Control de rutina o preventivo anual</div>
                    </div>
                </div>
                <input type="hidden" name="prioridad" id="prioridadInput" value="MEDIA">

                <div style="margin-bottom: 20px;">
                    <label style="font-size: 13px; font-weight: 600; color: var(--text-dark); display: block; margin-bottom: 6px;">
                        Fecha Recomendada de Próxima Atención
                    </label>
                    <input type="date" value="2026-11-04" style="padding: 10px 14px; border-radius: var(--radius-md); border: 1px solid var(--border-light); font-size: 14px;">
                </div>

                <div style="margin-bottom: 24px;">
                    <label style="font-size: 13px; font-weight: 600; color: var(--text-dark); display: block; margin-bottom: 6px;">
                        Indicaciones de Continuidad (Observaciones Clínicas)
                    </label>
                    <textarea name="indicaciones" placeholder="Ingrese las observaciones clínicas y plan de continuidad...">Control con resultados de ecocardiograma y perfil lipídico completo</textarea>
                </div>

                <div style="text-align: right;">
                    <button type="submit" class="btn-save-record">
                        <i data-lucide="check" style="width: 18px;"></i> Guardar Registro de Atención
                    </button>
                </div>
            </div>
        </form>
    </main>

    <script>
        lucide.createIcons();

        function selectAttendance(val) {
            document.getElementById('asistenciaInput').value = val;
            if (val === 'ASISTIO') {
                document.getElementById('btnAsistio').classList.add('selected');
                document.getElementById('btnNoAsistio').classList.remove('selected');
            } else {
                document.getElementById('btnAsistio').classList.remove('selected');
                document.getElementById('btnNoAsistio').classList.add('selected');
            }
        }

        function selectPriority(card, val) {
            document.querySelectorAll('.priority-card').forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
            document.getElementById('prioridadInput').value = val;
        }
    </script>
</body>
</html>
