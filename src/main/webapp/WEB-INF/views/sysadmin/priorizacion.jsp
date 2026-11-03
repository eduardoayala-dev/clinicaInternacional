<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="pe.edu.utp.clinica.dao.ConfiguracionOfertasDAO,pe.edu.utp.clinica.util.Html" %>
<%
    var parametros=(ConfiguracionOfertasDAO.Parametros)request.getAttribute("configuracionOfertas");
    String flashError=(String)session.getAttribute("flashError");
    session.removeAttribute("flashError");
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreAdmin = (String) request.getAttribute("nombreAdmin");
    if (nombreAdmin == null && usuario != null) {
        nombreAdmin = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreAdmin == null) nombreAdmin = "Administrador";

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Configuración de Priorización - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #0284c7;
            --primary-hover: #0369a1;
            --primary-light: #e8f1fc;
            --bg-body: #f3f6fb;
            --bg-surface: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border-light: #edf2f7;
            --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.02);
            --radius-md: 10px;
            --radius-lg: 14px;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { display: flex; height: 100vh; width: 100vw; background-color: var(--bg-body); overflow: hidden; color: var(--text-main); }

        /* PANEL IZQUIERDO ESTÁNDAR */
        .sidebar {
            width: 250px; min-width: 250px; height: 100vh; background-color: #ffffff;
            border-right: 1px solid var(--border-light); display: flex; flex-direction: column;
            padding: 24px 16px; flex-shrink: 0; z-index: 20;
        }
        .sidebar-brand { display: flex; align-items: center; gap: 12px; padding: 4px 8px 24px 8px; text-decoration: none; }
        .brand-icon-wrapper { width: 38px; height: 38px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
        .brand-icon-img { width: 36px; height: 36px; object-fit: contain; }
        .brand-text-block { display: flex; flex-direction: column; }
        .brand-name { font-size: 14.5px; font-weight: 700; color: var(--text-main); line-height: 1.2; }
        .brand-subtext { font-size: 11.5px; font-weight: 500; color: var(--text-muted); margin-top: 2px; }

        .sidebar-nav { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; flex: 1; }
        .nav-link {
            display: flex; align-items: center; gap: 12px; padding: 11px 16px; border-radius: 12px;
            color: #475569; text-decoration: none; font-size: 13.5px; font-weight: 500; transition: all 0.15s ease;
        }
        .nav-link i { width: 19px; height: 19px; color: #94a3b8; stroke-width: 2; transition: color 0.15s ease; }
        .nav-link:hover { background-color: #f8fafc; color: var(--text-main); }
        .nav-link:hover i { color: #64748b; }
        .nav-link.active { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }
        .nav-link.active i { color: var(--primary); }

        .sidebar-footer { margin-top: auto; padding-top: 16px; }
        .user-capsule { background-color: #edf2f7; border-radius: 14px; padding: 10px 14px; display: flex; align-items: center; gap: 12px; }
        .user-avatar-circle { width: 36px; height: 36px; border-radius: 50%; background-color: var(--primary); color: #ffffff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; flex-shrink: 0; }
        .user-info-text { display: flex; flex-direction: column; flex: 1; min-width: 0; }
        .user-full-name { font-size: 13px; font-weight: 700; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.2; }
        .user-role-desc { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
        .btn-sidebar-logout { background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 4px; border-radius: 6px; display: flex; align-items: center; justify-content: center; text-decoration: none; }
        .btn-sidebar-logout:hover { color: #dc2626; background-color: #fee2e2; }
        .btn-sidebar-logout i { width: 18px; height: 18px; }

        .main-content { flex: 1; height: 100vh; overflow-y: auto; padding: 28px 48px 40px 48px; }
        .breadcrumb-trail { font-size: 12.5px; color: var(--text-muted); margin-bottom: 20px; font-weight: 500; }
        .page-header { margin-bottom: 24px; }
        .page-title { font-size: 28px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
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
    .catalogo-acceso{display:inline-flex;float:right;padding:10px 14px;background:#fff;border:1px solid #cbd5e1;border-radius:8px;color:#1668b8;text-decoration:none;font-size:13px;font-weight:600}.catalogo-acceso:hover{background:#eff6ff}.catalogo-acceso:focus-visible{outline:3px solid #1668b8;outline-offset:3px}@media(max-width:700px){.catalogo-acceso{float:none;margin-bottom:16px}}</style>
</head>
<body>

    <!-- PANEL DE LA IZQUIERDA ESTÁNDAR -->
    <aside class="sidebar">
        <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="sidebar-brand">
            <div class="brand-icon-wrapper">
                <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" 
                     alt="Logo" class="brand-icon-img"
                     onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            </div>
            <div class="brand-text-block">
                <span class="brand-name">Clínica Internacional</span>
                <span class="brand-subtext">Portal del administrador</span>
            </div>
        </a>

        <nav class="sidebar-nav">
            <a href="${pageContext.request.contextPath}/sysadmin/dashboard" class="nav-link">
                <i data-lucide="home"></i>
                <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/usuarios" class="nav-link">
                <i data-lucide="shield"></i>
                <span>Usuarios y roles</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/sedes" class="nav-link">
                <i data-lucide="building"></i>
                <span>Sedes</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/priorizacion" class="nav-link active">
                <i data-lucide="sliders"></i>
                <span>Configuración de priorización</span>
            </a>
            <a href="${pageContext.request.contextPath}/sysadmin/analitica" class="nav-link">
                <i data-lucide="trending-up"></i>
                <span>Indicadores y analítica</span>
            </a>
        </nav>

        <div class="sidebar-footer">
            <div class="user-capsule">
                <div class="user-avatar-circle"><%= nombreAdmin != null && !nombreAdmin.isEmpty() ? ("" + nombreAdmin.charAt(0)).toUpperCase() : "JV" %></div>
                <div class="user-info-text">
                    <span class="user-full-name"><%= Html.texto(nombreAdmin) %></span>
                    <span class="user-role-desc">Administrador</span>
                </div>
                <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-sidebar-logout" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                    <i data-lucide="log-out"></i>
                </button>
</form>
            </div>
        </div>
    </aside>

    <main class="main-content">
        <div class="breadcrumb-trail">Administrador / <span>Configuración de priorización</span></div>
        <div class="page-header"><a class="catalogo-acceso" href="${pageContext.request.contextPath}/sysadmin/motivos">Gestionar motivos de consulta →</a>
            <h1 class="page-title">Configuración de Priorización y Reasignación</h1>
            <a href="${pageContext.request.contextPath}/sysadmin/ia/inasistencia">Abrir simulador de IA de inasistencias (experimental)</a>
            <p class="page-subtitle">Parámetros que rigen la cola algorítmica y los tiempos de oferta automática para cupos recuperados.</p>
        </div>

            <% if (flashMessage != null) { %>
                <div class="flash-alert">
                    <i data-lucide="check-circle" style="width: 18px;"></i>
                    <%= Html.texto(flashMessage) %>
                </div>
            <% } %>

            <% if(flashError!=null){ %><div class="flash-alert" style="background:#fef2f2;color:#b91c1c;border-color:#fecaca"><%= Html.texto(flashError) %></div><% } %>
            <form action="${pageContext.request.contextPath}/sysadmin/configuracion-ofertas" method="POST">
                <input type="hidden" name="version" value="<%= parametros.version() %>">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="guardarPriorizacion">

                <!-- SECCIÓN 1 -->
                <div class="panel">
                    <div class="panel-title">
                        <span>Criterios Principales de Asignación Algorítmica</span>
                        <span class="panel-tag">REGLAS DEL MOTOR ACTUAL</span>
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
                                <div class="step-name">Preferencias del paciente</div>
                                <div class="step-desc">Se respeta el médico preferido, el turno y la anticipación indicada por el paciente.</div>
                            </div>
                            <span class="step-tag">PREFERENCIAS</span>
                        </div>
                    </div>
                </div>

                <div class="panel"><h2 class="panel-title">Una oferta exclusiva por cupo</h2><p>El siguiente candidato recibe la oferta cuando el anterior rechaza o deja vencer su plazo. El sistema conserva la prioridad clínica y la antigüedad en la lista.</p></div>

                <!-- SECCIÓN 3 -->
                <div class="panel">
                    <div class="panel-title">
                        <span>Parámetros Operativos de Oferta Temporal</span>
                        <span class="panel-tag">TIEMPOS DE RESPUESTA</span>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="tiempoLimite">Tiempo límite de respuesta a la oferta (minutos): *</label>
                            <input type="number" id="tiempoLimite" name="tiempoLimite" value="<%= parametros.minutosRespuesta() %>" min="5" max="60" required>
                            <span style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Ventana de bloqueo temporal del cupo antes de notificar al siguiente candidato.</span>
                        </div>

                        <div class="form-group">
                            <label for="anticipacionMinima">Anticipación mínima global (minutos): *</label>
                            <input type="number" id="anticipacionMinima" name="anticipacionMinima" value="<%= parametros.anticipacionMinima() %>" min="0" max="10080" required>
                            <span style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Se respeta también la anticipación elegida por cada paciente; se aplica la más exigente.</span>
                        </div>
                    </div>
                </div>

                <div class="panel">
                    <div class="panel-title"><span>Confirmación y llegada</span><span class="panel-tag">PARÁMETROS DE ATENCIÓN</span></div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="confirmacionHoras">Límite para confirmar (horas antes de la cita): *</label>
                            <input type="number" id="confirmacionHoras" name="confirmacionHoras" min="1" max="168" required value="<%= parametros.confirmacionHoras() %>">
                            <span style="font-size:12px;color:var(--text-muted)">Solo cambia las nuevas reservas y reprogramaciones. Si se reserva dentro de este margen, se puede confirmar hasta la hora de la cita.</span>
                        </div>
                        <div class="form-group">
                            <label for="toleranciaLlegada">Tolerancia de llegada (minutos): *</label>
                            <input type="number" id="toleranciaLlegada" name="toleranciaLlegada" min="0" max="120" required value="<%= parametros.toleranciaLlegada() %>">
                            <span style="font-size:12px;color:var(--text-muted)">Se aplica al registrar llegadas y comprobar ausencias. No modifica las llegadas ya registradas.</span>
                        </div>
                    </div>
                </div>
                    <div class="panel">
                        <div class="panel-title"><span>Recordatorios y contactos</span><span class="panel-tag">SEGUIMIENTO DE ASISTENCIA</span></div>
                        <div class="form-row">
                            <div class="form-group">
                                <label for="recordatorioHoras">Recordar la cita (horas antes): *</label>
                                <input type="number" id="recordatorioHoras" name="recordatorioHoras" min="1" max="168" required value="<%= parametros.recordatorioHoras() %>">
                                <span style="font-size:12px;color:var(--text-muted)">Se consulta en cada ciclo si el envío automático está habilitado. No repite recordatorios ya procesados.</span>
                            </div>
                            <div class="form-group">
                                <label for="intentosConfirmacion">Contactos mínimos para cancelar por falta de confirmación: *</label>
                                <input type="number" id="intentosConfirmacion" name="intentosConfirmacion" min="1" max="10" required value="<%= parametros.intentosConfirmacion() %>">
                                <span style="font-size:12px;color:var(--text-muted)">Solo contactos realizados por admisión. Además debe vencer el plazo; la cancelación siempre es manual.</span>
                            </div>
                        </div>
                    </div>
                <div class="actions-row">
                    <button type="reset" class="btn-reset">Descartar cambios</button>
                    <button type="submit" class="btn-save"><i data-lucide="save" style="width: 16px; vertical-align: middle; margin-right: 6px;"></i> Guardar Configuración</button>
                </div>
            </form>
        </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
