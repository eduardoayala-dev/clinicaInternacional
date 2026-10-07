<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombreMedico = (String) request.getAttribute("nombreMedico");
    if (nombreMedico == null && usuario != null) {
        nombreMedico = "Dr. " + usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombreMedico == null) nombreMedico = "Dr. Carlos Mendoza";

    List<Map<String, Object>> agenda = (List<Map<String, Object>>) request.getAttribute("agenda");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mi Agenda de Citas - Portal del Médico | Clínica Internacional</title>
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

        /* STATUS BANNER */
        .status-banner {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 10px 16px;
            margin-bottom: 20px; font-size: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px;
        }

        /* HEADER SECTION */
        .header-box {
            display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
        }
        .header-title { font-size: 22px; font-weight: 800; text-transform: uppercase; letter-spacing: -0.5px; }
        .header-tag { background: #ffffff; border: 2px solid var(--border-dark); padding: 6px 12px; font-size: 12px; font-weight: 800; text-transform: uppercase; }

        /* PARAMETERS FILTER CARD */
        .filter-card {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 16px 20px; margin-bottom: 20px;
        }
        .filter-card-title { font-size: 11px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); margin-bottom: 12px; display: flex; justify-content: space-between; }
        .filter-grid { display: grid; grid-template-columns: 2fr 1.5fr 3fr 2fr; gap: 16px; align-items: flex-end; }
        .form-group label { display: block; font-size: 11px; font-weight: 800; text-transform: uppercase; margin-bottom: 4px; color: var(--text-dark); }
        .form-group select, .form-group input {
            width: 100%; padding: 8px 12px; border: 1px solid var(--border-dark); border-radius: 4px;
            font-size: 13px; font-weight: 600; outline: none; background: #ffffff;
        }

        .view-mode-group { display: flex; border: 1px solid var(--border-dark); border-radius: 4px; overflow: hidden; }
        .btn-view-mode { flex: 1; padding: 8px 0; border: none; background: #ffffff; font-size: 11px; font-weight: 800; cursor: pointer; text-align: center; }
        .btn-view-mode.active { background: var(--border-dark); color: #ffffff; }

        /* SUMMARY BAR */
        .summary-bar {
            background: #f1f5f9; border: 2px solid var(--border-dark); border-bottom: none; border-radius: 6px 6px 0 0;
            padding: 12px 18px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; font-weight: 800; text-transform: uppercase;
        }

        /* TABLE */
        .table-box { background: #ffffff; border: 2px solid var(--border-dark); border-radius: 0 0 6px 6px; overflow: hidden; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; border-bottom: 2px solid var(--border-dark); padding: 12px 16px; font-size: 11px; font-weight: 800; text-transform: uppercase; color: var(--text-dark); }
        td { border-bottom: 1px solid #e2e8f0; padding: 12px 16px; font-size: 13px; font-weight: 500; vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .time-slot { font-weight: 800; font-size: 12px; white-space: nowrap; }
        .patient-name { font-weight: 700; color: var(--text-dark); }
        .sub-tag { font-size: 10px; background: #0f172a; color: #ffffff; padding: 2px 6px; border-radius: 2px; margin-left: 6px; font-weight: 800; }

        .badge-status { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 800; border: 1px solid var(--border-dark); }
        .badge-atendido { background: #e2e8f0; color: #475569; }
        .badge-espera { background: #0f172a; color: #ffffff; }
        .badge-confirmada { background: #ffffff; color: #0f172a; }

        .btn-open {
            padding: 6px 14px; border-radius: 4px; font-size: 11px; font-weight: 800; border: 1px solid var(--border-dark);
            background: #ffffff; color: var(--text-dark); text-decoration: none; display: inline-block; transition: all 0.2s; text-transform: uppercase;
        }
        .btn-open.active { background: #0f172a; color: #ffffff; }
        .btn-open:hover { background: #f1f5f9; }

        /* FOOTER ACTIONS */
        .footer-action-bar {
            background: #ffffff; border: 2px solid var(--border-dark); border-radius: 6px; padding: 12px 18px;
            display: flex; justify-content: space-between; align-items: center; font-size: 12px;
        }
        .btn-foot {
            background: #ffffff; border: 1px solid var(--border-dark); padding: 8px 14px; font-size: 12px; font-weight: 800;
            border-radius: 4px; cursor: pointer; text-transform: uppercase; display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s;
        }
        .btn-foot:hover { background: #f1f5f9; }

        .pagination { display: flex; align-items: center; gap: 8px; font-weight: 800; font-size: 11px; text-transform: uppercase; }
        .btn-page { padding: 4px 10px; border: 1px solid var(--border-dark); border-radius: 4px; background: #ffffff; cursor: pointer; }
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
            <a href="${pageContext.request.contextPath}/medico/agenda" class="sidebar-item active">
                2. MI AGENDA <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="sidebar-item">
                3. ATENCIÓN PACIENTE <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="sidebar-item">
                4. PERFIL <i data-lucide="chevron-right" style="width: 16px;"></i>
            </a>
        </aside>

        <!-- MAIN CONTENT -->
        <main class="main-content">

            <!-- STATUS BANNER -->
            <div class="status-banner">
                [ ! ] ESTADO: AGENDA REGULAR OPERATIVA — 3 TURNOS HOY / PRÓXIMAS FECHAS ACTIVAS
            </div>

            <!-- HEADER SECTION -->
            <div class="header-box">
                <h1 class="header-title">2. MI AGENDA DE CITAS MÉDICAS — HOY</h1>
                <span class="header-tag">CONTROL AMBULATORIO // MATRIZ DIARIA</span>
            </div>

            <!-- PARAMETERS FILTER CARD -->
            <div class="filter-card">
                <div class="filter-card-title">
                    <span>SELECCIÓN DE PARÁMETROS OPERATIVOS</span>
                    <span>FILTROS DISPONIBLES: 4</span>
                </div>
                <div class="filter-grid">
                    <div class="form-group">
                        <label>SEDE CLÍNICA: *</label>
                        <select id="sedeSelect">
                            <option selected>Sede Central — San Isidro</option>
                            <option>Sede San Borja</option>
                            <option>Sede Surco / Miraflores</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>FECHA DE CONSULTA: *</label>
                        <input type="date" value="2026-10-14">
                    </div>

                    <div class="form-group">
                        <label>BUSCAR PACIENTE O N° CITA:</label>
                        <input type="text" placeholder="ej. Pérez, 72345678">
                    </div>

                    <div class="form-group">
                        <label>MODO DE VISTA:</label>
                        <div class="view-mode-group">
                            <button class="btn-view-mode active">DÍA</button>
                            <button class="btn-view-mode">SEM</button>
                            <button class="btn-view-mode">MES</button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- SUMMARY BAR -->
            <div class="summary-bar">
                <div><i data-lucide="map-pin" style="width: 14px; display: inline; vertical-align: middle;"></i> SEDE CENTRAL // PABELLÓN B - CONSULTORIO 302 // FECHA: 14 DE OCTUBRE DE 2026</div>
                <div>TOTAL ASIGNADOS: 6 &nbsp;|&nbsp; ATENDIDOS: 1 &nbsp;|&nbsp; EN ESPERA: 1 &nbsp;|&nbsp; PENDIENTES: 4</div>
            </div>

            <!-- TABLE BOX -->
            <div class="table-box">
                <table>
                    <thead>
                        <tr>
                            <th>HORA</th>
                            <th>PACIENTE</th>
                            <th>N° DOCUMENTO</th>
                            <th>MOTIVO DE CONSULTA</th>
                            <th>TIPO / ORIGEN</th>
                            <th>ESTADO</th>
                            <th style="text-align: right;">ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td class="time-slot">09:00 - 09:20</td>
                            <td class="patient-name">Juan Alberto Pérez Gómez</td>
                            <td>DNI 72345678</td>
                            <td>Chequeo preventivo / Dolor torácico</td>
                            <td>Reserva Web</td>
                            <td><span class="badge-status badge-atendido">ATENDIDO</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=1" class="btn-open">ABRIR</a>
                            </td>
                        </tr>
                        <tr style="background: #eff6ff;">
                            <td class="time-slot" style="color: #2563eb;">09:30 - 09:50</td>
                            <td class="patient-name" style="color: #2563eb;">
                                María Elena Ramos <span class="sub-tag">[ TURNO ACTUAL EN SALA ]</span>
                            </td>
                            <td>DNI 44102931</td>
                            <td>Evaluación postoperatoria</td>
                            <td>Reserva Web</td>
                            <td><span class="badge-status badge-espera">EN ESPERA</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=2" class="btn-open active">ABRIR</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-slot">10:00 - 10:20</td>
                            <td class="patient-name">Carlos Fernando Soto</td>
                            <td>DNI 10928374</td>
                            <td>Control de arritmia</td>
                            <td>Lista de espera</td>
                            <td><span class="badge-status badge-confirmada">CONFIRMADA</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=3" class="btn-open">ABRIR</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-slot">10:30 - 10:50</td>
                            <td class="patient-name">Lucía Alejandra Vega</td>
                            <td>DNI 55829104</td>
                            <td>Lectura de holter</td>
                            <td>Reserva Web</td>
                            <td><span class="badge-status badge-confirmada">CONFIRMADA</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=4" class="btn-open">ABRIR</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-slot">11:00 - 11:20</td>
                            <td class="patient-name">Roberto Mario Castro</td>
                            <td>DNI 06291823</td>
                            <td>Evaluación prequirúrgica</td>
                            <td>Cupo recuperado</td>
                            <td><span class="badge-status badge-confirmada">CONFIRMADA</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=5" class="btn-open">ABRIR</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="time-slot">11:30 - 11:50</td>
                            <td class="patient-name">Patricia Elena Domínguez</td>
                            <td>DNI 32918231</td>
                            <td>Hipertensión arterial control</td>
                            <td>Reserva Web</td>
                            <td><span class="badge-status badge-confirmada">CONFIRMADA</span></td>
                            <td style="text-align: right;">
                                <a href="${pageContext.request.contextPath}/medico/atencion?idCita=6" class="btn-open">ABRIR</a>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- FOOTER ACTION BAR -->
            <div class="footer-action-bar">
                <div style="display: flex; gap: 12px;">
                    <button class="btn-foot" onclick="alert('Exportando agenda en PDF/Texto...');">
                        <i data-lucide="download" style="width: 14px;"></i> EXPORTAR AGENDA EN LISTADO PDF/TEXTO
                    </button>
                    <button class="btn-foot" onclick="window.print();">
                        <i data-lucide="printer" style="width: 14px;"></i> IMPRIMIR HOJA DE TRABAJO
                    </button>
                </div>
                <div class="pagination">
                    <span>MOSTRANDO 6 DE 6 CITAS</span>
                    <button class="btn-page">ANTERIOR</button>
                    <span>PÁGINA 1 DE 1</span>
                    <button class="btn-page">SIGUIENTE</button>
                </div>
            </div>

        </main>
    </div>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
