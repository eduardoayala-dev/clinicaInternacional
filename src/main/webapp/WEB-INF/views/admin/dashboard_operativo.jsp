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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Playfair+Display:wght@600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css?v=20261102-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard-operativo.css?v=20261102-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admision-shell.css?v=20261104-pa"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-forms.css?v=20261104-2"></head>
<body class="portal-admision">

    <%@ include file="/WEB-INF/jspf/admin-sidebar.jspf" %>
<div class="adm-main-wrapper"><header class="adm-topbar"><div class="adm-breadcrumb">Admisión / <span>Dashboard operativo</span></div></header>
<main class="adm-content-scroll" id="panelOperativo" data-eventos-url="${pageContext.request.contextPath}/admin/eventos-confirmaciones">
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
            <output id="estadoActualizacionOperativa" aria-live="polite" class="page-subtitle" style="margin-bottom:16px">Actualización en vivo con respaldo cada 5 minutos mientras esta pestaña está visible.</output>
            <div class="metrics-grid-5">
                <div class="metric-card"><div class="metric-title">Citas del período</div><div class="metric-value"><%= resumen.total() %></div><div class="metric-sub"><%= resumen.atendidas() %> atendidas · <%= resumen.enAtencion() %> en atención<br><%= resumen.pendientes() %> activas pendientes de cierre</div></div>
                <div class="metric-card"><div class="metric-title">Médicos habilitados</div><div class="metric-value"><%= resumen.medicos() %></div><div class="metric-sub">Personal activo de esta sede actualmente<br>No representa ocupación de consultorios</div></div>
                <div class="metric-card"><div class="metric-title">Citas canceladas</div><div class="metric-value"><%= resumen.canceladas() %></div><div class="metric-sub"><%= porcentaje(resumen.canceladas(),resumen.total()) %> de las citas filtradas<br>Según el estado actual de cada cita</div></div>
                <div class="metric-card"><div class="metric-title">Ofertas aceptadas</div><div class="metric-value"><%= resumen.aceptadas() %></div><div class="metric-sub"><%= resumen.ofertas() %> ofertas sobre cupos del período<br>Tasa de aceptación: <%= porcentaje(resumen.aceptadas(),resumen.ofertas()) %></div></div>
                <div class="metric-card"><div class="metric-title">Espera media registrada</div><div class="metric-value"><%= resumen.esperaMedia()==null?"Sin datos":String.format(Locale.ROOT,"%.1f min",resumen.esperaMedia()) %></div><div class="metric-sub">Desde la llegada hasta iniciar la atención<br>Solo registros con ambas horas válidas</div></div>
            </div>
            <div class="metrics-grid-2">
                <div class="metric-card"><div class="metric-title">Ocupación de agenda publicada</div><div class="metric-value"><%= porcentaje(reporte.ocupacion().ocupados(),reporte.ocupacion().capacidad()) %></div><div class="metric-sub"><%= reporte.ocupacion().ocupados() %> cupos ocupados de <%= reporte.ocupacion().capacidad() %> publicados en el período.<br>Incluye todos los estados que consumen horario; independiente del filtro de estado.</div></div>
                <div class="metric-card"><div class="metric-title">Reasignaciones pendientes</div><div class="metric-value"><%= reporte.ocupacion().ofertasPendientes() %></div><div class="metric-sub">Ofertas vigentes para citas futuras del período.<br><a href="${pageContext.request.contextPath}/admin/reasignaciones">Revisar ofertas</a></div></div>
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
