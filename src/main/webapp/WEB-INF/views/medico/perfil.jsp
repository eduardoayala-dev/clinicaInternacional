<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    String seccionMedico = "perfil";
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Médico";

    String emailVal = String.valueOf(((java.util.Map<?,?>)request.getAttribute("detallesMedico")).get("email"));

    String inicialesMedico = "M";
    if (nombreMedico != null && !nombreMedico.trim().isEmpty()) {
        String[] partes = nombreMedico.replace("Dr.", "").replace("Dra.", "").trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesMedico = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesMedico = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Perfil del Médico - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Serif+Display&amp;family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css?v=20261028">
    <script defer src="${pageContext.request.contextPath}/assets/js/medico-portal.js?v=20261028"></script>
</head>
<body>

    <%@ include file="/WEB-INF/jspf/medico-sidebar.jspf" %>

    <div class="med-main-wrapper">
        <%@ include file="/WEB-INF/jspf/medico-topbar.jspf" %>
    <main class="med-content-scroll" id="medContenido" tabindex="-1">
        <div class="med-header-row">
            <div>
                <h1 class="med-title">Perfil del Médico</h1>
                <p class="med-subtitle">Información profesional, especialidad y credenciales registradas.</p>
            </div>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="med-btn-outline"><img src="${pageContext.request.contextPath}/assets/images/medico/agenda.svg" alt="" class="med-icon"> Mi agenda</a>
        </div>

        <%
            java.util.Map<String, Object> detMed = (java.util.Map<String, Object>) request.getAttribute("detallesMedico");
            String cmpVal = detMed != null && detMed.get("cmp") != null ? (String) detMed.get("cmp") : "Sin registrar";
            String espVal = detMed != null && detMed.get("especialidad") != null ? (String) detMed.get("especialidad") : "Medicina General";
            String sedeVal = detMed != null && detMed.get("sedeNombre") != null ? (String) detMed.get("sedeNombre") : "Sin registrar";
        %>

        <div class="med-profile-grid">

            <div class="med-profile-card">
                <div class="med-profile-avatar">
                    <div class="med-avatar"><%= Html.texto(inicialesMedico) %></div>
                    <div class="med-profile-name"><%= Html.texto(nombreMedico) %></div>
                    <span class="med-profile-badge">Perfil médico activo</span>
                </div>

                <div class="med-profile-field">
                    <div class="med-profile-label">Colegiatura / CMP</div>
                    <div class="med-profile-value"><%= Html.texto(cmpVal) %></div>
                </div>

                <div class="med-profile-field">
                    <div class="med-profile-label">Especialidad</div>
                    <div class="med-profile-value"><%= Html.texto(espVal) %></div>
                </div>
            </div>


            <div class="med-profile-card">
                <h2>
                    Detalles Institucionales
                </h2>

                <div class="med-profile-field">
                    <div class="med-profile-label">Correo Institucional</div>
                    <div class="med-profile-value"><%= Html.texto(emailVal) %></div>
                </div>

                <div class="med-profile-field">
                    <div class="med-profile-label">Sede Asignada Principal</div>
                    <div class="med-profile-value"><%= Html.texto(sedeVal) %></div>
                </div>

                <div class="med-profile-field">
                    <div class="med-profile-label">Consulta de agenda</div>
                    <div class="med-profile-value">Consulta las citas por fecha en Mi agenda.</div>
                </div>
            </div>
        </div>
    </main>
    </div>


</body>
</html>
