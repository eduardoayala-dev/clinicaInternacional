<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) nombrePaciente = "Paciente Registrado";

    pe.edu.utp.clinica.dao.CitaDAO citaDAO = new pe.edu.utp.clinica.dao.CitaDAO();

    List<Map<String, Object>> listaNotificaciones = (List<Map<String, Object>>) request.getAttribute("listaNotificaciones");
    if (usuario != null && listaNotificaciones == null) {
        listaNotificaciones = citaDAO.obtenerNotificacionesUsuario(usuario.getIdUsuario());
    }

    int unreadCount = 0;
    if (usuario != null) {
        unreadCount = citaDAO.contarNotificacionesSinLeer(usuario.getIdUsuario());
    }
    boolean tieneNotificacionesSinLeer = unreadCount > 0;
    int totalNotif = listaNotificaciones != null ? listaNotificaciones.size() : 0;

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
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
    <title>Alertas y Notificaciones - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/notificaciones.css">
</head>
<body>

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
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="${pageContext.request.contextPath}/paciente/reportes" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Reportes
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item active">
                <i data-lucide="bell" style="width: 18px;"></i> Alertas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi perfil
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= inicialesPaciente %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombrePaciente %></div>
                <div class="user-role-sm">Paciente</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Notificaciones</h1>
                <p class="header-subtitle"><%= unreadCount %> <%= unreadCount == 1 ? "notificación sin leer" : "notificaciones sin leer" %> (<%= totalNotif %> en total).</p>
            </div>
            <div style="display: flex; align-items: center; gap: 16px;">
                <% if (tieneNotificacionesSinLeer) { %>
                <a href="${pageContext.request.contextPath}/paciente/notificaciones?action=marcarLeidas" style="font-size: 13px; color: var(--primary); font-weight: 600; text-decoration: none;">Marcar todo como leído</a>
                <% } else { %>
                <span style="font-size: 13px; color: var(--text-light); font-weight: 500;">Todo leído</span>
                <% } %>
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell <%= tieneNotificacionesSinLeer ? "has-unread" : "" %>" title="Alertas">
                    <i data-lucide="bell" style="width: 20px;"></i>
                </a>
            </div>
        </div>

        <div class="cat-pills">
            <button class="cat-btn active" data-cat="Todas">Todas <span style="background: var(--primary); color: #fff; padding: 2px 6px; border-radius: 10px; font-size: 11px;"><%= totalNotif %></span></button>
            <button class="cat-btn" data-cat="Citas">Citas</button>
            <button class="cat-btn" data-cat="Resultados">Resultados</button>
            <button class="cat-btn" data-cat="Medicamentos">Medicamentos</button>
            <button class="cat-btn" data-cat="Sistema">Sistema</button>
        </div>

        <% if (listaNotificaciones != null && !listaNotificaciones.isEmpty()) {
            for (Map<String, Object> n : listaNotificaciones) {
                String tipo = String.valueOf(n.get("tipo"));
                String mensaje = (String) n.get("mensaje");
                Object fechaObj = n.get("fecha_envio");
                String fechaStr = fechaObj != null ? fechaObj.toString() : "";
                Object leidaObj = n.get("leida");
                boolean isLeida = false;
                if (leidaObj instanceof Boolean) {
                    isLeida = (Boolean) leidaObj;
                } else if (leidaObj instanceof Number) {
                    isLeida = ((Number) leidaObj).intValue() == 1;
                }

                String iconName = "bell";
                String iconClass = "icon-blue";
                String catAttr = "Sistema";
                String titleText = "Notificación del Sistema";
                if ("CITAS".equalsIgnoreCase(tipo) || tipo.contains("CITA")) {
                    iconName = "calendar";
                    iconClass = "icon-blue";
                    catAttr = "Citas";
                    titleText = "Confirmación de Cita";
                } else if ("RESULTADOS".equalsIgnoreCase(tipo)) {
                    iconName = "file-text";
                    iconClass = "icon-purple";
                    catAttr = "Resultados";
                    titleText = "Resultado Médico";
                } else if ("MEDICAMENTOS".equalsIgnoreCase(tipo)) {
                    iconName = "pill";
                    iconClass = "icon-green";
                    catAttr = "Medicamentos";
                    titleText = "Recordatorio de Medicamento";
                } else {
                    iconName = "info";
                    iconClass = "icon-gray";
                    catAttr = "Sistema";
                    titleText = "Aviso del Sistema";
                }
        %>
        <div class="notif-card <%= !isLeida ? "unread-bg" : "" %>" data-tipo="<%= catAttr %>">
            <div class="notif-icon <%= iconClass %>"><i data-lucide="<%= iconName %>"></i></div>
            <div class="notif-body">
                <div class="notif-head">
                    <div class="notif-title">
                        <%= titleText %>
                        <% if (!isLeida) { %>
                        <span class="notif-dot" title="Sin leer"></span>
                        <% } %>
                    </div>
                    <div class="notif-time"><%= fechaStr %></div>
                </div>
                <div class="notif-desc"><%= mensaje %></div>
                <div class="notif-actions">
                    <a href="${pageContext.request.contextPath}/paciente/dashboard" class="btn-act-primary">Ver mis citas</a>
                </div>
            </div>
        </div>
        <%  }
        } else { %>
        <div style="background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light); padding: 56px 24px; text-align: center; color: var(--text-gray);">
            <i data-lucide="bell-off" style="width: 44px; height: 44px; stroke-width: 1.5; margin-bottom: 12px; color: #94a3b8;"></i>
            <div style="font-size: 16px; font-weight: 600; color: var(--text-dark); margin-bottom: 4px;">No tienes notificaciones o alertas pendientes</div>
            <div style="font-size: 13px;">Las confirmaciones de tus citas y recordatorios clínicos aparecerán aquí automáticamente.</div>
        </div>
        <% } %>
    </main>

    <script>
        lucide.createIcons();

        document.querySelectorAll('.cat-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                document.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                const cat = this.getAttribute('data-cat');
                document.querySelectorAll('.notif-card').forEach(card => {
                    if (!cat || cat === 'Todas' || card.getAttribute('data-tipo') === cat) {
                        card.style.display = 'flex';
                    } else {
                        card.style.display = 'none';
                    }
                });
            });
        });
    </script>
</body>
</html>

