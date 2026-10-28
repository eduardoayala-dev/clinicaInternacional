<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombreCompleto();
    }
    if (nombrePaciente == null) {
        nombrePaciente = "Paciente";
    }

    String primerNombre = "Paciente";
    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        primerNombre = partes[0];
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    Integer numNotifSinLeer = (Integer) request.getAttribute("numNotifSinLeer");
    if (numNotifSinLeer == null) numNotifSinLeer = 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <%@ include file="/WEB-INF/jspf/csrf-head.jspf" %>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lista de Espera - Portal del Paciente | Clínica Internacional</title>

    <!-- Google Fonts: Inter & Playfair Display -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:ital,wght@0,600;0,700;1,600&display=swap" rel="stylesheet">

    <!-- Lucide Icons -->
    <script src="https://unpkg.com/lucide@latest"></script>

    <!-- Estilos Base y Específicos -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/lista-espera.css">
</head>
<body>

    <!-- SIDEBAR INSTITUCIONAL -->
    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clínica Internacional" class="brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div class="brand-text">
                <span class="brand-title">Clínica Internacional</span>
                <span class="brand-subtitle">Portal del paciente</span>
            </div>
        </div>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="home" style="width: 18px; height: 18px;"></i>
                <span>Inicio</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="search" style="width: 18px; height: 18px;"></i>
                <span>Buscar cita</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item">
                <i data-lucide="calendar" style="width: 18px; height: 18px;"></i>
                <span>Mis citas</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="nav-item active">
                <i data-lucide="clock" style="width: 18px; height: 18px;"></i>
                <span>Lista de espera</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
                <i data-lucide="bell" style="width: 18px; height: 18px;"></i>
                <span>Notificaciones</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px; height: 18px;"></i>
                <span>Perfil</span>
            </a>
        </nav>

        <div class="user-profile">
            <div class="avatar"><%= inicialesPaciente %></div>
            <div class="user-info">
                <span class="user-name"><%= nombrePaciente %></span>
                <span class="user-role">Paciente</span>
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/logout" style="margin:0;display:inline;">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
<button type="submit" class="btn-logout" title="Cerrar sesión" style="border:0;cursor:pointer;font-family:inherit;">
                <i data-lucide="log-out" style="width: 18px; height: 18px;"></i>
            </button>
