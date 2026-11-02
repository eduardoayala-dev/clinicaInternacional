<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%@ page import="pe.edu.utp.clinica.dao.ReporteAdmisionDAO" %>
<%@ page import="java.util.Locale" %>
<%
    Usuario usuario=(Usuario)session.getAttribute("usuarioLogueado");
    String nombrePersonal=usuario==null?"Personal de admisión":usuario.getNombres()+" "+usuario.getApellidos();
    ReporteAdmisionDAO.Reporte reporte=(ReporteAdmisionDAO.Reporte)request.getAttribute("reporte");
    var resumen=reporte.resumen();var filtro=reporte.filtro();
    String parametros="desde="+filtro.desde()+"&hasta="+filtro.hasta()+"&estado="+filtro.estado();
    String ruta=request.getContextPath()+"/admin/dashboard-operativo?"+parametros;
    String exportar=request.getContextPath()+"/admin/export?"+parametros;
%>
<%!
    private static String porcentaje(long valor,long total){return total==0?"Sin datos":String.format(Locale.ROOT,"%.1f%%",valor*100.0/total);}
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Operativo - Clínica Internacional</title>
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

        .app-body { display: flex; height: calc(100vh - 64px); overflow: hidden; }

        .sidebar {
            width: 280px; background-color: var(--bg-surface); border-right: 1px solid var(--border-light);
            padding: 24px 16px; display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; overflow-y: auto;
        }
        .nav-item {
            padding: 12px 16px; display: flex; align-items: center; gap: 12px;
            color: var(--text-muted); text-decoration: none; font-size: 14px; font-weight: 500;
            border-radius: var(--radius-md); transition: all 0.2s ease;
        }
        .nav-item:hover { background-color: var(--bg-body); color: var(--text-main); }
        .nav-item.active { background-color: var(--primary-light); color: var(--primary); font-weight: 600; }

        .main-content { flex: 1; height: 100%; overflow-y: auto; padding: 32px 40px; }
        .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .page-title { font-size: 24px; font-weight: 700; color: var(--text-main); margin-bottom: 6px; letter-spacing: -0.5px; }
        .page-subtitle { font-size: 14px; color: var(--text-muted); }

        .header-filters { display: flex; align-items: center; gap: 12px; }
        .select-filter {
            padding: 8px 12px; border: 1px solid var(--border-light); border-radius: var(--radius-md);
            font-size: 13px; color: var(--text-main); background: #ffffff; outline: none; cursor: pointer;
        }

        .btn-refresh {
            padding: 8px 16px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
            background: var(--primary); color: #ffffff; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; text-decoration: none;
        }
        .btn-refresh:hover { background: #1d4ed8; }

        .section-heading {
            font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;
            letter-spacing: 0.5px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;
        }

        .metrics-grid-5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; margin-bottom: 28px; }
        .metric-card {
            background: var(--bg-surface); padding: 18px; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            box-shadow: var(--shadow-sm); display: flex; flex-direction: column; justify-content: space-between;
        }
        .metric-title { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
        .metric-value { font-size: 24px; font-weight: 700; color: var(--text-main); margin: 8px 0 4px 0; }
        .metric-sub { font-size: 11px; color: var(--text-muted); line-height: 1.4; }
        .metric-tag { font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px; display: inline-block; margin-top: 6px; }
        .tag-blue { background: #eff6ff; color: #2563eb; }
        .tag-green { background: #dcfce7; color: #166534; }
        .tag-amber { background: #fef3c7; color: #92400e; }

        .risk-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 28px; }
        .risk-card {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px; box-shadow: var(--shadow-sm); position: relative; overflow: hidden;
        }
        .risk-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px; }
        .risk-card.low::before { background: #22c55e; }
        .risk-card.med::before { background: #f59e0b; }
        .risk-card.high::before { background: #ef4444; }

        .risk-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        .risk-title { font-size: 14px; font-weight: 700; }
        .risk-badge { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 12px; }
        .badge-low { background: #dcfce7; color: #166534; }
        .badge-med { background: #fef3c7; color: #92400e; }
        .badge-high { background: #fee2e2; color: #991b1b; }

        .risk-count { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
        .risk-desc { font-size: 12px; color: var(--text-muted); line-height: 1.4; margin-bottom: 12px; }
        .risk-action { font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--primary); }

        .panel { background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 24px; box-shadow: var(--shadow-sm); margin-bottom: 24px; }
        
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { padding: 12px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; border-bottom: 1px solid var(--border-light); }
        td { padding: 14px; font-size: 13px; color: var(--text-main); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-risk-pill { padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; }
        .badge-confirm-pill { padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; }
        .confirm-no { background: #f1f5f9; color: #475569; }
        .confirm-msg { background: #eff6ff; color: #2563eb; }
        .confirm-bounce { background: #fee2e2; color: #991b1b; }
        .confirm-ok { background: #dcfce7; color: #166534; }

        .reports-bar {
            background: var(--bg-surface); border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 20px 24px; box-shadow: var(--shadow-sm); display: flex; justify-content: space-between; align-items: center;
        }
        .btn-report {
            padding: 10px 18px; border-radius: var(--radius-md); font-size: 13px; font-weight: 600;
            border: 1px solid var(--border-light); background: #ffffff; color: var(--text-main); cursor: pointer;
            display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s; text-decoration: none;
        }
        .btn-report:hover { background: var(--bg-body); color: var(--primary); }

        .table-footer {
            display: flex; justify-content: space-between; align-items: center; margin-top: 16px; font-size: 13px; color: var(--text-muted);
        }
        .pagination-btns { display: flex; gap: 8px; }
        .btn-page { padding: 6px 12px; border-radius: var(--radius-md); border: 1px solid var(--border-light); background: #fff; font-size: 12px; cursor: pointer; }
        .btn-page:hover { background: var(--bg-body); }
    @media(max-width:1200px){.metrics-grid-5{grid-template-columns:repeat(2,minmax(0,1fr))}.page-header{flex-wrap:wrap;gap:16px}.header-filters{flex-wrap:wrap}} @media(max-width:700px){.metrics-grid-5,.risk-grid{grid-template-columns:1fr}.main-content{padding:16px}.sidebar{width:180px}.header-filters label{display:flex;flex-direction:column;gap:4px}}
</style>
</head>
<body>

    <header class="topbar">
        <div class="topbar-brand">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <div class="topbar-right">
            <div class="user-info">
                <span class="user-name"><%= Html.texto(nombrePersonal) %></span>
                <span class="user-role">PERSONAL ADMINISTRATIVO</span>
            </div>
            <div class="user-avatar"><i data-lucide="user" style="width: 18px; height: 18px;"></i></div>
            <div style="width: 1px; height: 24px; background: var(--border-light); margin: 0 4px;"></div>
            <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-logout" style="border:0;cursor:pointer;font-family:inherit;"><i data-lucide="log-out" style="width: 16px; height: 16px;"></i> Salir</button>
</form>
        </div>
    </header>

    <div class="app-body">
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/admin/medicos" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Médicos y Especialidades
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="nav-item">
                <i data-lucide="users" style="width: 18px;"></i> Pacientes
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="nav-item">
                <i data-lucide="refresh-cw" style="width: 18px;"></i> Reasignaciones
            </a>
            <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="nav-item active">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Dashboard Operativo
            </a>
            <a href="#" class="nav-item" style="margin-top: auto; border-top: 1px solid var(--border-light); border-radius: 0; padding-top: 16px;">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </aside>

        <main class="main-content" id="panelOperativo">
            <div class="page-header">
                <div><h1 class="page-title">Dashboard operativo</h1><p class="page-subtitle"><%= Html.texto(reporte.sede()) %> · Actualizado <%= Html.texto(reporte.actualizado().toString().replace('T',' ')) %> (Lima)</p></div>
                <form id="filtrosOperativo" class="header-filters" method="get" action="${pageContext.request.contextPath}/admin/dashboard-operativo">
                    <label for="desde">Desde <input class="select-filter" id="desde" name="desde" type="date" required value="<%= filtro.desde() %>"></label>
                    <label for="hasta">Hasta <input class="select-filter" id="hasta" name="hasta" type="date" required value="<%= filtro.hasta() %>"></label>
                    <label for="estado">Estado <select class="select-filter" id="estado" name="estado">
                        <option value="">Todos</option>
                        <% for(String estado:java.util.List.of("SOLICITADA","CONFIRMADA","EN_SALA","EN_ATENCION","ATENDIDA","NO_ASISTIO","NO_ATENDIDA","REPROGRAMADA","CANCELADA")){ %>
                        <option value="<%= estado %>" <%= estado.equals(filtro.estado())?"selected":"" %>><%= estado.replace('_',' ') %></option><% } %>
                    </select></label>
                    <button type="submit" class="btn-refresh">Consultar</button>
                    <button type="button" id="actualizarOperativo" class="btn-refresh">Actualizar ahora</button>
                </form>
            </div>
            <p class="page-subtitle" style="margin-bottom:20px">Período: <%= filtro.desde() %> al <%= filtro.hasta() %>. Hasta 31 días por consulta. Las cifras corresponden a las citas y al estado seleccionados.</p>
            <output id="estadoActualizacionOperativa" aria-live="polite" class="page-subtitle" style="margin-bottom:16px">Actualización automática cada 5 minutos mientras esta pestaña está visible.</output>
            <div class="metrics-grid-5">
                <div class="metric-card"><div class="metric-title">Citas del período</div><div class="metric-value"><%= resumen.total() %></div><div class="metric-sub"><%= resumen.atendidas() %> atendidas · <%= resumen.enAtencion() %> en atención<br><%= resumen.pendientes() %> activas pendientes de cierre</div></div>
                <div class="metric-card"><div class="metric-title">Médicos habilitados</div><div class="metric-value"><%= resumen.medicos() %></div><div class="metric-sub">Personal activo de esta sede actualmente<br>No representa ocupación de consultorios</div></div>
                <div class="metric-card"><div class="metric-title">Citas canceladas</div><div class="metric-value"><%= resumen.canceladas() %></div><div class="metric-sub"><%= porcentaje(resumen.canceladas(),resumen.total()) %> de las citas filtradas<br>Según el estado actual de cada cita</div></div>
                <div class="metric-card"><div class="metric-title">Ofertas aceptadas</div><div class="metric-value"><%= resumen.aceptadas() %></div><div class="metric-sub"><%= resumen.ofertas() %> ofertas sobre cupos del período<br>Tasa de aceptación: <%= porcentaje(resumen.aceptadas(),resumen.ofertas()) %></div></div>
                <div class="metric-card"><div class="metric-title">Espera media registrada</div><div class="metric-value"><%= resumen.esperaMedia()==null?"Sin datos":String.format(Locale.ROOT,"%.1f min",resumen.esperaMedia()) %></div><div class="metric-sub">Desde la llegada hasta iniciar la atención<br>Solo registros con ambas horas válidas</div></div>
            </div>
            <div class="section-heading"><span>Riesgo registrado en citas activas</span></div>
            <div class="risk-grid">
                <div class="risk-card low"><div class="risk-header"><span class="risk-title">Bajo</span><span class="risk-badge badge-low"><%= porcentaje(resumen.bajo(),resumen.pendientes()) %></span></div><div class="risk-count"><%= resumen.bajo() %> citas</div></div>
                <div class="risk-card med"><div class="risk-header"><span class="risk-title">Medio</span><span class="risk-badge badge-med"><%= porcentaje(resumen.medio(),resumen.pendientes()) %></span></div><div class="risk-count"><%= resumen.medio() %> citas</div></div>
                <div class="risk-card high"><div class="risk-header"><span class="risk-title">Alto</span><span class="risk-badge badge-high"><%= porcentaje(resumen.alto(),resumen.pendientes()) %></span></div><div class="risk-count"><%= resumen.alto() %> citas</div></div>
            </div>
            <p class="page-subtitle" style="margin:12px 0 20px">Sin clasificación: <%= resumen.sinRiesgo() %> citas activas. Se muestra el nivel guardado por el sistema; este tablero no calcula una nueva predicción.</p>
            <div class="panel">
                <div class="section-heading"><span>Citas del período seleccionado</span></div>
                <div style="overflow-x:auto"><table><thead><tr><th>Cita</th><th>Fecha y hora</th><th>Paciente</th><th>Documento</th><th>Médico</th><th>Especialidad</th><th>Estado</th><th>Asistencia</th></tr></thead><tbody>
                <% if(reporte.citas().isEmpty()){ %><tr><td colspan="8">No hay citas para estos filtros.</td></tr><% } %>
                <% for(var cita:reporte.citas()){ %><tr>
                    <td>#<%= cita.id() %></td><td><%= Html.texto(cita.fecha().toString().replace('T',' ')) %></td><td><%= Html.texto(cita.paciente()) %></td><td><%= Html.texto(cita.documento()) %></td><td><%= Html.texto(cita.medico()) %></td><td><%= Html.texto(cita.especialidad()) %></td><td><%= Html.texto(cita.estado().replace('_',' ')) %></td><td><%= Html.texto(cita.confirmacion()) %></td>
                </tr><% } %></tbody></table></div>
                <div class="table-footer" style="justify-content:flex-end;gap:12px">
                    <% if(reporte.pagina()>1){ %><a class="btn-page" href="<%= Html.texto(ruta+"&pagina="+(reporte.pagina()-1)) %>">Anterior</a><% } %>
                    <span>Página <%= reporte.pagina() %> de <%= reporte.paginas() %> · <%= resumen.total() %> citas</span>
                    <% if(reporte.pagina()<reporte.paginas()){ %><a class="btn-page" href="<%= Html.texto(ruta+"&pagina="+(reporte.pagina()+1)) %>">Siguiente</a><% } %>
                </div>
            </div>
            <div class="reports-bar" style="flex-wrap:wrap;gap:16px">
                <div><h2 style="font-size:14px">Descargar reportes</h2><p class="page-subtitle">Hasta 2000 citas por archivo. Se aplican este período, estado y sede.</p></div>
                <div style="display:flex;flex-wrap:wrap;gap:12px">
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=pdf") %>">Citas PDF</a>
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=xlsx") %>">Citas Excel</a>
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=csv") %>">Citas CSV</a>
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=pdf&type=indicadores") %>">Indicadores PDF</a>
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=xlsx&type=indicadores") %>">Indicadores Excel</a>
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=csv&type=indicadores") %>">Indicadores CSV</a>
                    <a class="btn-report" href="<%= Html.texto(exportar+"&format=csv&type=contacto") %>">CSV de contacto preventivo</a>
                </div>
            </div>
        </main></div>

    <script src="${pageContext.request.contextPath}/assets/js/dashboard-operativo.js"></script>
    <script>
        lucide.createIcons();
    </script>
</body>
</html>
