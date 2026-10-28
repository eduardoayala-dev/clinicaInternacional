<%@ page import="pe.edu.utp.clinica.util.Html" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Médico";

    String especialidadMedico = String.valueOf(request.getAttribute("especialidadMedico"));

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

    String pacienteNombre = hayPacienteActivo && cita.get("pacienteNombre") != null ? (String) cita.get("pacienteNombre") : "";
    String dni = hayPacienteActivo && cita.get("dni") != null ? (String) cita.get("dni") : "";
    String telefono = hayPacienteActivo && cita.get("telefono") != null ? (String) cita.get("telefono") : "";
    String tipoCobertura = hayPacienteActivo && cita.get("tipoCobertura") != null ? (String) cita.get("tipoCobertura") : "Sin registrar";
    String alergias = hayPacienteActivo && cita.get("alergias") != null ? (String) cita.get("alergias") : "Sin registrar";

    String fechaHora = hayPacienteActivo && cita.get("fechaFormateada") != null ? (String) cita.get("fechaFormateada") : "";
    String sedeYConsultorio = hayPacienteActivo && cita.get("sedeYConsultorio") != null ? (String) cita.get("sedeYConsultorio") : "Sin registrar";
    String medicoTratante = hayPacienteActivo && cita.get("medicoNombre") != null ? (String) cita.get("medicoNombre") : nombreMedico;
    String motivo = hayPacienteActivo && cita.get("motivo") != null ? (String) cita.get("motivo") : "Control general";
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
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">

    <script src="https://unpkg.com/lucide@latest"></script>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css">
</head>
<body>


    <aside class="med-sidebar">
        <a href="${pageContext.request.contextPath}/medico/inicio" class="med-brand">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clínica Internacional" class="med-brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div>
                <div class="med-brand-name">Clínica Internacional</div>
                <div class="med-brand-sub">Portal del médico</div>
            </div>
        </a>

        <nav class="med-nav">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="med-nav-item">
                <i data-lucide="home"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="med-nav-item">
                <i data-lucide="calendar"></i> Mi agenda
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion<%= Html.texto(hayPacienteActivo ? ("?idCita=" + idCita) : "") %>" class="med-nav-item active">
                <i data-lucide="stethoscope"></i> Atención de paciente actual
            </a>
        </nav>

        <div class="med-user-footer">
            <a href="${pageContext.request.contextPath}/medico/perfil" class="med-user-info-link" title="Mi perfil">
                <div class="med-avatar"><%= Html.texto(inicialesMedico) %></div>
                <div style="flex: 1; min-width: 0;">
                    <div class="med-user-name"><%= Html.texto(nombreMedico) %></div>
                    <div class="med-user-role"><%= Html.texto(especialidadMedico) %></div>
                </div>
            </a>
            <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="med-logout-btn" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                <i data-lucide="log-out"></i>
            </button>
