<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Locale" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePersonal = (String) request.getAttribute("nombrePersonal");
    if (nombrePersonal == null && usuario != null) {
        nombrePersonal = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePersonal == null || nombrePersonal.trim().isEmpty()) {
        nombrePersonal = "Lucía Ramírez";
    }

    String inicialesPersonal = "LR";
    if (nombrePersonal != null && !nombrePersonal.trim().isEmpty()) {
        String cleanNom = nombrePersonal.replaceAll("(?i)\\b(Lic\\.|Ing\\.|Sr\\.|Sra\\.)\\b", "").trim();
        String[] partes = cleanNom.split("\\s+");
        if (partes.length >= 2) {
            inicialesPersonal = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else if (partes.length == 1 && !partes[0].isEmpty()) {
            inicialesPersonal = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    List<Map<String, Object>> listaCitas = (List<Map<String, Object>>) request.getAttribute("listaCitas");
    List<Map<String, Object>> sedes = (List<Map<String, Object>>) request.getAttribute("sedes");
    List<Map<String, String>> ultimasOperaciones = (List<Map<String, String>>) request.getAttribute("ultimasOperaciones");

    Integer totalCitas = (Integer) request.getAttribute("totalCitas");
    if (totalCitas == null) totalCitas = 126;

    Integer cancelaciones = (Integer) request.getAttribute("cancelaciones");
    if (cancelaciones == null) cancelaciones = 38;

    Integer cuposLibres = (Integer) request.getAttribute("cuposLibres");
    if (cuposLibres == null) cuposLibres = 74;

    Integer reasignacionesPendientes = (Integer) request.getAttribute("reasignacionesPendientes");
    if (reasignacionesPendientes == null) reasignacionesPendientes = 12;

    String flashMessage = (String) session.getAttribute("flashMessage");
    if (flashMessage != null) {
        session.removeAttribute("flashMessage");
    }

    String flashError = (String) session.getAttribute("flashError");
    if (flashError != null) {
        session.removeAttribute("flashError");
    }

    LocalDateTime ahora = LocalDateTime.now();
    DateTimeFormatter dtfDia = DateTimeFormatter.ofPattern("EEE dd MMM yyyy", new Locale("es", "PE"));
    DateTimeFormatter dtfHora = DateTimeFormatter.ofPattern("HH:mm");
    String fechaHoyFormateada = ahora.format(dtfDia);
    fechaHoyFormateada = fechaHoyFormateada.substring(0, 1).toUpperCase() + fechaHoyFormateada.substring(1);
    String horaUltima = ahora.format(dtfHora);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tablero de Control Operativo Diario — Clínica Internacional</title>
    <!-- TIPOGRAFÍAS INSTITUCIONALES: Inter + Playfair Display -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:ital,wght@0,600;0,700;0,800;1,600&display=swap" rel="stylesheet">
    <!-- ICONOS LUCIDE -->
    <script src="https://unpkg.com/lucide@latest"></script>
    <!-- ESTILOS INSTITUCIONALES DEL PORTAL Y DEL TABLERO -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/portal-administrativo.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-tablero.css">
</head>
<body>

    <!-- ==========================================================================
         PANEL LATERAL INSTITUCIONAL (PORTAL ADMINISTRATIVO)
         ========================================================================== -->
    <aside class="adm-sidebar">
        <a href="${pageContext.request.contextPath}/admin/dashboard" class="adm-brand">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clínica Internacional" class="adm-brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div>
                <div class="adm-brand-name">Clínica Internacional</div>
                <div class="adm-brand-sub">Portal administrativo</div>
            </div>
        </a>

        <nav class="adm-nav">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="adm-nav-item active">
                <i data-lucide="home"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/admin/medicos" class="adm-nav-item">
                <i data-lucide="stethoscope"></i> Médicos y especialidades
            </a>
            <a href="${pageContext.request.contextPath}/admin/pacientes" class="adm-nav-item">
                <i data-lucide="users"></i> Pacientes
            </a>
            <a href="${pageContext.request.contextPath}/admin/reasignaciones" class="adm-nav-item">
                <i data-lucide="refresh-cw"></i> Reasignaciones y excepciones
            </a>
            <a href="${pageContext.request.contextPath}/admin/dashboard-operativo" class="adm-nav-item">
                <i data-lucide="trending-up"></i> Dashboard operativo
            </a>
        </nav>

        <div class="adm-user-footer">
            <div class="adm-user-info-link">
                <div class="adm-avatar"><%= inicialesPersonal %></div>
                <div>
                    <div class="adm-user-name"><%= nombrePersonal %></div>
                    <div class="adm-user-role">Personal administrativo</div>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/logout" class="adm-logout-btn" title="Cerrar sesión">
                <i data-lucide="log-out"></i>
            </a>
        </div>
    </aside>

    <!-- ==========================================================================
         ÁREA DE CONTENIDO Y TOPBAR
         ========================================================================== -->
    <div class="adm-main-wrapper">
        <!-- TOPBAR CON BREADCRUMB -->
        <header class="adm-topbar">
            <div class="adm-breadcrumb">
                Personal administrativo / <span>Inicio</span>
            </div>
        </header>

        <!-- CONTENEDOR CON SCROLL -->
        <main class="adm-content-scroll">

            <!-- MENSAJES FLASH -->
            <% if (flashMessage != null) { %>
                <div style="background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; padding: 12px 18px; border-radius: 10px; font-weight: 600; margin-bottom: 20px; font-size: 13.5px; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="check-circle" style="width: 18px; color: #16a34a;"></i>
                    <span><%= flashMessage %></span>
                </div>
            <% } %>

            <% if (flashError != null) { %>
                <div style="background: #fee2e2; color: #991b1b; border: 1px solid #fecaca; padding: 12px 18px; border-radius: 10px; font-weight: 600; margin-bottom: 20px; font-size: 13.5px; display: flex; align-items: center; gap: 10px;">
                    <i data-lucide="alert-circle" style="width: 18px; color: #dc2626;"></i>
                    <span><%= flashError %></span>
                </div>
            <% } %>

            <!-- 1. CABECERA DEL TABLERO Y BOTONES DE ACCIÓN -->
            <div class="tablero-header-row">
                <div class="tablero-title-block">
                    <h1 class="tablero-page-title">Tablero de control operativo diario</h1>
                    <p class="tablero-page-subtitle">
                        <%= fechaHoyFormateada %> · Sede San Borja · Se actualiza cada 5 min · Última: <%= horaUltima %>
                    </p>
                </div>

                <div class="tablero-actions-group">
                    <button type="button" class="tablero-btn-outline" onclick="exportarExcel()">
                        <i data-lucide="download"></i>
                        <span>Exportar Excel</span>
                    </button>
                    <button type="button" class="tablero-btn-outline" onclick="exportarCSV()">
                        <i data-lucide="download"></i>
                        <span>Exportar CSV</span>
                    </button>
                    <button type="button" class="tablero-btn-primary" onclick="actualizarAhora()">
                        <i data-lucide="refresh-cw"></i>
                        <span>Actualizar ahora</span>
                    </button>
                </div>
            </div>

            <!-- 2. TARJETAS DE MÉTRICAS (4 COLUMNAS) -->
            <div class="tablero-metrics-grid">
                <!-- Citas del día -->
                <div class="tablero-stat-card">
                    <div class="tablero-stat-top">
                        <span class="tablero-stat-label">Citas del día</span>
                        <div class="tablero-stat-icon-wrap">
                            <i data-lucide="calendar"></i>
                        </div>
                    </div>
                    <div class="tablero-stat-number"><%= totalCitas %></div>
                    <div class="tablero-stat-subtext">En las sedes de la red</div>
                </div>

                <!-- Cancelaciones registradas -->
                <div class="tablero-stat-card">
                    <div class="tablero-stat-top">
                        <span class="tablero-stat-label">Cancelaciones registradas</span>
                        <div class="tablero-stat-icon-wrap">
                            <i data-lucide="x"></i>
                        </div>
                    </div>
                    <div class="tablero-stat-number"><%= cancelaciones %></div>
                    <div class="tablero-stat-subtext">Hoy, en la red</div>
                </div>

                <!-- Cupos disponibles en red -->
                <div class="tablero-stat-card">
                    <div class="tablero-stat-top">
                        <span class="tablero-stat-label">Cupos disponibles en red</span>
                        <div class="tablero-stat-icon-wrap">
                            <i data-lucide="calendar"></i>
                        </div>
                    </div>
                    <div class="tablero-stat-number"><%= cuposLibres %></div>
                    <div class="tablero-stat-subtext">Para los próximos 7 días</div>
                </div>

                <!-- Reasignaciones pendientes -->
                <div class="tablero-stat-card">
                    <div class="tablero-stat-top">
                        <span class="tablero-stat-label">Reasignaciones pendientes</span>
                        <div class="tablero-stat-icon-wrap">
                            <i data-lucide="refresh-cw"></i>
                        </div>
                    </div>
                    <div class="tablero-stat-number"><%= reasignacionesPendientes %></div>
                    <span class="badge-ofertas-curso">Ofertas en curso</span>
                </div>
            </div>

            <!-- 3. SECCIÓN PRINCIPAL: CITAS DEL DÍA EN SALA DE ESPERA -->
            <div class="tablero-main-card">
                <div class="tablero-card-head">
                    <h2 class="tablero-card-title">Citas del día en sala de espera</h2>

                    <div class="tablero-filters-bar">
                        <!-- Buscador en tiempo real -->
                        <div class="tablero-search-wrap">
                            <input type="text" id="filtroTexto" class="tablero-search-input" placeholder="Buscar paciente, DNI o médico" oninput="filtrarCitas()">
                            <i data-lucide="search" class="tablero-search-icon"></i>
                        </div>

                        <!-- Filtro Sedes -->
                        <select id="filtroSede" class="tablero-select" onchange="filtrarCitas()">
                            <option value="">Todas las sedes</option>
                            <% if (sedes != null) {
                                for (Map<String, Object> s : sedes) { %>
                                    <option value="<%= s.get("nombre") %>"><%= s.get("nombre") %></option>
                            <%  }
                               } else { %>
                                <option value="Sede San Borja">Sede San Borja</option>
                                <option value="Sede Lima">Sede Lima</option>
                                <option value="Sede La Molina">Sede La Molina</option>
                            <% } %>
                        </select>

                        <!-- Filtro Estado -->
                        <select id="filtroEstado" class="tablero-select" onchange="filtrarCitas()">
                            <option value="">Filtro: en sala</option>
                            <option value="En sala">En sala</option>
                            <option value="Llegó tarde">Llegó tarde</option>
                            <option value="Confirmada - Web">Confirmada - Web</option>
                            <option value="Sin confirmar">Sin confirmar</option>
                        </select>
                    </div>
                </div>

                <!-- Tabla de Citas -->
                <div class="tablero-table-wrap">
                    <table class="tablero-table" id="tablaCitas">
                        <thead>
                            <tr>
                                <th>HORA</th>
                                <th>PACIENTE</th>
                                <th>SEDE / CONSULTORIO</th>
                                <th>MÉDICO</th>
                                <th>ESTADO</th>
                                <th>ACCIÓN</th>
                            </tr>
                        </thead>
                        <tbody id="tbodyCitas">
                            <% if (listaCitas != null && !listaCitas.isEmpty()) { 
                                for (Map<String, Object> c : listaCitas) { 
                                    String hora = (String) c.get("hora");
                                    String pacienteNom = (String) c.get("pacienteNombre");
                                    String dni = (String) c.get("pacienteDni");
                                    String tel = (String) c.get("pacienteTelefono");
                                    String sedeNom = (String) c.get("sedeNombre");
                                    String consultorio = (String) c.get("consultorio");
                                    String medicoNom = (String) c.get("medicoNombre");
                                    String estado = (String) c.get("estado");
                                    String accionTexto = (String) c.get("accionTexto");
                                    Object idCitaObj = c.get("idCita");
                                    int idCita = idCitaObj != null ? Integer.parseInt(idCitaObj.toString()) : 0;

                                    String pillClass = "pill-ensala";
                                    if ("Llegó tarde".equalsIgnoreCase(estado)) pillClass = "pill-tarde";
                                    else if ("Confirmada - Web".equalsIgnoreCase(estado) || "Confirmada".equalsIgnoreCase(estado)) pillClass = "pill-confirmada";
                                    else if ("Sin confirmar".equalsIgnoreCase(estado) || "Cancelada".equalsIgnoreCase(estado)) pillClass = "pill-sinconfirmar";
                            %>
                            <tr class="cita-fila"
                                data-paciente="<%= pacienteNom != null ? pacienteNom.toLowerCase() : "" %>"
                                data-dni="<%= dni != null ? dni : "" %>"
                                data-medico="<%= medicoNom != null ? medicoNom.toLowerCase() : "" %>"
                                data-sede="<%= sedeNom != null ? sedeNom : "" %>"
                                data-estado="<%= estado != null ? estado : "" %>">
                                <td>
                                    <span class="tablero-td-hora"><%= hora %></span>
                                </td>
                                <td>
                                    <div class="tablero-paciente-box">
                                        <span class="tablero-paciente-name"><%= pacienteNom %></span>
                                        <span class="tablero-paciente-sub">DNI <%= dni %> · <%= tel %></span>
                                    </div>
                                </td>
                                <td>
                                    <div class="tablero-sede-box">
                                        <span class="tablero-sede-name"><%= sedeNom %></span>
                                        <span class="tablero-sede-cons"><%= consultorio %></span>
                                    </div>
                                </td>
                                <td>
                                    <span class="tablero-medico-name"><%= medicoNom %></span>
                                </td>
                                <td>
                                    <span class="pill-status <%= pillClass %>"><%= estado %></span>
                                </td>
                                <td>
                                    <% if (accionTexto != null && !accionTexto.trim().isEmpty()) { %>
                                        <span class="tablero-action-done"><%= accionTexto %></span>
                                    <% } else { %>
                                        <button type="button" class="btn-action-admitir" onclick="admitirCita(<%= idCita %>, '<%= pacienteNom %>')">Admitir</button>
                                        <button type="button" class="btn-action-cancelar" onclick="cancelarCita(<%= idCita %>, '<%= pacienteNom %>')">Cancelar</button>
                                    <% } %>
                                </td>
                            </tr>
                            <%  } 
                               } %>
                        </tbody>
                    </table>
                </div>

                <!-- Paginación de la Tabla -->
                <div class="tablero-footer-bar">
                    <span id="tableroPaginationInfo">Mostrando 4 de 18 citas en sala</span>
                    <div class="tablero-pagination-ctrls">
                        <button type="button" class="btn-tablero-page" id="btnPrevPage" onclick="cambiarPagina(-1)">&lt; Anterior</button>
                        <span id="tableroPageNum" style="font-weight: 500; color: #334155; padding: 0 4px;">Página 1 de 3</span>
                        <button type="button" class="btn-tablero-page btn-tablero-page-next" id="btnNextPage" onclick="cambiarPagina(1)">Siguiente &gt;</button>
                    </div>
                </div>
            </div>

            <!-- 4. GRILLA INFERIOR: ÚLTIMAS OPERACIONES + ACCIONES RÁPIDAS -->
            <div class="tablero-bottom-grid">
                <!-- Últimas Operaciones Registradas -->
                <div class="tablero-feed-card">
                    <div class="tablero-feed-head">
                        <h3 class="tablero-feed-title">Últimas operaciones registradas</h3>
                        <div class="tablero-live-badge">
                            <span class="tablero-live-dot"></span>
                            <span>En vivo</span>
                        </div>
                    </div>

                    <div class="tablero-feed-list" id="feedOperaciones">
                        <% if (ultimasOperaciones != null && !ultimasOperaciones.isEmpty()) { 
                            for (Map<String, String> op : ultimasOperaciones) { %>
                            <div class="tablero-feed-item">
                                <span class="tablero-feed-time"><%= op.get("hora") %></span>
                                <span><%= op.get("texto") %></span>
                            </div>
                        <%  } 
                           } else { %>
                            <div class="tablero-feed-item">
                                <span class="tablero-feed-time">08:53</span>
                                <span>Admisión: Rosa Díaz Paredes llegó tarde (cita 08:40) - Consultorio 102</span>
                            </div>
                            <div class="tablero-feed-item">
                                <span class="tablero-feed-time">08:47</span>
                                <span>Cancelación: cita de Cardiología de las 12:40 - cupo ofertado a la lista de espera</span>
                            </div>
                            <div class="tablero-feed-item">
                                <span class="tablero-feed-time">08:41</span>
                                <span>Confirmación: Raúl Chávez Pinto confirmó por WhatsApp (cita 12:00)</span>
                            </div>
                        <% } %>
                    </div>
                </div>

                <!-- Acciones Rápidas -->
                <div class="tablero-quick-card">
                    <div class="tablero-quick-head">
                        <i data-lucide="zap"></i>
                        <span>Acciones rápidas</span>
                    </div>

                    <button type="button" class="btn-quick-paciente" onclick="abrirModalRegistrarPaciente()">
                        <i data-lucide="user-plus"></i>
                        <span>+ Registrar nuevo paciente en padrón</span>
                    </button>
                </div>
            </div>

        </main>
    </div>

    <!-- ==========================================================================
         FORMULARIOS OCULTOS PARA ACCIONES OPERATIVAS DIRECTAS
         ========================================================================== -->
    <form id="formAdmitir" action="${pageContext.request.contextPath}/admin/dashboard" method="POST" style="display: none;">
        <input type="hidden" name="action" value="admitir_cita">
        <input type="hidden" name="idCita" id="formAdmitirIdCita" value="">
    </form>

    <form id="formCancelar" action="${pageContext.request.contextPath}/admin/dashboard" method="POST" style="display: none;">
        <input type="hidden" name="action" value="cancelar_cita">
        <input type="hidden" name="idCita" id="formCancelarIdCita" value="">
    </form>

    <!-- ==========================================================================
         MODAL: REGISTRO RÁPIDO DE PACIENTE EN PADRÓN
         ========================================================================== -->
    <div class="tablero-modal-overlay" id="modalRegistrarPaciente" onclick="cerrarModalFondo(event)">
        <div class="tablero-modal-card">
            <div class="tablero-modal-head">
                <h3 class="tablero-modal-title">Registrar nuevo paciente en padrón</h3>
                <button type="button" class="tablero-modal-close" onclick="cerrarModalRegistrarPaciente()">&times;</button>
            </div>

            <form action="${pageContext.request.contextPath}/admin/dashboard" method="POST" id="formNuevoPaciente">
                <input type="hidden" name="action" value="registrar_paciente">

                <div class="tablero-form-grid">
                    <div class="tablero-form-group">
                        <label for="paciNombres">Nombres</label>
                        <input type="text" id="paciNombres" name="nombres" class="tablero-form-input" required placeholder="Ej. Carlos">
                    </div>

                    <div class="tablero-form-group">
                        <label for="paciApellidos">Apellidos</label>
                        <input type="text" id="paciApellidos" name="apellidos" class="tablero-form-input" required placeholder="Ej. Mendoza">
                    </div>

                    <div class="tablero-form-group">
                        <label for="paciDni">DNI / Documento</label>
                        <input type="text" id="paciDni" name="dni" class="tablero-form-input" required maxlength="8" placeholder="8 dígitos">
                    </div>

                    <div class="tablero-form-group">
                        <label for="paciTelefono">Teléfono celular</label>
                        <input type="text" id="paciTelefono" name="telefono" class="tablero-form-input" required placeholder="987 654 321">
                    </div>

                    <div class="tablero-form-group full">
                        <label for="paciEmail">Correo electrónico (Opcional)</label>
                        <input type="email" id="paciEmail" name="email" class="tablero-form-input" placeholder="paciente@correo.com">
                    </div>

                    <div class="tablero-form-group full">
                        <label for="paciDireccion">Dirección</label>
                        <input type="text" id="paciDireccion" name="direccion" class="tablero-form-input" placeholder="Av. Javier Prado 1200">
                    </div>

                    <div class="tablero-form-group full">
                        <label for="paciSede">Sede de preferencia</label>
                        <select id="paciSede" name="sede_preferida" class="tablero-form-select">
                            <option value="Sede San Borja">Sede San Borja</option>
                            <option value="Sede Lima Norte">Sede Lima Norte</option>
                            <option value="Sede Central Garcilaso">Sede Central Garcilaso</option>
                            <option value="Sede Lima Principal">Sede Lima Principal</option>
                        </select>
                    </div>
                </div>

                <div class="tablero-modal-foot">
                    <button type="button" class="tablero-btn-outline" onclick="cerrarModalRegistrarPaciente()">Cancelar</button>
                    <button type="submit" class="tablero-btn-primary">Guardar en padrón</button>
                </div>
            </form>
        </div>
    </div>

    <!-- ==========================================================================
         LÓGICA JAVASCRIPT DEL TABLERO (FILTRADO EN VIVO, ACCIONES, EXPORTACIONES)
         ========================================================================== -->
    <script>
        let paginaActual = 1;
        const filasPorPagina = 4;

        document.addEventListener("DOMContentLoaded", function () {
            if (window.lucide) {
                lucide.createIcons();
            }
            filtrarCitas();
        });

        // Filtrado en vivo de citas en sala de espera
        function filtrarCitas() {
            const texto = document.getElementById("filtroTexto").value.toLowerCase().trim();
            const sede = document.getElementById("filtroSede").value;
            const estado = document.getElementById("filtroEstado").value;

            const filas = document.querySelectorAll("#tbodyCitas .cita-fila");
            let visibles = [];

            filas.forEach(fila => {
                const fPaciente = fila.getAttribute("data-paciente") || "";
                const fDni = fila.getAttribute("data-dni") || "";
                const fMedico = fila.getAttribute("data-medico") || "";
                const fSede = fila.getAttribute("data-sede") || "";
                const fEstado = fila.getAttribute("data-estado") || "";

                const matchTexto = !texto || fPaciente.includes(texto) || fDni.includes(texto) || fMedico.includes(texto);
                const matchSede = !sede || fSede === sede;
                const matchEstado = !estado || fEstado.toLowerCase().includes(estado.toLowerCase());

                if (matchTexto && matchSede && matchEstado) {
                    visibles.push(fila);
                } else {
                    fila.style.display = "none";
                }
            });

            // Paginación visual
            const totalVisibles = visibles.length;
            const totalPaginas = Math.max(1, Math.ceil(totalVisibles / filasPorPagina));
            if (paginaActual > totalPaginas) paginaActual = 1;

            const inicio = (paginaActual - 1) * filasPorPagina;
            const fin = inicio + filasPorPagina;

            visibles.forEach((fila, idx) => {
                if (idx >= inicio && idx < fin) {
                    fila.style.display = "";
                } else {
                    fila.style.display = "none";
                }
            });

            // Actualizar etiquetas
            const mostrandoHasta = Math.min(fin, totalVisibles);
            const totalCitasLabel = totalVisibles;
            document.getElementById("tableroPaginationInfo").innerText = "Mostrando " + (totalVisibles === 0 ? "0" : (inicio + 1) + " - " + mostrandoHasta) + " de " + totalCitasLabel + " citas en sala";
            document.getElementById("tableroPageNum").innerText = "Página " + paginaActual + " de " + totalPaginas;

            document.getElementById("btnPrevPage").disabled = (paginaActual <= 1);
            document.getElementById("btnNextPage").disabled = (paginaActual >= totalPaginas);
        }

        function cambiarPagina(delta) {
            paginaActual += delta;
            filtrarCitas();
        }

        // Acciones sobre citas
        function admitirCita(idCita, pacienteNom) {
            if (confirm("¿Confirmar ingreso a sala de espera de " + pacienteNom + "?")) {
                document.getElementById("formAdmitirIdCita").value = idCita;
                document.getElementById("formAdmitir").submit();
            }
        }

        function cancelarCita(idCita, pacienteNom) {
            if (confirm("¿Desea cancelar la cita de " + pacienteNom + "? El cupo será ofrecido inmediatamente a los pacientes en lista de espera.")) {
                document.getElementById("formCancelarIdCita").value = idCita;
                document.getElementById("formCancelar").submit();
            }
        }

        function actualizarAhora() {
            window.location.reload();
        }

        // Exportación de Citas a Excel y CSV
        function exportarCSV() {
            exportarArchivo("csv");
        }

        function exportarExcel() {
            exportarArchivo("excel");
        }

        function exportarArchivo(formato) {
            const filas = document.querySelectorAll("#tbodyCitas .cita-fila");
            let csv = "\uFEFF"; // UTF-8 BOM para apertura correcta con tildes en Excel
            csv += "HORA;PACIENTE;DNI;SEDE;CONSULTORIO;MEDICO;ESTADO\r\n";

            filas.forEach(f => {
                const cols = f.querySelectorAll("td");
                if (cols.length >= 5) {
                    const hora = cols[0].innerText.trim();
                    const paciente = cols[1].querySelector(".tablero-paciente-name") ? cols[1].querySelector(".tablero-paciente-name").innerText.trim() : cols[1].innerText.trim();
                    const dniSub = cols[1].querySelector(".tablero-paciente-sub") ? cols[1].querySelector(".tablero-paciente-sub").innerText.trim() : "";
                    const sede = cols[2].querySelector(".tablero-sede-name") ? cols[2].querySelector(".tablero-sede-name").innerText.trim() : cols[2].innerText.trim();
                    const cons = cols[2].querySelector(".tablero-sede-cons") ? cols[2].querySelector(".tablero-sede-cons").innerText.trim() : "";
                    const medico = cols[3].innerText.trim();
                    const estado = cols[4].innerText.trim();

                    csv += '"' + hora + '";"' + paciente + '";"' + dniSub + '";"' + sede + '";"' + cons + '";"' + medico + '";"' + estado + '"\r\n';
                }
            });

            const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = formato === "excel" ? "tablero_citas_sala_espera.csv" : "citas_operativas_diarias.csv";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }

        // Modal de Registro de Pacientes
        function abrirModalRegistrarPaciente() {
            document.getElementById("modalRegistrarPaciente").style.display = "flex";
            if (window.lucide) lucide.createIcons();
        }

        function cerrarModalRegistrarPaciente() {
            document.getElementById("modalRegistrarPaciente").style.display = "none";
        }

        function cerrarModalFondo(event) {
            if (event.target === document.getElementById("modalRegistrarPaciente")) {
                cerrarModalRegistrarPaciente();
            }
        }
    </script>
</body>
</html>