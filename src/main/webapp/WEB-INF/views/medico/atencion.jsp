<%@ page import="pe.edu.utp.clinica.util.Html,pe.edu.utp.clinica.model.medico.ConsultaClinica" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%
    String seccionMedico = "atencion";
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Médico";


    String inicialesMedico = "M";
    if (nombreMedico != null && !nombreMedico.trim().isEmpty()) {
        String cleanNom = nombreMedico.replace("Dr.", "").replace("Dra.", "").trim();
        String[] partes = cleanNom.split("\\s+");
        if (partes.length >= 2) {
            inicialesMedico = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else if (partes.length == 1 && !partes[0].isEmpty()) {
            inicialesMedico = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    String mensajeExito = (String) request.getAttribute("mensajeExito");
    Integer idCitaObj = (Integer) request.getAttribute("idCita");
    int idCita = (idCitaObj != null) ? idCitaObj : -1;

    Map<String, Object> cita = (Map<String, Object>) request.getAttribute("cita");
    boolean hayPacienteActivo = (cita != null && !cita.isEmpty());
    ConsultaClinica consultaClinica = hayPacienteActivo ? (ConsultaClinica) cita.get("consultaClinica") : null;
    if (consultaClinica == null) consultaClinica = new ConsultaClinica("", "", "", "", "", "");

    String mensajeOperativo = "Cita de solo lectura; no permite iniciar ni modificar la atención.";
    if (cita != null) {
        if (Boolean.TRUE.equals(cita.get("editable"))) mensajeOperativo = "Consulta en curso. Completa la indicación antes de finalizar.";
        else if (Boolean.TRUE.equals(cita.get("puedeIniciar"))) mensajeOperativo = "Llegada registrada. Puedes iniciar la consulta.";
        else if ("EN_SALA".equals(cita.get("estado"))) mensajeOperativo = Boolean.TRUE.equals(cita.get("consultaOcupada"))
                ? "El médico o el paciente tiene otra consulta activa. Debe finalizar antes de iniciar esta atención."
                : "Solo se puede iniciar una cita de hoy con la llegada de hoy registrada por admisión.";
        else if ("CONFIRMADA".equals(cita.get("estado")) || "SOLICITADA".equals(cita.get("estado"))) mensajeOperativo = "Admisión debe registrar la llegada del paciente antes de iniciar la atención.";
    }


    String pacienteNombre = hayPacienteActivo && cita.get("pacienteNombre") != null ? (String) cita.get("pacienteNombre") : "";
    String dni = hayPacienteActivo && cita.get("dni") != null ? (String) cita.get("dni") : "";
    String telefono = hayPacienteActivo && cita.get("telefono") != null ? (String) cita.get("telefono") : "";
    String tipoCobertura = hayPacienteActivo && cita.get("tipoCobertura") != null ? (String) cita.get("tipoCobertura") : "Sin registrar";
    String alergias = hayPacienteActivo && cita.get("alergias") != null ? (String) cita.get("alergias") : "Sin registrar";

    String fechaHora = hayPacienteActivo && cita.get("fechaFormateada") != null ? (String) cita.get("fechaFormateada") : "";
    String sedeYConsultorio = hayPacienteActivo && cita.get("sedeYConsultorio") != null ? (String) cita.get("sedeYConsultorio") : "Sin registrar";
    String medicoTratante = hayPacienteActivo && cita.get("medicoNombre") != null ? (String) cita.get("medicoNombre") : nombreMedico;
    String motivo = hayPacienteActivo && cita.get("motivo") != null ? (String) cita.get("motivo") : "";
    if (motivo.isBlank()) motivo = "Sin registrar";
    String canalReserva = hayPacienteActivo && cita.get("canalReserva") != null ? (String) cita.get("canalReserva") : "Reserva web";
    String fechaRegistro = hayPacienteActivo && cita.get("fechaRegistro") != null ? (String) cita.get("fechaRegistro") : "";
    String estadoCita = hayPacienteActivo && cita.get("estado") != null ? (String) cita.get("estado") : "CONFIRMADA";
    String urgencia = hayPacienteActivo && cita.get("urgencia") != null ? (String) cita.get("urgencia") : "MEDIA";
    String fechaRecomendadaProxima = hayPacienteActivo && cita.get("fechaRecomendadaProxima") != null && !((String) cita.get("fechaRecomendadaProxima")).isEmpty()
            ? (String) cita.get("fechaRecomendadaProxima") : "";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Atención de paciente actual — Clínica Internacional</title>

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


            <% if (mensajeExito != null) { %>
                <div>
                    <img src="${pageContext.request.contextPath}/assets/images/medico/informacion.svg" alt="" class="med-icon">
                    <%= Html.texto(mensajeExito) %>
                </div>
            <% } %>

            <% if (hayPacienteActivo) { %>



                <div class="med-atencion-banner">
                    <img src="${pageContext.request.contextPath}/assets/images/medico/informacion.svg" alt="" class="med-icon">
                    <div class="med-atencion-banner-text">
                        <span class="med-atencion-banner-strong">Mensaje operativo del sistema</span><br>
                        Estado registrado: <%= Html.texto(cita.get("estadoTexto")) %>
                    </div>
                </div>


                <div class="med-atencion-header">
                    <div>
                        <h1 class="med-atencion-title">Atención de paciente</h1>
                        <div class="med-atencion-subtitle">Detalle de la cita · <%= Html.texto(pacienteNombre) %></div>
                    </div>
                    <div class="med-atencion-fecha-reg">
                        <% if (!fechaRegistro.isEmpty()) { %>
                            Fecha de registro: <%= Html.texto(fechaRegistro) %>
                        <% } %>
                    </div>
                </div>

                    <div class="med-atencion-card">
                        <div class="med-atencion-card-head">
                            <span class="med-atencion-tag-title">Datos registrados de la cita</span>
                            <span class="med-badge-solo-lectura">Solo lectura</span>
                        </div>

                        <div class="med-meta-grid">

                            <div>
                                <div class="med-meta-col-title">Información del paciente</div>
                                <div class="med-meta-list">
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Nombre completo</span>
                                        <span class="med-meta-val"><%= Html.texto(pacienteNombre) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Documento / ficha</span>
                                        <span class="med-meta-val">DNI <%= Html.texto(dni) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Teléfono de contacto</span>
                                        <span class="med-meta-val"><%= Html.texto(telefono) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Tipo de cobertura</span>
                                        <span class="med-meta-val"><%= Html.texto(tipoCobertura) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Alergias registradas</span>
                                        <span class="med-meta-val">
                                            <% if (alergias.trim().isEmpty()) { %>
                                                <span>Sin registrar</span>
                                            <% } else if ("Ninguna".equalsIgnoreCase(alergias)) { %>
                                                <span class="med-pill-alergia ninguna">Ninguna</span>
                                            <% } else { %>
                                                <span class="med-pill-alergia"><%= Html.texto(alergias) %></span>
                                            <% } %>
                                        </span>
                                    </div>
                                </div>
                            </div>


                            <div>
                                <div class="med-meta-col-title">Detalles de programación</div>
                                <div class="med-meta-list">
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Fecha y hora</span>
                                        <span class="med-meta-val"><%= Html.texto(fechaHora) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Sede de la cita</span>
                                        <span class="med-meta-val"><%= Html.texto(sedeYConsultorio) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Médico tratante</span>
                                        <span class="med-meta-val"><%= Html.texto(medicoTratante) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Motivo de consulta</span>
                                        <span class="med-meta-val"><%= Html.texto(motivo) %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Canal de reserva</span>
                                        <span class="med-meta-val"><%= Html.texto(canalReserva) %></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>


                <div class="med-atencion-card">
                    <div class="med-atencion-card-head"><h2 class="med-atencion-tag-title">Control operativo de atención</h2></div>
                    <dl class="med-registro-tiempos">
                        <div><dt>Llegada registrada</dt><dd><%= Html.texto(cita.get("horaLlegada")) %></dd></div>
                        <div><dt>Inicio de atención</dt><dd><%= Html.texto(cita.get("inicioTexto")) %></dd></div>
                        <div><dt>Cierre de atención</dt><dd><%= Html.texto(cita.get("finTexto")) %></dd></div>
                    </dl>
                    <div class="med-consulta-resumen">
                        <div class="med-estado-recepcion-card"><img src="${pageContext.request.contextPath}/assets/images/medico/editar.svg" alt="" class="med-icon">
                            <div><div class="med-estado-recepcion-title">Estado actual: <%= Html.texto(cita.get("estadoTexto")) %></div>
                            <div class="med-estado-recepcion-desc"><%= Html.texto(mensajeOperativo) %></div></div>
                        </div>
                        <% if (Boolean.TRUE.equals(cita.get("puedeIniciar"))) { %>
                            <form id="formIniciarAtencion" method="POST" data-pending-label="Iniciando…" action="${pageContext.request.contextPath}/medico/atencion">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="idCita" value="<%= Html.texto(idCita) %>">
                                <input type="hidden" name="accion" value="iniciar">
                                <button class="med-btn-guardar" type="submit"><img src="${pageContext.request.contextPath}/assets/images/medico/cargando-claro.svg" alt="" class="med-icon med-loading-icon"><span class="med-button-label">Iniciar atención</span></button>
                            </form>
                        <% } %>
                        <div class="med-metricas">
                            <% if (cita.get("esperaMinutos") != null) { %><span>Espera: <strong><%= Html.texto(((Number)cita.get("esperaMinutos")).longValue()==0 ? "Menos de 1 min" : cita.get("esperaMinutos")+" min") %></strong></span><% } %>
                            <% if (cita.get("duracionMinutos") != null) { %><span>Duración: <strong><%= Html.texto(((Number)cita.get("duracionMinutos")).longValue()==0 ? "Menos de 1 min" : cita.get("duracionMinutos")+" min") %></strong></span><% } %>
                        </div>
                    </div>
                    <% if ("EN_SALA".equals(estadoCita) && !Boolean.TRUE.equals(cita.get("puedeIniciar"))) { %>
                        <p class="med-reason">Para iniciar, la cita debe ser de hoy, tener llegada registrada y el médico y paciente deben estar libres de otra consulta activa.</p>
                    <% } %>
                    <% if ("NO_ATENDIDA".equals(estadoCita)) { %><p class="med-reason">Motivo de no atención: <strong><%= Html.texto(cita.get("motivoNoAtencion")) %></strong></p><% } %>
                </div>

                <form id="formAtencion" action="${pageContext.request.contextPath}/medico/atencion" method="POST" data-pending-label="Guardando…">
<fieldset class="med-atencion-fieldset" <%= Html.texto(Boolean.TRUE.equals(cita.get("editable")) ? "" : "disabled") %>>
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="idCita" value="<%= Html.texto(idCita) %>">
                    <input type="hidden" name="accion" id="accionInput" value="guardar">
                    <input type="hidden" name="prioridad" id="prioridadInput" value="<%= Html.texto(urgencia) %>">
                    <section class="med-clinical-section" aria-label="Registro clínico de la consulta">
                        <h2 class="med-section-heading">Registro clínico</h2>
                        <p class="med-subtitle">Completa el registro antes de finalizar. Los datos se conservarán como parte de esta atención.</p>
                        <% if (!Boolean.TRUE.equals(cita.get("editable")) && consultaClinica.diagnostico().isBlank()) { %><p>Esta cita no tiene un registro clínico estructurado. Las indicaciones anteriores se conservan abajo.</p><% } %>
                        <div class="med-clinical-grid">
                            <div class="med-form-group"><label class="med-field-label" for="motivoClinico">Motivo de consulta *</label>
                            <textarea class="med-clinical-input" id="motivoClinico" name="motivoClinico" maxlength="1000" rows="3" required><%= Html.texto(consultaClinica.motivo().isBlank() && Boolean.TRUE.equals(cita.get("editable")) ? cita.get("motivo") : consultaClinica.motivo()) %></textarea></div>
                            <div class="med-form-group"><label class="med-field-label" for="antecedentesClinicos">Antecedentes relevantes</label>
                            <textarea class="med-clinical-input" id="antecedentesClinicos" name="antecedentes" maxlength="2000" rows="3"><%= Html.texto(consultaClinica.antecedentes()) %></textarea></div>
                            <div class="med-form-group"><label class="med-field-label" for="hallazgosClinicos">Hallazgos de la consulta</label>
                            <textarea class="med-clinical-input" id="hallazgosClinicos" name="hallazgos" maxlength="2000" rows="3"><%= Html.texto(consultaClinica.hallazgos()) %></textarea></div>
                            <div class="med-form-group"><label class="med-field-label" for="evaluacionClinica">Evaluación clínica *</label>
                            <textarea class="med-clinical-input" id="evaluacionClinica" name="evaluacion" maxlength="2000" rows="3" required><%= Html.texto(consultaClinica.evaluacion()) %></textarea></div>
                            <div class="med-form-group"><label class="med-field-label" for="diagnosticoClinico">Diagnóstico o impresión diagnóstica *</label>
                            <textarea class="med-clinical-input" id="diagnosticoClinico" name="diagnostico" maxlength="1000" rows="3" required><%= Html.texto(consultaClinica.diagnostico()) %></textarea></div>
                            <div class="med-form-group"><label class="med-field-label" for="planClinico">Plan de atención *</label>
                            <textarea class="med-clinical-input" id="planClinico" name="plan" maxlength="2000" rows="3" required><%= Html.texto(consultaClinica.plan()) %></textarea></div>
                        </div>
                    </section>



                    <div class="med-atencion-card">
                        <div class="med-atencion-tag-title">Plan de seguimiento</div>

                        <span class="med-field-label">Nivel de prioridad clínica *</span>

                        <fieldset class="med-prioridad-cards" aria-label="Nivel de prioridad clínica"><legend class="med-sr-only">Nivel de prioridad clínica</legend>
                            <button type="button" class="med-prio-card <%= Html.texto("ALTA".equalsIgnoreCase(urgencia) ? "active" : "") %>" data-prioridad="ALTA" aria-pressed="<%= Html.texto("ALTA".equalsIgnoreCase(urgencia) ? "true" : "false") %>" onclick="setPrioridad('ALTA', this)">
                                <span class="med-prio-card-left"><img src="${pageContext.request.contextPath}/assets/images/medico/prioridad-alta.svg" alt="" class="med-icon"><span><span class="med-prio-title">Alta</span><span class="med-prio-sub">Requiere atención pronta</span></span></span>
                                <span class="med-prio-check"><img src="${pageContext.request.contextPath}/assets/images/medico/confirmar.svg" alt="" class="med-icon"></span>
                            </button>
                            <button type="button" class="med-prio-card <%= Html.texto(!"ALTA".equalsIgnoreCase(urgencia) && !"BAJA".equalsIgnoreCase(urgencia) ? "active" : "") %>" data-prioridad="MEDIA" aria-pressed="<%= Html.texto(!"ALTA".equalsIgnoreCase(urgencia) && !"BAJA".equalsIgnoreCase(urgencia) ? "true" : "false") %>" onclick="setPrioridad('MEDIA', this)">
                                <span class="med-prio-card-left"><img src="${pageContext.request.contextPath}/assets/images/medico/prioridad-media.svg" alt="" class="med-icon"><span><span class="med-prio-title">Media</span><span class="med-prio-sub">Seguimiento regular</span></span></span>
                                <span class="med-prio-check"><img src="${pageContext.request.contextPath}/assets/images/medico/confirmar.svg" alt="" class="med-icon"></span>
                            </button>
                            <button type="button" class="med-prio-card <%= Html.texto("BAJA".equalsIgnoreCase(urgencia) ? "active" : "") %>" data-prioridad="BAJA" aria-pressed="<%= Html.texto("BAJA".equalsIgnoreCase(urgencia) ? "true" : "false") %>" onclick="setPrioridad('BAJA', this)">
                                <span class="med-prio-card-left"><img src="${pageContext.request.contextPath}/assets/images/medico/prioridad-baja.svg" alt="" class="med-icon"><span><span class="med-prio-title">Baja</span><span class="med-prio-sub">Control de rutina</span></span></span>
                                <span class="med-prio-check"><img src="${pageContext.request.contextPath}/assets/images/medico/confirmar.svg" alt="" class="med-icon"></span>
                            </button>
                        </fieldset>

                        <label class="med-field-label" for="fechaProximaInput">Fecha recomendada de próxima atención (opcional)</label>
                        <div class="med-date-group">
                            <div class="med-input-date-wrap">
                                <input min="<%= Html.texto(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(1)) %>" type="date" name="fechaProxima" id="fechaProximaInput" class="med-input-date" value="<%= Html.texto(fechaRecomendadaProxima) %>" onchange="actualizarBadgeMeses()">
                                <img src="${pageContext.request.contextPath}/assets/images/medico/calendario-campo.svg" alt="" class="med-icon med-field-icon">
                            </div>
                            <span class="med-badge-meses" id="badgeMeses">Sin fecha de seguimiento</span>
                        </div>


                        <div class="med-textarea-header">
                            <label class="med-field-label" for="indicacionesTextarea">
                                Indicación breve de continuidad *
                            </label>
                            <span class="med-char-counter" id="charCounter">0 / 200 caracteres</span>
                        </div>

                        <textarea required name="indicaciones" id="indicacionesTextarea" class="med-textarea-atencion" maxlength="200" oninput="actualizarContador(this)"><%= Html.texto(cita.get("indicaciones")) %></textarea>

                        <div class="med-disclaimer">
                            <img src="${pageContext.request.contextPath}/assets/images/medico/nota.svg" alt="" class="med-icon">
                            <span>Nota: esta indicación orienta la próxima cita. No reemplaza ni constituye una historia clínica.</span>
                        </div>
                    </div>


                    <div class="med-bottom-actions">
                        <button type="button" class="med-btn-restablecer" onclick="restablecerFormulario()">
                            Restablecer
                        </button>
                        <button type="submit" class="med-btn-guardar">
                            <img src="${pageContext.request.contextPath}/assets/images/medico/guardar.svg" alt="" class="med-icon med-submit-icon"><img src="${pageContext.request.contextPath}/assets/images/medico/cargando-claro.svg" alt="" class="med-icon med-loading-icon"><span class="med-button-label">Finalizar atención</span>
                        </button>
                    </div>
                </fieldset></form>
                <% if (Boolean.TRUE.equals(cita.get("puedeNoAtender"))) { %>
                    <details class="med-atencion-card med-critical-panel">
                    <summary>Registrar que no se completó la atención</summary>
                    <form id="formNoAtendida" method="POST" data-pending-label="Registrando…" action="${pageContext.request.contextPath}/medico/atencion" onsubmit="return confirm('¿Confirmar que esta cita termina sin atención completa?')">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="accion" value="no_atender">
                        <input type="hidden" name="idCita" value="<%= Html.texto(idCita) %>">
                        <label class="med-field-label" for="motivoNoAtencion">Motivo de no atención</label>
                        <textarea required minlength="3" maxlength="200" id="motivoNoAtencion" name="motivo" class="med-textarea-atencion"></textarea>
                        <button type="submit" class="med-btn-no-presento"><img src="${pageContext.request.contextPath}/assets/images/medico/cargando.svg" alt="" class="med-icon med-loading-icon"><span class="med-button-label">Registrar como no atendida</span></button>
                    </form>
                    </details>
                <% } %>

            <% } else { %>

                <div class="med-atencion-header">
                    <div>
                        <h1 class="med-atencion-title">Atención de paciente</h1>
                        <div class="med-atencion-subtitle">Módulo de atención clínica ambulatoria en consultorio</div>
                    </div>
                </div>

                <div class="med-atencion-card med-empty-state">
                    <div class="med-empty-icon">
                        <img src="${pageContext.request.contextPath}/assets/images/medico/informacion.svg" alt="" class="med-icon">
                    </div>
                    <h2>
                        No hay ningún paciente en atención en este momento
                    </h2>
                    <p>
                        Para iniciar una atención, diríjase a su lista de citas del turno o agenda y seleccione al paciente admitido en recepción mediante la opción <strong>«Atender»</strong> o <strong>«Ver ficha»</strong> y luego <strong>«Iniciar atención»</strong>.
                    </p>
                    <div class="med-header-actions">
                        <a href="${pageContext.request.contextPath}/medico/inicio" class="med-btn-guardar">
                            <img src="${pageContext.request.contextPath}/assets/images/medico/inicio.svg" alt="" class="med-icon"> Ir a Inicio
                        </a>
                        <a href="${pageContext.request.contextPath}/medico/agenda" class="med-btn-restablecer">
                            <img src="${pageContext.request.contextPath}/assets/images/medico/agenda.svg" alt="" class="med-icon"> Ver mi agenda
                        </a>
                    </div>
                </div>

            <% } %>

        </main>
    </div>


    <script src="${pageContext.request.contextPath}/assets/js/medico-atencion.js"></script>
</body>
</html>