</form>
        </div>
    </aside>


    <div class="med-main-wrapper">

        <header class="med-topbar">
            <div class="med-breadcrumb">
                Médico &nbsp;/&nbsp; <span>Atención de paciente actual</span>
            </div>
        </header>


        <main class="med-content-scroll">


            <% if (mensajeExito != null) { %>
                <div style="background: #dcfce7; border: 1px solid #86efac; color: #166534; padding: 12px 18px; border-radius: 8px; margin-bottom: 20px; font-size: 13.5px; font-weight: 600; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="check-circle" style="width: 18px; height: 18px; color: #16a34a;"></i>
                    <%= Html.texto(mensajeExito) %>
                </div>
            <% } %>

            <% if (hayPacienteActivo) { %>



                <div class="med-atencion-banner">
                    <i data-lucide="info"></i>
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
                    <p>Llegada: <strong><%= Html.texto(cita.get("horaLlegada")) %></strong> · Inicio: <strong><%= Html.texto(cita.get("inicioTexto")) %></strong> · Cierre: <strong><%= Html.texto(cita.get("finTexto")) %></strong></p>
                    <% if (cita.get("esperaMinutos") != null) { %><p>Espera registrada: <%= Html.texto(((Number)cita.get("esperaMinutos")).longValue()==0?"Menos de 1 min":cita.get("esperaMinutos")+" min") %></p><% } %>
                    <% if (cita.get("duracionMinutos") != null) { %><p>Duración de consulta: <%= Html.texto(((Number)cita.get("duracionMinutos")).longValue()==0?"Menos de 1 min":cita.get("duracionMinutos")+" min") %></p><% } %>
                    <% if (Boolean.TRUE.equals(cita.get("puedeIniciar"))) { %>
                        <form id="formIniciarAtencion" method="POST" action="${pageContext.request.contextPath}/medico/atencion">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="idCita" value="<%= Html.texto(idCita) %>">
                            <input type="hidden" name="accion" value="iniciar">
                            <button class="med-btn-guardar" type="submit">Iniciar atención</button>
                        </form>
                    <% } else if ("EN_SALA".equals(estadoCita)) { %>
                        <p>Para iniciar, la cita debe ser de hoy, tener llegada registrada y el médico y paciente deben estar libres de otra consulta activa.</p>
                    <% } %>
                    <% if ("EN_ATENCION".equals(estadoCita)) { %><p>Consulta en curso. Completa la indicación antes de finalizar.</p><% } %>
                    <% if ("NO_ATENDIDA".equals(estadoCita)) { %><p>Motivo de no atención: <strong><%= Html.texto(cita.get("motivoNoAtencion")) %></strong></p><% } %>
                </div>

                <form id="formAtencion" action="${pageContext.request.contextPath}/medico/atencion" method="POST">
<fieldset class="med-atencion-fieldset" <%= Html.texto(Boolean.TRUE.equals(cita.get("editable")) ? "" : "disabled") %>>
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="idCita" value="<%= Html.texto(idCita) %>">
                    <input type="hidden" name="accion" id="accionInput" value="guardar">
                    <input type="hidden" name="prioridad" id="prioridadInput" value="<%= Html.texto(urgencia) %>">


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
                        <div class="med-atencion-tag-title" style="margin-bottom: 14px;">Control operativo requerido</div>

                        <div class="med-aviso-box">
                            Registra únicamente información verificada durante la atención.
                        </div>

                        <div class="med-control-action-bar">
                            <div class="med-estado-recepcion-card">
                                <i data-lucide="edit-3"></i>
                                <div>
                                    <div class="med-estado-recepcion-title">Estado actual: <%= Html.texto(cita.get("estadoTexto")) %></div>
                                    <div class="med-estado-recepcion-desc">La atención debe iniciarse antes de guardar el cierre. Las consultas cerradas quedan en solo lectura.</div>
                                </div>
                            </div>
                        </div>
                    </div>


                    <div class="med-atencion-card">
                        <div class="med-atencion-tag-title" style="margin-bottom: 14px;">Plan de seguimiento</div>

                        <span class="med-field-label">Nivel de prioridad clínica *</span>

                        <div class="med-prioridad-cards">

                            <div class="med-prio-card <%= Html.texto("ALTA".equalsIgnoreCase(urgencia) ? "active" : "") %>" onclick="setPrioridad('ALTA', this)">
                                <div class="med-prio-card-left">
                                    <span class="med-prio-dot alta"></span>
                                    <div>
                                        <div class="med-prio-title">Alta</div>
                                        <div class="med-prio-sub">Requiere atención pronta</div>
                                    </div>
                                </div>
                                <div class="med-prio-check"><i data-lucide="check"></i></div>
                            </div>


                            <div class="med-prio-card <%= Html.texto(!"ALTA".equalsIgnoreCase(urgencia) && !"BAJA".equalsIgnoreCase(urgencia) ? "active" : "") %>" onclick="setPrioridad('MEDIA', this)">
                                <div class="med-prio-card-left">
                                    <span class="med-prio-dot media"></span>
                                    <div>
                                        <div class="med-prio-title">Media</div>
                                        <div class="med-prio-sub">Seguimiento regular</div>
                                    </div>
                                </div>
                                <div class="med-prio-check"><i data-lucide="check"></i></div>
                            </div>


                            <div class="med-prio-card <%= Html.texto("BAJA".equalsIgnoreCase(urgencia) ? "active" : "") %>" onclick="setPrioridad('BAJA', this)">
                                <div class="med-prio-card-left">
                                    <span class="med-prio-dot baja"></span>
                                    <div>
                                        <div class="med-prio-title">Baja</div>
                                        <div class="med-prio-sub">Control de rutina</div>
                                    </div>
                                </div>
                                <div class="med-prio-check"><i data-lucide="check"></i></div>
                            </div>
                        </div>


                        <label class="med-field-label" for="fechaProximaInput">Fecha recomendada de próxima atención (opcional)</label>
                        <div class="med-date-group">
                            <div class="med-input-date-wrap">
                                <input min="<%= Html.texto(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(1)) %>" type="date" name="fechaProxima" id="fechaProximaInput" class="med-input-date" value="<%= Html.texto(fechaRecomendadaProxima) %>" onchange="actualizarBadgeMeses()">
                            </div>
                            <span class="med-badge-meses" id="badgeMeses">Sin fecha de seguimiento</span>
                        </div>


                        <div class="med-textarea-header">
                            <label class="med-field-label" for="indicacionesTextarea" style="margin-bottom: 0;">
                                Indicación breve de continuidad (texto libre)
                            </label>
                            <span class="med-char-counter" id="charCounter">0 / 200 caracteres</span>
                        </div>

                        <textarea required name="indicaciones" id="indicacionesTextarea" class="med-textarea-atencion" maxlength="200" oninput="actualizarContador(this)"><%= Html.texto(cita.get("indicaciones")) %></textarea>

                        <div class="med-disclaimer">
                            <i data-lucide="info"></i>
                            <span>Nota: esta indicación orienta la próxima cita. No reemplaza ni constituye una historia clínica.</span>
                        </div>
                    </div>


                    <div class="med-bottom-actions">
                        <button type="button" class="med-btn-restablecer" onclick="restablecerFormulario()">
                            Restablecer
                        </button>
                        <button type="submit" class="med-btn-guardar">
                            <i data-lucide="save"></i>
                            Finalizar atención
                        </button>
                    </div>
                </fieldset></form>
                <% if (Boolean.TRUE.equals(cita.get("puedeNoAtender"))) { %>
                    <form id="formNoAtendida" class="med-atencion-card" method="POST" action="${pageContext.request.contextPath}/medico/atencion" onsubmit="return confirm('¿Confirmar que esta cita termina sin atención completa?')">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="accion" value="no_atender">
                        <input type="hidden" name="idCita" value="<%= Html.texto(idCita) %>">
                        <label class="med-field-label" for="motivoNoAtencion">Motivo de no atención</label>
                        <textarea required minlength="3" maxlength="200" id="motivoNoAtencion" name="motivo" class="med-textarea-atencion"></textarea>
                        <button type="submit" class="med-btn-no-presento">Registrar como no atendida</button>
                    </form>
                <% } %>

            <% } else { %>

                <div class="med-atencion-header">
                    <div>
                        <h1 class="med-atencion-title">Atención de paciente</h1>
                        <div class="med-atencion-subtitle">Módulo de atención clínica ambulatoria en consultorio</div>
                    </div>
                </div>

                <div class="med-atencion-card" style="text-align: center; padding: 60px 30px;">
                    <div style="width: 72px; height: 72px; border-radius: 50%; background: #f0f7ff; color: #0077c8; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 18px;">
                        <i data-lucide="user-x" style="width: 36px; height: 36px;"></i>
                    </div>
                    <h2 style="font-size: 20px; font-weight: 700; color: #0c2340; margin-bottom: 8px;">
                        No hay ningún paciente en atención en este momento
                    </h2>
                    <p style="font-size: 14px; color: #64748b; max-width: 520px; margin: 0 auto 24px auto; line-height: 1.5;">
                        Para iniciar una atención, diríjase a su lista de citas del turno o agenda y seleccione al paciente admitido en recepción mediante la opción <strong>«Ver ficha»</strong> y luego <strong>«Iniciar atención»</strong>.
                    </p>
                    <div style="display: flex; justify-content: center; gap: 12px;">
                        <a href="${pageContext.request.contextPath}/medico/inicio" class="med-btn-guardar" style="text-decoration: none;">
                            <i data-lucide="home"></i> Ir a Inicio
                        </a>
                        <a href="${pageContext.request.contextPath}/medico/agenda" class="med-btn-restablecer" style="text-decoration: none;">
                            <i data-lucide="calendar" style="width: 16px; height: 16px; display: inline; vertical-align: middle; margin-right: 4px;"></i> Ver mi agenda
                        </a>
                    </div>
                </div>

            <% } %>

        </main>
    </div>


    <script src="${pageContext.request.contextPath}/assets/js/medico-atencion.js"></script>
</body>
</html>
