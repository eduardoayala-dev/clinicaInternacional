<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    String mensajeExito = (String) request.getAttribute("mensajeExito");
    int idCita = request.getAttribute("idCita") != null ? (Integer) request.getAttribute("idCita") : 1;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Atención de Paciente Actual - Portal del Médico | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #0f172a;
            --primary-accent: #2563eb;
            --bg-page: #f8fafc;
            --bg-card: #ffffff;
            --border-dark: #0f172a;
            --border-light: #cbd5e1;
            --text-dark: #0f172a;
            --text-muted: #64748b;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background-color: var(--bg-page); color: var(--text-dark); display: flex; flex-direction: column; min-height: 100vh; }

        .top-header {
            background: #ffffff; border-bottom: 2px solid var(--border-dark); padding: 12px 24px;
            display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 700;
        }
        .top-header-left { display: flex; align-items: center; gap: 10px; text-transform: uppercase; letter-spacing: 0.5px; }
        .top-header-right { display: flex; align-items: center; gap: 16px; }
        .user-tag { background: #f1f5f9; padding: 6px 14px; border: 1px solid var(--border-dark); border-radius: 4px; font-size: 12px; }
        .btn-logout {
            background: #ffffff; border: 1px solid var(--border-dark); color: var(--text-dark); padding: 6px 14px;
            border-radius: 4px; text-decoration: none; font-weight: 700; display: flex; align-items: center; gap: 6px;
            font-size: 12px; transition: all 0.2s;
        }
        .btn-logout:hover { background: #fee2e2; border-color: #ef4444; color: #b91c1c; }

        .app-container { display: flex; flex: 1; }

        .sidebar {
            width: 240px; background: #ffffff; border-right: 2px solid var(--border-dark); padding: 16px;
            display: flex; flex-direction: column; gap: 8px; flex-shrink: 0;
        }
        .sidebar-item {
            padding: 12px 14px; border: 1px solid var(--border-dark); border-radius: 4px; color: var(--text-dark);
            text-decoration: none; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;
            transition: all 0.2s; display: flex; align-items: center; justify-content: space-between;
        }
        .sidebar-item:hover { background: #f1f5f9; }
        .sidebar-item.active { background: var(--border-dark); color: #ffffff; }

        .main-content { flex: 1; padding: 24px 32px; overflow-y: auto; }

        .alert-success {
            background: #dcfce7; color: #166534; border: 2px solid #166534; padding: 12px 16px;
            border-radius: 6px; font-weight: 700; margin-bottom: 20px; font-size: 13px; display: flex; align-items: center; gap: 8px;
        }

        /* EXPEDIENTE BAR */
        .expediente-bar {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 10px 16px;
            margin-bottom: 12px; font-size: 13px; font-weight: 800; text-transform: uppercase;
        }

        .op-msg-box {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 12px 16px;
            margin-bottom: 20px; font-size: 12px; display: flex; align-items: center; gap: 12px;
        }
        .op-msg-tag { background: #0f172a; color: #ffffff; padding: 4px 8px; font-weight: 800; font-size: 11px; border-radius: 3px; }

        /* SECTION BOX */
        .sec-box {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; margin-bottom: 20px; overflow: hidden;
        }
        .sec-head {
            background: #f1f5f9; border-bottom: 2px solid var(--border-dark); padding: 12px 20px;
            display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 800; text-transform: uppercase;
        }

        .sec-body { padding: 20px; }

        /* SECTION 1 GRID */
        .sec1-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
        .sec1-col-title { font-size: 12px; font-weight: 800; text-transform: uppercase; color: var(--text-dark); margin-bottom: 12px; display: flex; justify-content: space-between; }

        .info-row { margin-bottom: 10px; }
        .info-label { font-size: 11px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); margin-bottom: 3px; }
        .info-value-box {
            background: #f8fafc; border: 1px solid var(--border-dark); padding: 8px 12px; border-radius: 4px;
            font-size: 13px; font-weight: 700; color: var(--text-dark);
        }

        /* SECTION 2 CONTROL */
        .notice-box {
            background: #f8fafc; border: 1px solid var(--border-dark); padding: 10px 14px; border-radius: 4px;
            font-size: 12px; font-weight: 700; color: var(--text-muted); margin-bottom: 16px; text-transform: uppercase;
        }
        .attendance-group { display: flex; gap: 16px; margin-bottom: 16px; }
        .btn-attendance {
            flex: 1; padding: 14px; border: 2px solid var(--border-dark); border-radius: 6px; background: #ffffff;
            font-size: 13px; font-weight: 800; cursor: pointer; text-align: center; text-transform: uppercase; transition: all 0.2s;
        }
        .btn-attendance.selected { background: #0f172a; color: #ffffff; }

        .status-confirmed-box {
            background: #ffffff; border: 2px solid var(--border-dark); padding: 12px 16px; border-radius: 6px;
            font-size: 12px; font-weight: 800; text-transform: uppercase; text-align: center;
        }

        /* SECTION 3 PRIORITY */
        .priority-options { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
        .priority-card {
            border: 2px solid var(--border-dark); border-radius: 6px; padding: 14px; cursor: pointer;
            background: #ffffff; transition: all 0.2s;
        }
        .priority-card.selected { background: #f1f5f9; border-width: 3px; }
        .priority-title { font-size: 13px; font-weight: 800; text-transform: uppercase; margin-bottom: 4px; }
        .priority-sub { font-size: 11px; color: var(--text-muted); font-weight: 600; }

        .date-picker-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
        .date-input { padding: 8px 12px; border: 1px solid var(--border-dark); border-radius: 4px; font-weight: 700; font-size: 13px; }

        textarea {
            width: 100%; height: 80px; padding: 10px 14px; border: 1px solid var(--border-dark); border-radius: 4px;
            font-size: 13px; font-weight: 600; outline: none; margin-top: 6px; font-family: 'Inter', sans-serif;
        }

        /* FOOTER ACTIONS */
        .form-foot { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 20px; background: #f8fafc; border-top: 2px solid var(--border-dark); }
        .btn-save-record {
            background: #0f172a; color: #ffffff; border: 2px solid var(--border-dark); padding: 12px 24px;
            font-size: 13px; font-weight: 800; border-radius: 4px; cursor: pointer; text-transform: uppercase; display: inline-flex; align-items: center; gap: 8px;
        }
        .btn-save-record:hover { background: #2563eb; border-color: #2563eb; }
        .btn-reset {
            background: #ffffff; color: var(--text-dark); border: 1px solid var(--border-dark); padding: 12px 20px;
            font-size: 13px; font-weight: 800; border-radius: 4px; cursor: pointer; text-transform: uppercase;
        }
    </style>
</head>
<body>

    <!-- TOP HEADER -->
    <header class="top-header">
        <div class="top-header-left">
            <i data-lucide="activity" style="width: 18px;"></i>
            PORTAL DEL MÉDICO — CLÍNICA INTERNACIONAL
        </div>
        <div class="top-header-right">
            <span class="user-tag"><%= nombreMedico %> | ROL: MÉDICO</span>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">
                <i data-lucide="log-out" style="width: 14px;"></i> CERRAR SESIÓN
            </a>
        </div>
    </header>

    <div class="app-container">
        <!-- SIDEBAR -->
        <aside class="sidebar">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="sidebar-item">
                0. INICIO <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="sidebar-item">
                2. MI AGENDA <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="sidebar-item active">
                3. ATENCIÓN PACIENTE <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="sidebar-item">
                4. PERFIL <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
        </aside>

        <!-- MAIN CONTENT -->
        <main class="main-content">

            <% if (mensajeExito != null) { %>
                <div class="alert-success">
                    <i data-lucide="check-circle-2" style="width: 18px;"></i> <%= mensajeExito %>
                </div>
            <% } %>

            <!-- EXPEDIENTE BAR -->
            <div class="expediente-bar">
                EXPEDIENTE: CITA #CIT-2026-9482 &nbsp;|&nbsp; FECHA REGISTRO: 14/10/2026 09:30:14
            </div>

            <!-- OP MSG BOX -->
            <div class="op-msg-box">
                <span class="op-msg-tag">[ ! ] MENSAJE OPERATIVO SISTEMA</span>
                <strong style="text-transform: uppercase;">ESTADO: REGISTRO OPERATIVO DE CONSULTA AMBULATORIA ACTIVA</strong>
            </div>

            <form action="${pageContext.request.contextPath}/medico/atencion" method="POST">
                <input type="hidden" name="idCita" value="<%= idCita %>">

                <!-- SECTION 1: DATOS DE LA CITA MÉDICA -->
                <div class="sec-box">
                    <div class="sec-head">
                        <span>3. ATENCIÓN DE PACIENTES — DETALLE DE CITA</span>
                        <span style="font-size: 11px; color: var(--text-muted);">METADATOS CLÍNICOS VALIDADOS</span>
                    </div>

                    <div class="sec-body">
                        <h3 style="font-size: 13px; font-weight: 800; text-transform: uppercase; margin-bottom: 16px; border-bottom: 2px solid var(--border-dark); padding-bottom: 6px;">
                            SECCIÓN 1: DATOS DE LA CITA MÉDICA
                        </h3>

                        <div class="sec1-grid">
                            <!-- IZQUIERDA: PACIENTE -->
                            <div>
                                <div class="sec1-col-title">
                                    <span>INFORMACIÓN DEL PACIENTE</span>
                                    <span style="background: #f1f5f9; padding: 2px 6px; border: 1px solid var(--border-dark); border-radius: 3px;">[ HC: #89281 ]</span>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">NOMBRE COMPLETO:</div>
                                    <div class="info-value-box">[ Juan Alberto Pérez Gómez ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">DOCUMENTO / FICHA:</div>
                                    <div class="info-value-box">[ DNI 72345678 ] &nbsp;|&nbsp; EDAD: [ 54 AÑOS ] &nbsp;|&nbsp; SEXO: [ MASCULINO ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">TELÉFONO CONTACTO:</div>
                                    <div class="info-value-box">[ +51 987 654 321 ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">TIPO DE COBERTURA:</div>
                                    <div class="info-value-box">[ PLAN EPS REGULAR — ASEGURADORA CONVENIO ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">ALERGIAS REGISTRADAS:</div>
                                    <div class="info-value-box" style="color: #166534;">[ NINGUNA CONOCIDA ]</div>
                                </div>
                            </div>

                            <!-- DERECHA: PROGRAMACIÓN -->
                            <div>
                                <div class="sec1-col-title">
                                    <span>DETALLES DE PROGRAMACIÓN</span>
                                    <span style="background: #0f172a; color: #ffffff; padding: 2px 6px; border-radius: 3px;">[ BLOQUE REGULAR ]</span>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">FECHA Y HORA:</div>
                                    <div class="info-value-box">[ JUEVES 24 DE OCTUBRE DE 2026 — 09:30 AM ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">SEDE Y CONSULTORIO:</div>
                                    <div class="info-value-box">[ SEDE CENTRAL — CONSULTORIO 302 (PABELLÓN B) ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">MÉDICO TRATANTE:</div>
                                    <div class="info-value-box">[ <%= nombreMedico %> — CARDIOLOGÍA ]</div>
                                </div>

                                <div class="info-row">
                                    <div class="info-label">MOTIVO DE CONSULTA:</div>
                                    <div class="info-value-box">[ CHEQUEO PREVENTIVO / MOLESTIA TORÁCICA LEVE AL ESFUERZO ]</div>
                                </div>

                                <div style="display: flex; gap: 12px; margin-top: 10px;">
                                    <div style="flex: 1;" class="info-label">CANAL: [ WEB PACIENTES ]</div>
                                    <div style="flex: 1;" class="info-label">TICKET: [ TK-08918-B ]</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- SECTION 2: CONTROL DE ASISTENCIA DEL PACIENTE -->
                <div class="sec-box">
                    <div class="sec-head">
                        <span>SECCIÓN 2: CONTROL DE ASISTENCIA DEL PACIENTE</span>
                        <span style="font-size: 11px; color: var(--text-muted);">CONTROL OPERATIVO REQUERIDO</span>
                    </div>
                    <div class="sec-body">
                        <div class="notice-box">
                            [ AVISO CLÍNICO-ADMINISTRATIVO: Marque el estado de comparecencia del paciente para habilitar la facturación y cierre de turno ]
                        </div>

                        <div class="attendance-group">
                            <button type="button" class="btn-attendance selected" id="btnAsistio" onclick="selectAttendance('ASISTIO')">
                                [ (X) MARCAR ASISTIÓ ]
                            </button>
                            <button type="button" class="btn-attendance" id="btnNoAsistio" onclick="selectAttendance('NO_ASISTIO')">
                                [ ( ) MARCAR NO ASISTIÓ ]
                            </button>
                        </div>
                        <input type="hidden" name="asistencia" id="asistenciaInput" value="ASISTIO">

                        <div class="status-confirmed-box">
                            <i data-lucide="check" style="width: 14px; display: inline; vertical-align: middle;"></i>
                            ESTADO ACTUAL: ASISTENCIA CONFIRMADA POR RECEPCIÓN — EN CONSULTORIO
                        </div>
                    </div>
                </div>

                <!-- SECTION 3: REGISTRO DE PRIORIDAD CLÍNICA Y PRÓXIMA ATENCIÓN -->
                <div class="sec-box">
                    <div class="sec-head">
                        <span>SECCIÓN 3: REGISTRO DE PRIORIDAD CLÍNICA Y PRÓXIMA ATENCIÓN</span>
                        <span style="font-size: 11px; color: var(--text-muted);">PLAN DE SEGUIMIENTO</span>
                    </div>

                    <div class="sec-body">
                        <div style="font-size: 11px; font-weight: 800; text-transform: uppercase; margin-bottom: 10px; color: var(--text-dark);">
                            NIVEL DE PRIORIDAD CLÍNICA: * (Seleccione la criticidad o periodicidad para citas posteriores)
                        </div>

                        <div class="priority-options">
                            <div class="priority-card" onclick="selectPriority(this, 'ALTA')">
                                <div class="priority-title">[ ] ALTA</div>
                                <div class="priority-sub">Requiere seguimiento urgente o derivación prioritaria</div>
                            </div>

                            <div class="priority-card selected" onclick="selectPriority(this, 'MEDIA')">
                                <div class="priority-title">[X] MEDIA — SELECCIONADA</div>
                                <div class="priority-sub">Control estándar según evolución clínica</div>
                            </div>

                            <div class="priority-card" onclick="selectPriority(this, 'BAJA')">
                                <div class="priority-title">[ ] BAJA</div>
                                <div class="priority-sub">Control de rutina / preventivo anual</div>
                            </div>
                        </div>
                        <input type="hidden" name="prioridad" id="prioridadInput" value="MEDIA">

                        <div style="margin-top: 20px;">
                            <label style="font-size: 11px; font-weight: 800; text-transform: uppercase; display: block; margin-bottom: 6px;">
                                FECHA RECOMENDADA DE PRÓXIMA ATENCIÓN: *
                            </label>
                            <div class="date-picker-row">
                                <input type="date" class="date-input" value="2026-11-04">
                                <span style="background: #f1f5f9; padding: 6px 12px; border: 1px solid var(--border-dark); border-radius: 4px; font-size: 11px; font-weight: 800; text-transform: uppercase;">
                                    RECOMENDADO: EN 15 DÍAS
                                </span>
                            </div>
                        </div>

                        <div style="margin-top: 16px;">
                            <label style="font-size: 11px; font-weight: 800; text-transform: uppercase; display: flex; justify-content: space-between;">
                                <span>INDICACIÓN BREVE DE CONTINUIDAD (TEXTO LIBRE):</span>
                                <span style="color: var(--text-muted);">[ LÍMITE: 250 CARACTERES ]</span>
                            </label>
                            <textarea name="indicaciones" placeholder="Ingrese las observaciones clínicas y plan de continuidad...">Control con resultados de ecocardiograma y perfil lipídico completo</textarea>
                            <div style="font-size: 10px; color: var(--text-muted); margin-top: 4px; font-weight: 700;">
                                [ NOTA: Este texto se adjunta automáticamente a la orden médica y recordatorio SMS del paciente ]
                            </div>
                        </div>
                    </div>

                    <div class="form-foot">
                        <button type="reset" class="btn-reset">[ RESTABLECER ]</button>
                        <button type="submit" class="btn-save-record">
                            <i data-lucide="save" style="width: 16px;"></i> [ GUARDAR REGISTRO DE ATENCIÓN ]
                        </button>
                    </div>
                </div>

            </form>

        </main>
    </div>

    <script>
        lucide.createIcons();

        function selectAttendance(val) {
            document.getElementById('asistenciaInput').value = val;
            if (val === 'ASISTIO') {
                document.getElementById('btnAsistio').classList.add('selected');
                document.getElementById('btnNoAsistio').classList.remove('selected');
            } else {
                document.getElementById('btnAsistio').classList.remove('selected');
                document.getElementById('btnNoAsistio').classList.add('selected');
            }
        }

        function selectPriority(card, val) {
            document.querySelectorAll('.priority-card').forEach(c => {
                c.classList.remove('selected');
                c.querySelector('.priority-title').textContent = c.querySelector('.priority-title').textContent.replace('[X]', '[ ]');
            });
            card.classList.add('selected');
            card.querySelector('.priority-title').textContent = card.querySelector('.priority-title').textContent.replace('[ ]', '[X]');
            document.getElementById('prioridadInput').value = val;
        }
    </script>
</body>
</html>
