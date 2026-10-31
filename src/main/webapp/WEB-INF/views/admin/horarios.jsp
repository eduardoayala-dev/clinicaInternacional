<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.util.FormatoClinico,pe.edu.utp.clinica.model.medico.PropuestaHorario,pe.edu.utp.clinica.model.Usuario" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePersonal = usuario.getNombres()+" "+usuario.getApellidos();
    String inicialesPersonal = nombrePersonal.isBlank() ? "A" : nombrePersonal.substring(0,1);
    var propuestas = (java.util.List<PropuestaHorario>) request.getAttribute("propuestas");
    var hoy = (java.time.LocalDate) request.getAttribute("hoy");
    request.setAttribute("revisionHorarios", true);
%>
<!DOCTYPE html><html lang="es"><head><title>Propuestas de horarios — Admisión</title>
<%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/horarios-propuestas.css">
<script defer src="${pageContext.request.contextPath}/assets/js/horarios-propuestas.js"></script>
</head><body class="hor-adm">
<%@ include file="/WEB-INF/jspf/admin-sidebar-medicos.jspf" %>

<div class="adm-main-wrapper"><header class="adm-topbar"><div class="adm-breadcrumb">Admisión / Propuestas de horarios</div></header>
<main class="adm-content-scroll">
<a class="med-btn-outline hor-volver" href="${pageContext.request.contextPath}/admin/medicos">Volver a médicos</a>
<h1 class="adm-page-title">Propuestas de horarios</h1>
<p class="adm-page-subtitle">Revisa las propuestas de los médicos de tu sede antes de publicar cupos para los pacientes.</p>
<p>Antes de aprobar, confirma que haya un consultorio disponible para el turno. El sistema comprobará los cruces de agenda del médico.</p>
<%@ include file="/WEB-INF/jspf/horarios-mensajes.jspf" %>
<p>Se muestran hasta 100 propuestas, primero las pendientes. Al aprobar se publican únicamente los turnos futuros de la vigencia. Si hay un cruce, no se publica ningún turno de la propuesta.</p>
<%@ include file="/WEB-INF/jspf/horarios-tabla.jspf" %>
</main></div></body></html>