</form>
        </div>
    </aside>

    <!-- ÁREA PRINCIPAL -->
    <main class="main-content" style="flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #f8fafc;">

        <!-- BARRA SUPERIOR -->
        <header class="dash-top-header">
            <div class="breadcrumb-trail">
                <span>Paciente</span>
                <span class="bc-sep">/</span>
                <strong>Lista de espera</strong>
            </div>

            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-avisos-pill">
                <i data-lucide="bell" style="width: 16px; height: 16px; color: #64748b;"></i>
                <span>Avisos</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-avisos-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
        </header>

        <!-- CONTENEDOR CON SCROLL -->
        <div class="espera-scroll-container">
            <c:if test="${not empty toastMsg}">
                <p role="alert"><c:out value="${toastMsg}"/></p>
            </c:if>

            <!-- ENCABEZADO -->
            <div class="page-title-section">
                <h1 class="espera-title">Lista de espera</h1>
                <p class="espera-subtitle">Registrate para recibir la oferta de un cupo liberado</p>
            </div>

            <!-- GRID PRINCIPAL (FORMULARIO A LA IZQUIERDA, WIDGETS A LA DERECHA) -->
            <div class="espera-layout-grid">

                <!-- COLUMNA IZQUIERDA: FORMULARIO DE INSCRIPCIÓN -->
                <div class="card-formulario-espera">
                    <h2 class="form-card-title">Formulario de inscripción</h2>

                    <form id="formInscripcionEspera" method="POST" action="${pageContext.request.contextPath}/paciente/lista-espera">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="inscribir">

                        <!-- FILA 1: ESPECIALIDAD Y MÉDICO PREFERIDO -->
                        <div class="form-two-cols">
                            <div class="form-group">
                                <label for="selectEspecialidadEspera" class="field-label">1. Especialidad requerida *</label>
                                <div class="select-field-wrap">
                                    <select id="selectEspecialidadEspera" name="especialidadId" class="espera-select-control" required onchange="filtrarMedicosPorEspecialidad()">
                                        <c:forEach var="esp" items="${listaEspecialidades}">
                                            <option value="${esp.id}" ${esp.nombre == 'Dermatología' ? 'selected' : ''}>
                                                <c:out value="${esp.nombre}" />
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <i data-lucide="chevron-down" class="select-chevron-icon"></i>
                                </div>
                            </div>

                            <div class="form-group">
                                <label for="selectMedicoEspera" class="field-label">2. Médico preferido (opcional)</label>
                                <div class="select-field-wrap">
                                    <select id="selectMedicoEspera" name="medicoId" class="espera-select-control">
                                        <option value="">Cualquier especialista disponible</option>
                                        <c:forEach var="med" items="${listaMedicos}">
                                            <option value="${med.idMedico}" data-esp="${med.idEspecialidad}" data-sede="${med.idSede}">
                                                <c:out value="${med.nombre}" />
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <i data-lucide="chevron-down" class="select-chevron-icon"></i>
                                </div>
                            </div>
                        </div>

                        <!-- FILA 2: SEDE PREFERIDA -->
                        <div class="form-group" style="margin-top: 20px;">
                            <label class="field-label">3. Sede preferida *</label>
                            <input type="hidden" name="sede" id="inputSedeEspera" value="cualquiera">
                            <div class="pills-group-sedes">
                                <button type="button" class="btn-pill-sede active" data-sede="cualquiera" onclick="seleccionarSede(this, 'cualquiera')">
                                    <span>Cualquier sede</span>
                                </button>
                                <c:forEach var="sede" items="${listaSedes}">
                                    <button type="button" class="btn-pill-sede" data-sede="${sede.id}" onclick="seleccionarSede(this, this.dataset.sede)">
                                        <span><c:out value="${sede.nombre}"/></span>
                                    </button>
                                </c:forEach>
                            </div>
                        </div>

                        <!-- FILA 3: PREFERENCIA DE HORARIO Y DISPONIBILIDAD ANTE CANCELACIONES -->
                        <div class="form-two-cols" style="margin-top: 20px;">
                            
                            <!-- HORARIOS -->
                            <div class="form-group">
                                <label class="field-label">4. Preferencia de horario * (una o más)</label>
                                <div class="checkboxes-row-group">
                                    <label class="horario-card active" id="labelHorarioManana">
                                        <input type="checkbox" name="horarios" value="manana" checked onchange="toggleCheckboxCard(this, 'labelHorarioManana')">
                                        <span class="custom-checkbox">
                                            <i data-lucide="check" style="width: 13px; height: 13px;"></i>
                                        </span>
                                        <span class="card-item-label">Mañana</span>
                                    </label>

                                    <label class="horario-card" id="labelHorarioTarde">
                                        <input type="checkbox" name="horarios" value="tarde" onchange="toggleCheckboxCard(this, 'labelHorarioTarde')">
                                        <span class="custom-checkbox">
                                            <i data-lucide="check" style="width: 13px; height: 13px;"></i>
                                        </span>
                                        <span class="card-item-label">Tarde</span>
                                    </label>
                                </div>
                            </div>

                            <!-- DISPONIBILIDAD ANTE CANCELACIONES -->
                            <div class="form-group">
                                <label class="field-label">5. Disponibilidad ante cancelaciones inmediatas *</label>
                                <div class="radios-col-group">
                                    <label class="disponibilidad-card" id="labelDispPrevia">
                                        <input type="radio" name="disponibilidad" value="12h" onchange="actualizarRadiosDisponibilidad()">
                                        <span class="custom-radio"></span>
                                        <span class="card-item-desc">Requiere aviso previo (mínimo 12 horas de anticipación)</span>
                                    </label>

                                    <label class="disponibilidad-card active" id="labelDispFlexible">
                                        <input type="radio" name="disponibilidad" value="3h" checked onchange="actualizarRadiosDisponibilidad()">
                                        <span class="custom-radio"></span>
                                        <span class="card-item-desc">Disponibilidad flexible (puedo acudir el mismo día con al menos 3 horas de aviso)</span>
                                    </label>
                                </div>
                            </div>

                        </div>

                        <!-- FILA 4: CANAL DE NOTIFICACIÓN -->
                        <div class="form-group" style="margin-top: 20px;">
                            <span class="field-label" id="preferenciaContacto">6. Preferencia de contacto *</span>
                            <p>Las ofertas se notifican en este portal. Registramos WhatsApp y correo como preferencias de contacto.</p>
                            <div class="checkboxes-row-group" role="group" aria-labelledby="preferenciaContacto">
                                <label class="canal-card active" id="labelCanalWhatsapp">
                                    <input type="checkbox" name="canales" value="whatsapp" checked onchange="toggleCheckboxCard(this, 'labelCanalWhatsapp')">
                                    <span class="custom-checkbox">
                                        <i data-lucide="check" style="width: 13px; height: 13px;"></i>
                                    </span>
                                    <i data-lucide="smartphone" class="canal-device-icon"></i>
                                    <span class="card-item-label">SMS / WhatsApp</span>
                                </label>

                                <label class="canal-card" id="labelCanalEmail">
                                    <input type="checkbox" name="canales" value="email" onchange="toggleCheckboxCard(this, 'labelCanalEmail')">
                                    <span class="custom-checkbox">
                                        <i data-lucide="check" style="width: 13px; height: 13px;"></i>
                                    </span>
                                    <i data-lucide="mail" class="canal-device-icon"></i>
                                    <span class="card-item-label">Correo electrónico</span>
                                </label>
                            </div>
                        </div>

                        <!-- FILA DE BOTONES DE ACCIÓN -->
                        <div class="form-actions-bottom">
                            <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-cancelar-espera">
                                <i data-lucide="chevron-left" style="width: 17px; height: 17px;"></i>
                                <span>Cancelar y volver a búsqueda</span>
                            </a>

                            <button type="submit" class="btn-submit-espera">
                                <span>Registrarme en lista de espera</span>
                                <i data-lucide="check" style="width: 18px; height: 18px;"></i>
                            </button>
                        </div>

                    </form>
                </div>

                <!-- COLUMNA DERECHA: CÓMO FUNCIONA Y REGISTROS ACTIVOS -->
                <div class="espera-sidebar-widgets">

                    <!-- CARD 1: CÓMO FUNCIONA EL CUPO -->
                    <div class="widget-card">
                        <h3 class="widget-card-title">Cómo funciona el cupo</h3>
                        
                        <div class="pasos-como-funciona">
                            <div class="paso-item">
                                <div class="paso-num-circle">1</div>
                                <div class="paso-text">Si se libera un cupo que coincide con tus preferencias, el sistema te lo ofrece.</div>
                            </div>
                            <div class="paso-item">
                                <div class="paso-num-circle">2</div>
                                <div class="paso-text">Tienes un tiempo límite para aceptarlo o rechazarlo.</div>
                            </div>
                            <div class="paso-item">
                                <div class="paso-num-circle">3</div>
                                <div class="paso-text">Si lo aceptas, la cita queda reservada y sales de la lista de espera.</div>
                            </div>
                        </div>
                    </div>

                    <!-- CARD 2: ESTADO DE REGISTROS ACTIVOS -->
                    <div class="widget-card">
                        <div class="widget-header-row">
                            <h3 class="widget-card-title" style="margin-bottom: 0;">Estado de registros activos</h3>
                            <span class="badge-activo-pill">
                                <c:choose>
                                    <c:when test="${not empty estadoListaEspera}">1 activo</c:when>
                                    <c:otherwise>0 activos</c:otherwise>
                                </c:choose>
                            </span>
                        </div>

                        <c:choose>
                            <c:when test="${not empty estadoListaEspera}">
                                <div class="detalles-activo-list">
                                    <div class="detalle-activo-row">
                                        <span class="detalle-activo-label">Especialidad</span>
                                        <strong class="detalle-activo-val"><c:out value="${estadoListaEspera.especialidad}" /></strong>
                                    </div>
                                    <div class="detalle-activo-row">
                                        <span class="detalle-activo-label">Sede</span>
                                        <strong class="detalle-activo-val"><c:out value="${estadoListaEspera.sede}" /></strong>
                                    </div>
                                    <div class="detalle-activo-row">
                                        <span class="detalle-activo-label">Preferencias</span>
                                        <strong class="detalle-activo-val"><c:out value="${estadoListaEspera.preferencias}" /></strong>
                                    </div>
                                    <div class="detalle-activo-row" style="align-items: center;">
                                        <span class="detalle-activo-label">Estado</span>
                                        <span class="badge-status-oferta ${estadoListaEspera.estadoCodigo == 'OFERTADA' ? 'pendiente' : 'activa'}">
                                            <c:out value="${estadoListaEspera.estado}" />
                                        </span>
                                    </div>
                                </div>

                                <form method="POST" action="${pageContext.request.contextPath}/paciente/lista-espera" style="margin-top: 18px;" onsubmit="return confirm('¿Estás seguro de que deseas retirarte de la lista de espera?');">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="retirar">
                                    <input type="hidden" name="idListaEspera" value="${estadoListaEspera.idListaEspera}">
                                    <button type="submit" class="btn-retirarme-outline">Retirarme de la lista de espera</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <div class="empty-activo-state">
                                    <i data-lucide="clock" style="width: 32px; height: 32px; color: #94a3b8; stroke-width: 1.5; margin-bottom: 8px;"></i>
                                    <p class="empty-activo-text">No tienes solicitudes activas en la lista de espera.</p>
                                    <span class="empty-activo-hint">Completa el formulario para inscribirte.</span>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>

                </div>

            </div>

        </div> <!-- /.espera-scroll-container -->

    </main>

    <!-- SCRIPTS INTERACTIVOS -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            lucide.createIcons();
            filtrarMedicosPorEspecialidad();
        });

        // 1. Manejo de píldoras de Sede
        function seleccionarSede(btn, valor) {
            document.querySelectorAll('.btn-pill-sede').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('inputSedeEspera').value = valor;
            filtrarMedicosPorEspecialidad();
        }

        // 2. Toggle de cards tipo checkbox (horarios y canales)
        function toggleCheckboxCard(chk, labelId) {
            const card = document.getElementById(labelId);
            if (chk.checked) {
                card.classList.add('active');
            } else {
                card.classList.remove('active');
            }
        }

        // 3. Manejo de radio de disponibilidad
        function actualizarRadiosDisponibilidad() {
            const radioFlex = document.querySelector('input[name="disponibilidad"][value="3h"]');
            const cardPrevia = document.getElementById('labelDispPrevia');
            const cardFlexible = document.getElementById('labelDispFlexible');

            if (radioFlex && radioFlex.checked) {
                cardFlexible.classList.add('active');
                cardPrevia.classList.remove('active');
            } else {
                cardPrevia.classList.add('active');
                cardFlexible.classList.remove('active');
            }
        }

        // 4. Filtrar médicos disponibles según especialidad seleccionada
        function filtrarMedicosPorEspecialidad() {
            const selEsp = document.getElementById('selectEspecialidadEspera');
            const selMed = document.getElementById('selectMedicoEspera');
            if (!selEsp || !selMed) return;

            const espId = selEsp.value;
            const sede = document.getElementById('inputSedeEspera').value;
            const options = selMed.querySelectorAll('option');

            options.forEach(opt => {
                const compatible = !opt.value || (opt.dataset.esp === espId && (sede === 'cualquiera' || opt.dataset.sede === sede));
                opt.hidden = !compatible;
                opt.disabled = !compatible;
            });

            const currentSelected = selMed.options[selMed.selectedIndex];
            if (currentSelected && currentSelected.disabled) {
                selMed.value = "";
            }
        }
    </script>
</body>
</html>
