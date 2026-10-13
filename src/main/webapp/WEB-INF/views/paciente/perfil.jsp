<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="pe.edu.utp.clinica.model.Paciente" %>
<%
    Usuario usuario = (Usuario) request.getAttribute("usuario");
    if (usuario == null) usuario = (Usuario) session.getAttribute("usuarioLogueado");
    
    Paciente paciente = (Paciente) request.getAttribute("paciente");

    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null || nombrePaciente.trim().isEmpty()) {
        nombrePaciente = "Paciente";
    }

    String nombresVal = usuario != null && usuario.getNombres() != null ? usuario.getNombres() : "";
    String apellidosVal = usuario != null && usuario.getApellidos() != null ? usuario.getApellidos() : "";
    String emailVal = usuario != null && usuario.getEmail() != null ? usuario.getEmail() : "";
    String dniVal = (paciente != null && paciente.getDni() != null) ? paciente.getDni() : "";
    
    String fechaNacVal = "";
    if (paciente != null && paciente.getFechaNacimiento() != null) {
        fechaNacVal = paciente.getFechaNacimiento().toString(); // YYYY-MM-DD
    }

    String edadVal = (paciente != null) ? paciente.getEdadCalculada() : "--";
    String telefonoVal = (paciente != null && paciente.getTelefono() != null) ? paciente.getTelefono() : "";
    String direccionVal = (paciente != null && paciente.getDireccion() != null) ? paciente.getDireccion() : "";
    String canalAvisoVal = (paciente != null && paciente.getCanalAviso() != null) ? paciente.getCanalAviso() : "WhatsApp";
    String sedePrefVal = (paciente != null && paciente.getSedePreferida() != null) ? paciente.getSedePreferida() : "Sede San Borja";
    String alergiasVal = (paciente != null && paciente.getAlergias() != null) ? paciente.getAlergias() : "";
    String coberturaVal = (paciente != null && paciente.getTipoCobertura() != null) ? paciente.getTipoCobertura() : "EPS";
    String codigoRegistro = (paciente != null) ? paciente.getCodigoRegistro() : (usuario != null ? "PAC-" + String.format("%06d", usuario.getIdUsuario()) : "PAC-000000");

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty() && !"Paciente".equalsIgnoreCase(nombrePaciente.trim())) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }

    String mensajeExito = (String) request.getAttribute("mensajeExito");
    String mensajeError = (String) request.getAttribute("mensajeError");

    pe.edu.utp.clinica.dao.CitaDAO citaDAO_bell = new pe.edu.utp.clinica.dao.CitaDAO();
    int numNotifSinLeer = request.getAttribute("numNotifSinLeer") != null ? (Integer) request.getAttribute("numNotifSinLeer") : (usuario != null ? citaDAO_bell.contarNotificacionesSinLeer(usuario.getIdUsuario()) : 0);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Perfil del paciente - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Playfair+Display:wght@600;700;800&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/paciente-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/perfil-paciente.css">
