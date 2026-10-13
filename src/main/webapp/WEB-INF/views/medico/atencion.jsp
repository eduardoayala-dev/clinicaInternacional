<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String especialidadMedico = "Medicina general";

    String inicialesMedico = "CM";
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
    String tipoCobertura = hayPacienteActivo && cita.get("tipoCobertura") != null ? (String) cita.get("tipoCobertura") : "EPS";
    String alergias = hayPacienteActivo && cita.get("alergias") != null ? (String) cita.get("alergias") : "Ninguna";
    
    String fechaHora = hayPacienteActivo && cita.get("fechaFormateada") != null ? (String) cita.get("fechaFormateada") : "";
    String sedeYConsultorio = hayPacienteActivo && cita.get("sedeYConsultorio") != null ? (String) cita.get("sedeYConsultorio") : "Sede San Borja · Consultorio 102";
    String medicoTratante = hayPacienteActivo && cita.get("medicoNombre") != null ? (String) cita.get("medicoNombre") : nombreMedico;
    String motivo = hayPacienteActivo && cita.get("motivo") != null ? (String) cita.get("motivo") : "Control general";
    String canalReserva = hayPacienteActivo && cita.get("canalReserva") != null ? (String) cita.get("canalReserva") : "Reserva web";
    String fechaRegistro = hayPacienteActivo && cita.get("fechaRegistro") != null ? (String) cita.get("fechaRegistro") : "";
    String estadoCita = hayPacienteActivo && cita.get("estado") != null ? (String) cita.get("estado") : "CONFIRMADA";
    String urgencia = hayPacienteActivo && cita.get("urgencia") != null ? (String) cita.get("urgencia") : "MEDIA";
    String fechaRecomendadaProxima = hayPacienteActivo && cita.get("fechaRecomendadaProxima") != null && !((String) cita.get("fechaRecomendadaProxima")).isEmpty() 
            ? (String) cita.get("fechaRecomendadaProxima") : "2027-01-15";
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Atención de paciente actual — Clínica Internacional</title>
    <!-- FUENTES: Inter para textos y Playfair Display para títulos institucionales -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">
    <!-- ICONOS LUCIDE -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <!-- ESTILOS UNIFICADOS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-medico.css">
