<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Ing. Roberto Vargas";

    String iniciales = "RV";
    if (nombreAdmin != null && !nombreAdmin.trim().isEmpty()) {
        String[] partes = nombreAdmin.trim().split("\\s+");
        if (partes.length >= 2) {
            iniciales = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            iniciales = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Configuración de Priorización - Clínica Internacional</title>
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

        .flash-alert {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        .note-card {
            background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 16px 20px;
            border-radius: var(--radius-md); font-size: 13px; margin-bottom: 24px; line-height: 1.5; font-weight: 500;
        }

        .section-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .section-title { font-size: 16px; font-weight: 700; color: var(--text-dark); margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; }
        .section-tag { font-size: 11px; background: var(--primary-light); color: var(--primary); padding: 4px 10px; border-radius: 20px; font-weight: 600; }

        .step-list { display: flex; flex-direction: column; gap: 16px; position: relative; }
        .step-item {
            background: #f8fafc; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            padding: 16px; display: flex; align-items: center; gap: 16px;
        }
        .step-badge {
            width: 32px; height: 32px; border-radius: 50%; background: var(--primary); color: #ffffff;
            font-weight: 700; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0;
        }
        .step-content { flex: 1; }
        .step-name { font-size: 14px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; }
        .step-desc { font-size: 13px; color: var(--text-gray); }
        .step-tag { font-size: 11px; background: #e2e8f0; color: #475569; padding: 2px 8px; border-radius: 6px; font-weight: 600; }

        .grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .complementary-card {
            background: #f8fafc; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 18px;
        }
        .comp-title { font-size: 14px; font-weight: 700; color: var(--text-dark); margin-bottom: 6px; display: flex; justify-content: space-between; }

        .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 10px; }
        .form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-dark); margin-bottom: 6px; }
        .form-group input {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-dark); outline: none; background: #ffffff;
        }
        .form-group input:focus { border-color: var(--primary); }

        .actions-row { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
        .btn-reset { padding: 12px 24px; border-radius: 30px; border: 1px solid var(--border-light); background: #ffffff; cursor: pointer; font-weight: 600; }
        .btn-save {
            padding: 12px 28px; border-radius: 30px; border: none; background: var(--primary); color: #ffffff;
            cursor: pointer; font-weight: 600; box-shadow: 0 4px 12px rgba(37,99,235,0.2); transition: all 0.2s;
        }
        .btn-save:hover { background: var(--primary-hover); }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .grid-2col, .form-row { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> 1. Inicio
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> 2. Usuarios y Roles
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i> 3. Sedes
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item active">
                <i data-lucide="sliders" style="width: 18px;"></i> 4. Config. Priorización
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="bar-chart-3" style="width: 18px;"></i> 5. Indicadores y Analítica
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= iniciales %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombreAdmin %></div>
                <div class="user-role-sm">Administrador del Sistema</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Configuración de Criterios de Priorización y Reasignación</h1>
                <p class="header-subtitle">Parámetros que rigen la cola algorítmica y los tiempos de oferta automática para cupos recuperados.</p>
            </div>
            <a href="#" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <% if (flashMessage != null) { %>
            <div class="flash-alert">
                <i data-lucide="check-circle" style="width: 18px;"></i>
                <%= flashMessage %>
            </div>
        <% } %>

        <div class="note-card">
            <i data-lucide="info" style="width: 16px; vertical-align: middle; margin-right: 6px;"></i>
            <strong>NOTA VINCULANTE:</strong> Los criterios se evalúan en estricto orden secuencial. Sin pesos porcentuales ni fórmulas matemáticas. Si un paciente no cumple el criterio superior, queda descartado antes de evaluar el siguiente nivel.
        </div>

        <form action="${pageContext.request.contextPath}/sysadmin/priorizacion" method="POST">
            <input type="hidden" name="action" value="guardarPriorizacion">

            <!-- SECCIÓN 1 -->
            <div class="section-card">
                <div class="section-title">
                    <span>SECCIÓN 1: CRITERIOS PRINCIPALES DE ASIGNACIÓN</span>
                    <span class="section-tag">ORDEN DE PRELACIÓN OBLIGATORIO 1 A 5</span>
                </div>
                <div class="step-list">
                    <div class="step-item">
                        <div class="step-badge">01</div>
                        <div class="step-content">
                            <div class="step-name">1. ESPECIALIDAD REQUERIDA</div>
                            <div class="step-desc">Coincidencia exacta con la especialidad médica del cupo liberado (Obligatorio). Si no hay match total de cartera de servicios, el algoritmo no avanza en la evaluación.</div>
                        </div>
                        <span class="step-tag">CONDICIÓN SINE QUA NON</span>
                    </div>

                    <div class="step-item">
                        <div class="step-badge">02</div>
                        <div class="step-content">
                            <div class="step-name">2. SEDE CLÍNICA</div>
                            <div class="step-desc">Coincidencia con la sede clínica declarada por el paciente (Obligatorio). Exclusión automática si el paciente ha rechazado explícitamente la movilidad entre campus asistenciales.</div>
                        </div>
                        <span class="step-tag">COINCIDENCIA TERRITORIAL</span>
                    </div>

                    <div class="step-item">
                        <div class="step-badge">03</div>
                        <div class="step-content">
                            <div class="step-name">3. PRIORIDAD CLÍNICA</div>
                            <div class="step-desc">Nivel de urgencia registrado por triaje o médico tratante (Alta > Media > Baja). Selección por bloques categóricos; candidatos con urgencia 'Alta' agotan la cola antes de habilitar 'Media'.</div>
                        </div>
                        <span class="step-tag">CRITERIO TRIAJE</span>
                    </div>

                    <div class="step-item">
                        <div class="step-badge">04</div>
                        <div class="step-content">
                            <div class="step-name">4. TIEMPO DE ESPERA</div>
                            <div class="step-desc">Antigüedad cronológica (FIFO) de inscripción en lista de espera. Dentro del mismo nivel clínico, los registros con mayor fecha de antigüedad poseen prelación absoluta.</div>
                        </div>
                        <span class="step-tag">COLA FIFO</span>
                    </div>

                    <div class="step-item">
                        <div class="step-badge">05</div>
                        <div class="step-content">
                            <div class="step-name">5. CERCANÍA DEL PACIENTE</div>
                            <div class="step-desc">Proximidad de la zona de residencia del paciente a la sede del cupo. Determinación por anillo perimetral distrital para maximizar tasa de comparecencia efectiva.</div>
                        </div>
                        <span class="step-tag">GEODISPONIBILIDAD</span>
                    </div>
                </div>
            </div>

            <!-- SECCIÓN 2 -->
            <div class="section-card">
                <div class="section-title">
                    <span>SECCIÓN 2: CRITERIOS COMPLEMENTARIOS (APLICABLES EXCLUSIVAMENTE PARA DESEMPATE)</span>
                    <span class="section-tag">SUBSISTEMA CONDICIONAL</span>
                </div>
                <div class="grid-2col">
                    <div class="complementary-card">
                        <div class="comp-title">
                            <span>CRITERIO A: DISPONIBILIDAD DEL PACIENTE</span>
                            <span class="step-tag">DISPONIBILIDAD</span>
                        </div>
                        <p style="font-size: 13px; color: var(--text-gray); margin-bottom: 12px;">Preferencia horaria y tolerancia de aviso anticipado informada por el paciente. Solo se computa si dos o más postulantes igualan todas las condiciones secuenciales de la Sección 1.</p>
                        <span style="font-size: 11px; color: #166534; font-weight: 700;">ESTADO: ACTIVO EN DESEMPATE</span>
                    </div>

                    <div class="complementary-card">
                        <div class="comp-title">
                            <span>CRITERIO B: HISTORIAL DE ASISTENCIA</span>
                            <span class="step-tag">CUMPLIMIENTO</span>
                        </div>
                        <p style="font-size: 13px; color: var(--text-gray); margin-bottom: 12px;">Registro de cumplimiento y baja tasa de inasistencia previa. Prioriza al candidato con menor índice de cancelaciones tardías o ausencias no notificadas.</p>
                        <span style="font-size: 11px; color: #166534; font-weight: 700;">ESTADO: ACTIVO EN DESEMPATE</span>
                    </div>
                </div>
            </div>

            <!-- SECCIÓN 3 -->
            <div class="section-card">
                <div class="section-title">
                    <span>SECCIÓN 3: PARÁMETROS OPERATIVOS DE OFERTA TEMPORAL</span>
                    <span class="section-tag">VARIABLES DE EJECUCIÓN</span>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Tiempo límite de respuesta a la oferta temporal (minutos): *</label>
                        <input type="number" name="tiempoLimite" value="15" min="5" max="60" required>
                        <span style="font-size: 12px; color: var(--text-gray); margin-top: 4px;">Ventana de bloqueo temporal del cupo antes de liberar la oferta al siguiente candidato en cola.</span>
                    </div>

                    <div class="form-group">
                        <label>Número de candidatos notificados en simultáneo: *</label>
                        <input type="number" name="numCandidatos" value="1" min="1" max="5" required>
                        <span style="font-size: 12px; color: var(--text-gray); margin-top: 4px;">Cantidad de usuarios calificados que reciben la notificación push/SMS simultánea para asegurar cobertura.</span>
                    </div>
                </div>
            </div>

            <div class="actions-row">
                <button type="button" class="btn-reset">Cancelar / Restablecer</button>
                <button type="submit" class="btn-save"><i data-lucide="save" style="width: 16px; vertical-align: middle; margin-right: 6px;"></i> Guardar Configuración</button>
            </div>
        </form>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
