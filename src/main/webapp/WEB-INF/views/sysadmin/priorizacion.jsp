<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Roberto Vargas";

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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-light: #eff6ff;
            --bg-body: #f8fafc;
            --bg-surface: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-light: #e2e8f0;
            --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
            --radius-md: 8px;
            --radius-lg: 12px;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { display: flex; flex-direction: column; height: 100vh; background-color: var(--bg-body); overflow: hidden; color: var(--text-main); }

        /* HEADER SUPERIOR (TOPBAR) */
        .topbar {
            height: 64px; flex-shrink: 0; background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-light); display: flex;
            align-items: center; justify-content: space-between; padding: 0 32px; z-index: 10;
        }
        .topbar-brand { display: flex; align-items: center; gap: 12px; }
        .topbar-brand img { height: 36px; }
        .topbar-right { display: flex; align-items: center; gap: 16px; }
        .user-info { display: flex; flex-direction: column; align-items: flex-end; }
        .user-name { font-weight: 700; font-size: 13px; color: var(--text-main); }
        .user-role { font-size: 11px; color: var(--primary); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
        .user-avatar { width: 36px; height: 36px; border-radius: 50%; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; }
        .btn-logout { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-radius: var(--radius-md); font-size: 13px; font-weight: 500; color: var(--text-muted); text-decoration: none; transition: all 0.2s; }
        .btn-logout:hover { color: #ef4444; background: #fef2f2; }

        /* CONTENEDOR INFERIOR */
        .app-body { display: flex; height: calc(100vh - 64px); overflow: hidden; }

        /* SIDEBAR BLANCO */
        .sidebar {
            width: 260px; background-color: var(--bg-surface); border-right: 1px solid var(--border-light);
            padding: 24px 16px; display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; overflow-y: auto;
        }
        .nav-item {
            padding: 12px 16px; display: flex; align-items: center; gap: 12px;
            color: var(--text-muted); text-decoration: none; font-size: 14px; font-weight: 500;
            border-radius: var(--radius-md); transition: all 0.2s ease;
        }
        .nav-item:hover { background-color: var(--bg-body); color: var(--text-main); }
        .nav-item.active { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }

        /* ÁREA PRINCIPAL */
        .main-content { flex: 1; height: 100%; overflow-y: auto; padding: 32px 40px; }
        .page-header { margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .flash-alert {
            background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 14px 18px;
            border-radius: var(--radius-md); font-weight: 600; margin-bottom: 24px; font-size: 14px; display: flex; align-items: center; gap: 10px;
        }

        .panel {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm);
        }
        .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; }
        .panel-tag { font-size: 11px; background: var(--primary-light); color: var(--primary); padding: 4px 10px; border-radius: 20px; font-weight: 600; }

        .step-list { display: flex; flex-direction: column; gap: 16px; }
        .step-item {
            background: #f8fafc; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            padding: 16px; display: flex; align-items: center; gap: 16px;
        }
        .step-badge {
            width: 32px; height: 32px; border-radius: 50%; background: var(--primary); color: #ffffff;
            font-weight: 700; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0;
        }
        .step-content { flex: 1; }
        .step-name { font-size: 14px; font-weight: 700; color: var(--text-main); margin-bottom: 4px; }
        .step-desc { font-size: 13px; color: var(--text-muted); }
        .step-tag { font-size: 11px; background: #e2e8f0; color: #475569; padding: 2px 8px; border-radius: 6px; font-weight: 600; }

        .grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .complementary-card {
            background: #f8fafc; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 18px;
        }
        .comp-title { font-size: 14px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; display: flex; justify-content: space-between; }

        .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 10px; }
        .form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-main); margin-bottom: 6px; }
        .form-group input {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-main); outline: none; background: #ffffff;
        }
        .form-group input:focus { border-color: var(--primary); }

        .actions-row { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
        .btn-reset { padding: 12px 24px; border-radius: 20px; border: 1px solid var(--border-light); background: #ffffff; cursor: pointer; font-weight: 600; }
        .btn-save {
            padding: 12px 28px; border-radius: 20px; border: none; background: var(--primary); color: #ffffff;
            cursor: pointer; font-weight: 600; box-shadow: 0 4px 12px rgba(37,99,235,0.2); transition: all 0.2s;
        }
        .btn-save:hover { background: #1d4ed8; }

        @media(max-width:1024px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .grid-2col, .form-row { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><%= nombreAdmin %></span>
                <span class="user-role">ADMINISTRADOR DEL SISTEMA</span>
            </div>
            <div class="user-avatar"><i data-lucide="user" style="width: 18px; height: 18px;"></i></div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</a>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Usuarios y Roles
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-item">
                <i data-lucide="building-2" style="width: 18px;"></i> Gestión de Sedes
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-item active">
                <i data-lucide="sliders-horizontal" style="width: 18px;"></i> Config. Priorización
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-item">
                <i data-lucide="pie-chart" style="width: 18px;"></i> Métricas
            </a>
            <a href="#" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content">
            <div class="page-header">
                <h1 class="page-title">Configuración de Priorización y Reasignación</h1>
                <p class="page-subtitle">Parámetros que rigen la cola algorítmica y los tiempos de oferta automática para cupos recuperados.</p>
            </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px;"></i>
                    <%= flashMessage %>
                </div>
            <% } %>

            <form action="${pageContext.request.contextPath}/sysadmin/priorizacion" method="POST">
                <input type="hidden" name="action" value="guardarPriorizacion">

                <!-- SECCIÓN 1 -->
                <div class="panel">
                    <div class="panel-title">
                        <span>Criterios Principales de Asignación Algorítmica</span>
                        <span class="panel-tag">ORDEN DE PRELACIÓN OBLIGATORIO</span>
                    </div>
                    <div class="step-list">
                        <div class="step-item">
                            <div class="step-badge">1</div>
                            <div class="step-content">
                                <div class="step-name">Especialidad Requerida</div>
                                <div class="step-desc">Coincidencia exacta con la especialidad médica del cupo liberado. Requisito obligatorio para el emparejamiento.</div>
                            </div>
                            <span class="step-tag">OBLIGATORIO</span>
                        </div>

                        <div class="step-item">
                            <div class="step-badge">2</div>
                            <div class="step-content">
                                <div class="step-name">Sede Clínica</div>
                                <div class="step-desc">Coincidencia con la sede clínica declarada por el paciente o preferencia de campus.</div>
                            </div>
                            <span class="step-tag">TERRITORIAL</span>
                        </div>

                        <div class="step-item">
                            <div class="step-badge">3</div>
                            <div class="step-content">
                                <div class="step-name">Prioridad Clínica</div>
                                <div class="step-desc">Nivel de urgencia registrado por triaje o médico tratante (Alta > Media > Baja).</div>
                            </div>
                            <span class="step-tag">TRIAJE</span>
                        </div>

                        <div class="step-item">
                            <div class="step-badge">4</div>
                            <div class="step-content">
                                <div class="step-name">Tiempo de Espera en Lista</div>
                                <div class="step-desc">Antigüedad cronológica (FIFO) de inscripción en lista de espera.</div>
                            </div>
                            <span class="step-tag">COLA FIFO</span>
                        </div>

                        <div class="step-item">
                            <div class="step-badge">5</div>
                            <div class="step-content">
                                <div class="step-name">Proximidad Geográfica</div>
                                <div class="step-desc">Cercanía de la zona de residencia del paciente a la sede del cupo disponible.</div>
                            </div>
                            <span class="step-tag">UBICACIÓN</span>
                        </div>
                    </div>
                </div>

                <!-- SECCIÓN 2 -->
                <div class="panel">
                    <div class="panel-title">
                        <span>Criterios Complementarios de Desempate</span>
                        <span class="panel-tag">REGLAS CONDICIONALES</span>
                    </div>
                    <div class="grid-2col">
                        <div class="complementary-card">
                            <div class="comp-title">
                                <span>Disponibilidad Horaria</span>
                                <span class="step-tag">DISPONIBILIDAD</span>
                            </div>
                            <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;">Preferencia horaria e historial de respuesta rápida informada por el paciente en la aplicación.</p>
                            <span style="font-size: 11px; color: #166534; font-weight: 700;">ESTADO: ACTIVO</span>
                        </div>

                        <div class="complementary-card">
                            <div class="comp-title">
                                <span>Historial de Asistencia</span>
                                <span class="step-tag">CUMPLIMIENTO</span>
                            </div>
                            <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;">Registro previo de cumplimiento y baja tasa de cancelación tardía o ausentismo.</p>
                            <span style="font-size: 11px; color: #166534; font-weight: 700;">ESTADO: ACTIVO</span>
                        </div>
                    </div>
                </div>

                <!-- SECCIÓN 3 -->
                <div class="panel">
                    <div class="panel-title">
                        <span>Parámetros Operativos de Oferta Temporal</span>
                        <span class="panel-tag">TIEMPOS DE RESPUESTA</span>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label>Tiempo límite de respuesta a la oferta (minutos): *</label>
                            <input type="number" name="tiempoLimite" value="15" min="5" max="60" required>
                            <span style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Ventana de bloqueo temporal del cupo antes de notificar al siguiente candidato.</span>
                        </div>

                        <div class="form-group">
                            <label>Candidatos notificados en simultáneo: *</label>
                            <input type="number" name="numCandidatos" value="1" min="1" max="5" required>
                            <span style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Número de usuarios calificados que reciben la notificación de cupo disponible.</span>
                        </div>
                    </div>
                </div>

                <div class="actions-row">
                    <button type="button" class="btn-reset">Restablecer Valores</button>
                    <button type="submit" class="btn-save"><i data-lucide="save" style="width: 16px; vertical-align: middle; margin-right: 6px;"></i> Guardar Configuración</button>
                </div>
            </form>
        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