</head>
<body>

    <!-- SIDEBAR INSTITUCIONAL -->
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
            <a href="${pageContext.request.contextPath}/medico/atencion<%= hayPacienteActivo ? ("?idCita=" + idCita) : "" %>" class="med-nav-item active">
                <i data-lucide="stethoscope"></i> Atención de paciente actual
            </a>
        </nav>

        <div class="med-user-footer">
            <a href="${pageContext.request.contextPath}/medico/perfil" class="med-user-info-link" title="Mi perfil">
                <div class="med-avatar"><%= inicialesMedico %></div>
                <div style="flex: 1; min-width: 0;">
                    <div class="med-user-name"><%= nombreMedico %></div>
                    <div class="med-user-role"><%= especialidadMedico %></div>
                </div>
            </a>
            <a href="${pageContext.request.contextPath}/logout" class="med-logout-btn" title="Cerrar sesión">
                <i data-lucide="log-out"></i>
            </a>
        </div>
    </aside>

    <!-- CONTENEDOR PRINCIPAL -->
    <div class="med-main-wrapper">
        <!-- TOPBAR CON BREADCRUMB -->
        <header class="med-topbar">
            <div class="med-breadcrumb">
                Médico &nbsp;/&nbsp; <span>Atención de paciente actual</span>
            </div>
        </header>

        <!-- ÁREA CON SCROLL -->
        <main class="med-content-scroll">

            <!-- MENSAJE DE ÉXITO TRAS GUARDAR -->
            <% if (mensajeExito != null) { %>
                <div style="background: #dcfce7; border: 1px solid #86efac; color: #166534; padding: 12px 18px; border-radius: 8px; margin-bottom: 20px; font-size: 13.5px; font-weight: 600; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="check-circle" style="width: 18px; height: 18px; color: #16a34a;"></i>
                    <%= mensajeExito %>
                </div>
            <% } %>

            <% if (hayPacienteActivo) { %>
                <!-- ========================================================
                     CASO A: HAY PACIENTE ACTIVO EN CONSULTORIO
                     ======================================================== -->

                <!-- MENSAJE OPERATIVO DEL SISTEMA -->
                <div class="med-atencion-banner">
                    <i data-lucide="info"></i>
                    <div class="med-atencion-banner-text">
                        <span class="med-atencion-banner-strong">Mensaje operativo del sistema</span><br>
                        Estado: paciente admitido en recepción · en consultorio
                    </div>
                </div>

                <!-- ENCABEZADO DE LA VISTA -->
                <div class="med-atencion-header">
                    <div>
                        <h1 class="med-atencion-title">Atención de paciente</h1>
                        <div class="med-atencion-subtitle">Detalle de la cita · <%= pacienteNombre %></div>
                    </div>
                    <div class="med-atencion-fecha-reg">
                        <% if (!fechaRegistro.isEmpty()) { %>
                            Fecha de registro: <%= fechaRegistro %>
                        <% } %>
                    </div>
                </div>

                <form id="formAtencion" action="${pageContext.request.contextPath}/medico/atencion" method="POST">
                    <input type="hidden" name="idCita" value="<%= idCita %>">
                    <input type="hidden" name="accion" id="accionInput" value="guardar">
                    <input type="hidden" name="prioridad" id="prioridadInput" value="<%= urgencia %>">

                    <!-- TARJETA 1: METADATOS CLÍNICOS VALIDADOS -->
                    <div class="med-atencion-card">
                        <div class="med-atencion-card-head">
                            <span class="med-atencion-tag-title">Metadatos clínicos validados</span>
                            <span class="med-badge-solo-lectura">Solo lectura</span>
                        </div>

                        <div class="med-meta-grid">
                            <!-- Columna Izquierda: Información del paciente -->
                            <div>
                                <div class="med-meta-col-title">Información del paciente</div>
                                <div class="med-meta-list">
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Nombre completo</span>
                                        <span class="med-meta-val"><%= pacienteNombre %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Documento / ficha</span>
                                        <span class="med-meta-val">DNI <%= dni %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Teléfono de contacto</span>
                                        <span class="med-meta-val"><%= telefono %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Tipo de cobertura</span>
                                        <span class="med-meta-val"><%= tipoCobertura %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Alergias registradas</span>
                                        <span class="med-meta-val">
                                            <% if ("Ninguna".equalsIgnoreCase(alergias) || alergias.trim().isEmpty()) { %>
                                                <span class="med-pill-alergia ninguna">Ninguna</span>
                                            <% } else { %>
                                                <span class="med-pill-alergia"><%= alergias %></span>
                                            <% } %>
                                        </span>
                                    </div>
                                </div>
                            </div>

                            <!-- Columna Derecha: Detalles de programación -->
                            <div>
                                <div class="med-meta-col-title">Detalles de programación</div>
                                <div class="med-meta-list">
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Fecha y hora</span>
                                        <span class="med-meta-val"><%= fechaHora %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Sede y consultorio</span>
                                        <span class="med-meta-val"><%= sedeYConsultorio %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Médico tratante</span>
                                        <span class="med-meta-val"><%= medicoTratante %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Motivo de consulta</span>
                                        <span class="med-meta-val"><%= motivo %></span>
                                    </div>
                                    <div class="med-meta-row">
                                        <span class="med-meta-label">Canal de reserva</span>
                                        <span class="med-meta-val"><%= canalReserva %></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TARJETA 2: CONTROL OPERATIVO REQUERIDO -->
                    <div class="med-atencion-card">
                        <div class="med-atencion-tag-title" style="margin-bottom: 14px;">Control operativo requerido</div>

                        <div class="med-aviso-box">
                            <strong>Aviso clínico-administrativo:</strong> Ninguno registrado
                        </div>

                        <div class="med-control-action-bar">
                            <button type="button" class="med-btn-no-presento" onclick="marcarNoSePresento()">
                                No se presentó al llamado
                            </button>

                            <div class="med-estado-recepcion-card">
                                <i data-lucide="edit-3"></i>
                                <div>
                                    <div class="med-estado-recepcion-title">Estado actual: En sala · admitido por recepción a tiempo</div>
                                    <div class="med-estado-recepcion-desc">Llegada registrada en recepción · la atención se cierra al guardar el registro</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TARJETA 3: PLAN DE SEGUIMIENTO -->
                    <div class="med-atencion-card">
                        <div class="med-atencion-tag-title" style="margin-bottom: 14px;">Plan de seguimiento</div>

                        <label class="med-field-label">
                            Nivel de prioridad clínica <span class="med-field-required">*</span>
                        </label>

                        <div class="med-prioridad-cards">
                            <!-- Alta -->
                            <div class="med-prio-card <%= "ALTA".equalsIgnoreCase(urgencia) ? "active" : "" %>" onclick="setPrioridad('ALTA', this)">
                                <div class="med-prio-card-left">
                                    <span class="med-prio-dot alta"></span>
                                    <div>
                                        <div class="med-prio-title">Alta</div>
                                        <div class="med-prio-sub">Requiere atención pronta</div>
                                    </div>
                                </div>
                                <div class="med-prio-check"><i data-lucide="check"></i></div>
                            </div>

                            <!-- Media -->
                            <div class="med-prio-card <%= !"ALTA".equalsIgnoreCase(urgencia) && !"BAJA".equalsIgnoreCase(urgencia) ? "active" : "" %>" onclick="setPrioridad('MEDIA', this)">
                                <div class="med-prio-card-left">
                                    <span class="med-prio-dot media"></span>
                                    <div>
                                        <div class="med-prio-title">Media</div>
                                        <div class="med-prio-sub">Seguimiento regular</div>
                                    </div>
                                </div>
                                <div class="med-prio-check"><i data-lucide="check"></i></div>
                            </div>

                            <!-- Baja -->
                            <div class="med-prio-card <%= "BAJA".equalsIgnoreCase(urgencia) ? "active" : "" %>" onclick="setPrioridad('BAJA', this)">
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

                        <!-- Fecha recomendada de próxima atención -->
                        <label class="med-field-label">
                            Fecha recomendada de próxima atención <span class="med-field-required">*</span>
                        </label>
                        <div class="med-date-group">
                            <div class="med-input-date-wrap">
                                <input type="date" name="fechaProxima" id="fechaProximaInput" class="med-input-date" value="<%= fechaRecomendadaProxima %>" onchange="actualizarBadgeMeses()">
                            </div>
                            <span class="med-badge-meses" id="badgeMeses">En 3 meses</span>
                        </div>

                        <!-- Indicación breve de continuidad -->
                        <div class="med-textarea-header">
                            <label class="med-field-label" style="margin-bottom: 0;">
                                Indicación breve de continuidad (texto libre)
                            </label>
                            <span class="med-char-counter" id="charCounter">42 / 200 caracteres</span>
                        </div>

                        <textarea name="indicaciones" id="indicacionesTextarea" class="med-textarea-atencion" maxlength="200" oninput="actualizarContador(this)">Control en 3 meses para evaluar evolución.</textarea>

                        <div class="med-disclaimer">
                            <i data-lucide="info"></i>
                            <span>Nota: esta indicación orienta la próxima cita. No reemplaza ni constituye una historia clínica.</span>
                        </div>
                    </div>

                    <!-- BARRA INFERIOR DE ACCIONES -->
                    <div class="med-bottom-actions">
                        <button type="button" class="med-btn-restablecer" onclick="restablecerFormulario()">
                            Restablecer
                        </button>
                        <button type="submit" class="med-btn-guardar">
                            <i data-lucide="save"></i>
                            Guardar registro de atención
                        </button>
                    </div>
                </form>

            <% } else { %>
                <!-- ========================================================
                     CASO B: NO HAY PACIENTE EN CONSULTORIO
                     ======================================================== -->
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
                        Para iniciar una atención, diríjase a su lista de citas del turno o agenda y seleccione al paciente admitido en recepción mediante la opción <strong>«Atender ahora»</strong>.
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

    <!-- SCRIPTS INTERACTIVOS -->
    <script>
        lucide.createIcons();

        function setPrioridad(valor, elemento) {
            document.querySelectorAll('.med-prio-card').forEach(c => c.classList.remove('active'));
            elemento.classList.add('active');
            document.getElementById('prioridadInput').value = valor;
        }

        function marcarNoSePresento() {
            if (confirm('¿Confirmar que el paciente no se presentó al llamado en el consultorio?')) {
                document.getElementById('accionInput').value = 'no_asistio';
                document.getElementById('formAtencion').submit();
            }
        }

        function actualizarContador(textarea) {
            const count = textarea.value.length;
            document.getElementById('charCounter').textContent = count + ' / 200 caracteres';
        }

        function actualizarBadgeMeses() {
            const el = document.getElementById('fechaProximaInput');
            if (!el) return;
            const fechaInput = el.value;
            if (!fechaInput) {
                document.getElementById('badgeMeses').textContent = '--';
                return;
            }
            const fechaSel = new Date(fechaInput);
            const hoy = new Date();
            const diffMeses = Math.round((fechaSel - hoy) / (1000 * 60 * 60 * 24 * 30));
            if (diffMeses <= 0) {
                document.getElementById('badgeMeses').textContent = 'Próximos días';
            } else if (diffMeses === 1) {
                document.getElementById('badgeMeses').textContent = 'En 1 mes';
            } else {
                document.getElementById('badgeMeses').textContent = 'En ' + diffMeses + ' meses';
            }
        }

        function restablecerFormulario() {
            const txt = document.getElementById('indicacionesTextarea');
            const fInput = document.getElementById('fechaProximaInput');
            if (txt) {
                txt.value = 'Control en 3 meses para evaluar evolución.';
                actualizarContador(txt);
            }
            if (fInput) {
                fInput.value = '<%= fechaRecomendadaProxima %>';
                actualizarBadgeMeses();
            }
        }

        // Inicializar badges y contador
        document.addEventListener('DOMContentLoaded', () => {
            const textarea = document.getElementById('indicacionesTextarea');
            if (textarea) actualizarContador(textarea);
            actualizarBadgeMeses();
        });
    </script>
</body>
</html>