</head>
<body style="display: flex; height: 100vh; overflow: hidden; background: #f4f7fb; font-family: 'Inter', sans-serif;">

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
            <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="nav-item">
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
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item active">
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
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout" title="Cerrar sesión">
                <i data-lucide="log-out" style="width: 18px; height: 18px;"></i>
            </a>
        </div>
    </aside>

    <!-- ÁREA PRINCIPAL -->
    <main class="perfil-main-container">
        
        <!-- BARRA SUPERIOR INSTITUCIONAL (ESTÁNDAR COMPARTIDO) -->
        <header class="dash-top-header">
            <div class="breadcrumb-trail">
                <span>Paciente</span>
                <span class="bc-sep">/</span>
                <strong>Perfil</strong>
            </div>

            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-avisos-pill" title="Centro de Avisos">
                <i data-lucide="bell" style="width: 16px; height: 16px; color: #64748b;"></i>
                <span>Avisos</span>
                <% if (numNotifSinLeer > 0) { %>
                    <span class="badge-avisos-count"><%= numNotifSinLeer %></span>
                <% } %>
            </a>
        </header>

        <!-- CONTENEDOR CON SCROLL -->
        <div class="perfil-scroll-container">
            <div class="perfil-content-inner">

            <% if (mensajeExito != null) { %>
                <div class="perfil-alert perfil-alert-success">
                    <i data-lucide="check-circle" style="width: 18px; height: 18px;"></i>
                    <span><%= mensajeExito %></span>
                </div>
            <% } %>

            <% if (mensajeError != null) { %>
                <div class="perfil-alert perfil-alert-error">
                    <i data-lucide="alert-circle" style="width: 18px; height: 18px;"></i>
                    <span><%= mensajeError %></span>
                </div>
            <% } %>

            <!-- ENCABEZADO DE PERFIL CON AVATAR E IDENTIFICADOR -->
            <div class="perfil-title-bar">
                <div class="perfil-avatar-circle"><%= inicialesPaciente %></div>
                <div class="perfil-title-details">
                    <h1 class="perfil-main-heading">Perfil del paciente</h1>
                    <div class="perfil-meta-row">
                        <span>ID registro: <strong><%= codigoRegistro %></strong></span>
                        <span class="badge-activo-tag">Activo</span>
                    </div>
                </div>
            </div>

            <!-- FORMULARIO GENERAL DE PERFIL -->
            <form id="perfilForm" action="${pageContext.request.contextPath}/paciente/perfil" method="POST">
                
                <!-- GRID DE TARJETAS (2 COLUMNAS SUPERIORES) -->
                <div class="perfil-grid-container">

                    <!-- COLUMNA IZQUIERDA: DATOS PERSONALES E IDENTIFICACIÓN -->
                    <div class="perfil-card">
                        <h2 class="perfil-card-title">Datos personales e identificación</h2>

                        <div class="form-row-2col">
                            <div class="form-group-item">
                                <label for="nombresInput">Nombres completos *</label>
                                <input type="text" id="nombresInput" name="nombres" value="<%= nombresVal %>" required class="perfil-input">
                            </div>
                            <div class="form-group-item">
                                <label for="apellidosInput">Apellidos completos *</label>
                                <input type="text" id="apellidosInput" name="apellidos" value="<%= apellidosVal %>" required class="perfil-input">
                            </div>
                        </div>

                        <div class="form-row-full">
                            <div class="form-group-item">
                                <label for="documentoInput">Tipo y número de documento</label>
                                <div class="input-wrapper-relative">
                                    <input type="text" id="documentoInput" value="<%= dniVal.isEmpty() ? "" : ("DNI " + dniVal) %>" readonly class="perfil-input readonly-input">
                                    <span class="badge-solo-lectura">Solo lectura</span>
                                </div>
                            </div>
                        </div>

                        <div class="form-row-2col" style="margin-bottom: 0;">
                            <div class="form-group-item">
                                <label for="fechaNacimientoInput">Fecha de nacimiento *</label>
                                <div class="input-wrapper-relative">
                                    <input type="date" id="fechaNacimientoInput" name="fechaNacimiento" value="<%= fechaNacVal %>" required class="perfil-input" onchange="calcularEdad(this.value)">
                                </div>
                            </div>
                            <div class="form-group-item">
                                <label for="edadCalculadaInput">Edad calculada</label>
                                <div class="input-wrapper-relative">
                                    <input type="text" id="edadCalculadaInput" value="<%= edadVal %>" readonly class="perfil-input readonly-input">
                                    <span class="badge-solo-lectura">Solo lectura</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- COLUMNA DERECHA: CONTACTO Y UBICACIÓN -->
                    <div class="right-column-stack">
                        
                        <!-- TARJETA: CONTACTO Y NOTIFICACIÓN -->
                        <div class="perfil-card">
                            <h2 class="perfil-card-title">Datos de contacto y notificación</h2>

                            <div class="form-row-2col">
                                <div class="form-group-item">
                                    <label for="telefonoInput">Teléfono celular / WhatsApp *</label>
                                    <input type="tel" id="telefonoInput" name="telefono" value="<%= telefonoVal %>" required class="perfil-input">
                                </div>
                                <div class="form-group-item">
                                    <label for="emailInput">Correo electrónico principal *</label>
                                    <input type="email" id="emailInput" name="email" value="<%= emailVal %>" required class="perfil-input">
                                </div>
                            </div>

                            <div class="form-row-full" style="margin-bottom: 0;">
                                <div class="form-group-item">
                                    <label>Canal preferido de aviso *</label>
                                    <div class="radio-options-row">
                                        <label class="radio-pill-option <%= "SMS".equalsIgnoreCase(canalAvisoVal) ? "active" : "" %>">
                                            <input type="radio" name="canalAviso" value="SMS" <%= "SMS".equalsIgnoreCase(canalAvisoVal) ? "checked" : "" %> onchange="actualizarRadioPills()">
                                            <span>SMS</span>
                                        </label>
                                        <label class="radio-pill-option <%= "WhatsApp".equalsIgnoreCase(canalAvisoVal) ? "active" : "" %>">
                                            <input type="radio" name="canalAviso" value="WhatsApp" <%= "WhatsApp".equalsIgnoreCase(canalAvisoVal) ? "checked" : "" %> onchange="actualizarRadioPills()">
                                            <span>WhatsApp</span>
                                        </label>
                                        <label class="radio-pill-option <%= "Correo".equalsIgnoreCase(canalAvisoVal) ? "active" : "" %>">
                                            <input type="radio" name="canalAviso" value="Correo" <%= "Correo".equalsIgnoreCase(canalAvisoVal) ? "checked" : "" %> onchange="actualizarRadioPills()">
                                            <span>Correo</span>
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TARJETA: UBICACIÓN Y ZONA DE RESIDENCIA -->
                        <div class="perfil-card">
                            <h2 class="perfil-card-title">Ubicación y zona de residencia</h2>

                            <div class="form-row-2col" style="margin-bottom: 0;">
                                <div class="form-group-item">
                                    <label for="direccionInput">Dirección / zona de residencia *</label>
                                    <input type="text" id="direccionInput" name="direccion" value="<%= direccionVal %>" required class="perfil-input">
                                </div>
                                <div class="form-group-item">
                                    <label for="sedePreferidaSelect">Sede preferida *</label>
                                    <select id="sedePreferidaSelect" name="sedePreferida" class="perfil-select">
                                        <option value="Sede San Borja" <%= "Sede San Borja".equalsIgnoreCase(sedePrefVal) ? "selected" : "" %>>Sede San Borja</option>
                                        <option value="Sede Lima Centro" <%= "Sede Lima Centro".equalsIgnoreCase(sedePrefVal) ? "selected" : "" %>>Sede Lima Centro</option>
                                        <option value="Sede Surco" <%= "Sede Surco".equalsIgnoreCase(sedePrefVal) ? "selected" : "" %>>Sede Surco</option>
                                        <option value="Sede Bellavista" <%= "Sede Bellavista".equalsIgnoreCase(sedePrefVal) ? "selected" : "" %>>Sede Bellavista</option>
                                    </select>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>

                <!-- TARJETA INFERIOR (ANCHO COMPLETO): SALUD Y COBERTURA -->
                <div class="perfil-card perfil-card-full">
                    <h2 class="perfil-card-title">Datos de salud y cobertura</h2>

                    <div class="form-row-2col" style="margin-bottom: 0;">
                        <div class="form-group-item">
                            <label for="alergiasInput">Alergias conocidas</label>
                            <input type="text" id="alergiasInput" name="alergias" value="<%= alergiasVal %>" placeholder="Ninguna o especificar medicamentos/alimentos" class="perfil-input">
                        </div>
                        <div class="form-group-item">
                            <label for="tipoCoberturaSelect">Tipo de cobertura *</label>
                            <select id="tipoCoberturaSelect" name="tipoCobertura" class="perfil-select">
                                <option value="EPS" <%= (coberturaVal != null && (coberturaVal.contains("EPS") || "Particular / EPS".equalsIgnoreCase(coberturaVal))) ? "selected" : "" %>>EPS</option>
                                <option value="Particular" <%= "Particular".equalsIgnoreCase(coberturaVal) ? "selected" : "" %>>Particular</option>
                                <option value="Seguro Privado / RIMAC" <%= (coberturaVal != null && (coberturaVal.contains("Rimac") || "Seguro Privado / RIMAC".equalsIgnoreCase(coberturaVal))) ? "selected" : "" %>>Seguro Privado / RIMAC</option>
                                <option value="EsSalud / SIS" <%= (coberturaVal != null && (coberturaVal.contains("EsSalud") || "EsSalud / SIS".equalsIgnoreCase(coberturaVal))) ? "selected" : "" %>>EsSalud / SIS</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- BARRA DE ACCIONES INFERIOR -->
                <div class="perfil-footer-bar">
                    <span class="footnote-required">Los campos con (*) son obligatorios</span>

                    <div class="footer-btn-actions">
                        <a href="${pageContext.request.contextPath}/paciente/perfil" class="btn-cancel-profile">Cancelar cambios</a>
                        <button type="submit" class="btn-save-profile">
                            <i data-lucide="check" style="width: 17px; height: 17px;"></i>
                            <span>Guardar cambios</span>
                        </button>
                    </div>
                </div>

            </form>
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();

        function actualizarRadioPills() {
            document.querySelectorAll('.radio-pill-option').forEach(function(label) {
                var radio = label.querySelector('input[type="radio"]');
                if (radio && radio.checked) {
                    label.classList.add('active');
                } else {
                    label.classList.remove('active');
                }
            });
        }

        function calcularEdad(fechaStr) {
            if (!fechaStr) return;
            var fechaNac = new Date(fechaStr);
            if (isNaN(fechaNac.getTime())) return;

            var hoy = new Date();
            var edad = hoy.getFullYear() - fechaNac.getFullYear();
            var m = hoy.getMonth() - fechaNac.getMonth();
            if (m < 0 || (m === 0 && hoy.getDate() < fechaNac.getDate())) {
                edad--;
            }
            if (edad >= 0) {
                document.getElementById('edadCalculadaInput').value = edad + ' años';
            }
        }
    </script>
</body>
</html>
