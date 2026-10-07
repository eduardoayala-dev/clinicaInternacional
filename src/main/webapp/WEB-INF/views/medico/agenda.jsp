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

    String inicialesMedico = "CM";
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
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mi Agenda - Portal del Médico | Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/lucide@latest"></script>
    <style>
        :root {
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --primary-light: #eff6ff;
            --bg-page: #f0f8ff;
            --bg-card: #ffffff;
            --text-dark: #0f172a;
            --text-gray: #64748b;
            --text-light: #94a3b8;
            --border-light: #e2e8f0;
            --radius-lg: 16px;
            --radius-md: 12px;
            --sidebar-width: 260px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Inter', sans-serif; }
        body { background-color: var(--bg-page); color: var(--text-dark); display: flex; height: 100vh; overflow: hidden; }

        .sidebar {
            width: var(--sidebar-width); background-color: var(--bg-card); border-right: 1px solid var(--border-light);
            display: flex; flex-direction: column; padding: 24px 20px; flex-shrink: 0; z-index: 10;
        }
        .brand-container { display: flex; align-items: center; gap: 12px; margin-bottom: 40px; padding-left: 8px; }
        .brand-container img { height: 36px; }
        .nav-menu { display: flex; flex-direction: column; gap: 8px; flex: 1; }
        .nav-item {
            display: flex; align-items: center; gap: 14px; padding: 12px 16px; border-radius: var(--radius-md);
            color: var(--text-gray); text-decoration: none; font-size: 14px; font-weight: 500; transition: all 0.2s;
        }
        .nav-item:hover { background-color: #f8fafc; color: var(--text-dark); }
        .nav-item.active { background-color: #eff6ff; color: var(--primary); font-weight: 600; }
        .user-profile-bottom {
            margin-top: auto; display: flex; align-items: center; gap: 12px; padding: 16px 8px 8px 8px;
            border-top: 1px solid var(--border-light); cursor: pointer; text-decoration: none;
        }
        .avatar-initials {
            display: flex; align-items: center; justify-content: center; border-radius: 50%; font-weight: 600;
            color: var(--primary); background-color: #eff6ff; width: 36px; height: 36px; font-size: 13px; flex-shrink: 0;
        }
        .user-name-sm { font-size: 13px; font-weight: 600; color: var(--text-dark); line-height: 1.3; }
        .user-role-sm { font-size: 11px; color: var(--text-gray); }

        .main-content { flex: 1; padding: 32px 40px; overflow-y: auto; }
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
        .header-title { font-size: 26px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }

        /* FILTER CARD */
        .filter-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            padding: 24px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
        }
        .filter-grid { display: grid; grid-template-columns: 2fr 1.5fr 3fr 2fr; gap: 20px; align-items: flex-end; }
        .form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-dark); margin-bottom: 6px; }
        .form-group select, .form-group input {
            width: 100%; padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light);
            font-size: 14px; color: var(--text-dark); outline: none; background: #ffffff; transition: all 0.2s;
        }
        .form-group select:focus, .form-group input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }

        .view-pill-group { display: flex; background: #f1f5f9; padding: 4px; border-radius: 30px; border: 1px solid var(--border-light); }
        .btn-view-pill {
            flex: 1; padding: 8px 16px; border: none; background: transparent; font-size: 13px; font-weight: 600;
            color: var(--text-gray); border-radius: 20px; cursor: pointer; transition: all 0.2s; text-align: center;
        }
        .btn-view-pill.active { background: #ffffff; color: var(--primary); box-shadow: 0 2px 6px rgba(0,0,0,0.06); }

        /* SUMMARY BANNER */
        .summary-banner {
            background: var(--primary-light); border: 1px solid #bfdbfe; border-radius: var(--radius-lg);
            padding: 16px 24px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center;
            font-size: 14px; color: #1e40af; font-weight: 600;
        }

        /* TABLE CARD */
        .table-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02); margin-bottom: 24px;
        }
        table { width: 100%; border-collapse: collapse; text-align: left; }
        th { background: #f8fafc; padding: 16px 20px; font-size: 12px; font-weight: 600; color: var(--text-gray); border-bottom: 1px solid var(--border-light); }
        td { padding: 16px 20px; font-size: 14px; color: var(--text-dark); border-bottom: 1px solid var(--border-light); vertical-align: middle; }
        tr:last-child td { border-bottom: none; }
        tr:hover { background: #f8fafc; }

        .badge-status { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-block; }
        .badge-atendido { background: #f1f5f9; color: #475569; }
        .badge-espera { background: #dcfce7; color: #166534; }
        .badge-confirmada { background: #eff6ff; color: #2563eb; }

        .btn-open-tbl {
            padding: 8px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; text-decoration: none;
            transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px; background: var(--primary-light); color: var(--primary);
        }
        .btn-open-tbl:hover { background: var(--primary); color: #ffffff; }

        /* FOOTER ACTIONS */
        .footer-action-bar {
            display: flex; justify-content: space-between; align-items: center;
        }
        .btn-action-foot {
            background: #ffffff; border: 1px solid var(--border-light); padding: 12px 20px; border-radius: 30px;
            font-size: 13px; font-weight: 600; color: var(--text-dark); cursor: pointer; transition: all 0.2s;
            display: inline-flex; align-items: center; gap: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.02);
        }
        .btn-action-foot:hover { background: #f8fafc; transform: translateY(-1px); }

        @media(max-width:900px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } .filter-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/medico/inicio" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/medico/agenda" class="nav-item active">
                <i data-lucide="calendar" style="width: 18px;"></i> Mi Agenda
            </a>
            <a href="${pageContext.request.contextPath}/medico/atencion" class="nav-item">
                <i data-lucide="stethoscope" style="width: 18px;"></i> Atención Paciente
            </a>
            <a href="${pageContext.request.contextPath}/medico/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi Perfil
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= inicialesMedico %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombreMedico %></div>
                <div class="user-role-sm">Médico Especialista</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Mi Agenda de Citas Médicas</h1>
                <p class="header-subtitle">Gestión diaria de consultas ambulatorias y control de turnos asignados.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <!-- FILTER CARD -->
        <div class="filter-card">
            <div class="filter-grid">
                <div class="form-group">
                    <label>Sede Clínica</label>
                    <select id="sedeSelect">
                        <option selected>Sede Central — San Isidro</option>
                        <option>Sede San Borja</option>
                        <option>Sede Surco / Miraflores</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>Fecha de Consulta</label>
                    <input type="date" value="2026-10-14">
                </div>

                <div class="form-group">
                    <label>Buscar Paciente o N° Cita</label>
                    <input type="text" placeholder="Ej: Pérez o DNI 72345678">
                </div>

                <div class="form-group">
                    <label>Modo de Vista</label>
                    <div class="view-pill-group">
                        <button class="btn-view-pill active">Día</button>
                        <button class="btn-view-pill">Semana</button>
                        <button class="btn-view-pill">Mes</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- SUMMARY BANNER -->
        <div class="summary-banner">
            <div>
                <i data-lucide="map-pin" style="width: 16px; display: inline; vertical-align: middle; margin-right: 6px;"></i>
                Sede Central — Consultorio 302 (Pabellón B) &nbsp;•&nbsp; 14 de Octubre de 2026
            </div>
            <div>
                Total Asignados: 6 &nbsp;•&nbsp; Atendidos: 1 &nbsp;•&nbsp; En Espera: 1 &nbsp;•&nbsp; Pendientes: 4
            </div>
        </div>

        <!-- TABLE CARD -->
        <div class="table-card">
            <table>
                <thead>
                    <tr>
                        <th>HORA</th>
                        <th>PACIENTE</th>
                        <th>DOCUMENTO</th>
                        <th>MOTIVO DE CONSULTA</th>
                        <th>TIPO / ORIGEN</th>
                        <th>ESTADO</th>
                        <th style="text-align: right;">ACCIÓN</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td style="font-weight: 700;">09:00 - 09:20</td>
                        <td style="font-weight: 600;">Juan Alberto Pérez Gómez</td>
                        <td style="color: var(--text-gray);">DNI 72345678</td>
                        <td>Chequeo preventivo / Dolor torácico</td>
                        <td>Reserva Web</td>
                        <td><span class="badge-status badge-atendido">Atendido</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=1" class="btn-open-tbl">Abrir</a>
                        </td>
                    </tr>
                    <tr style="background: #eff6ff;">
                        <td style="font-weight: 700; color: var(--primary);">09:30 - 09:50</td>
                        <td style="font-weight: 700; color: var(--primary);">
                            María Elena Ramos <span style="font-size: 11px; background: var(--primary); color: #ffffff; padding: 2px 8px; border-radius: 10px; margin-left: 6px;">Turno Actual</span>
                        </td>
                        <td style="color: var(--text-gray);">DNI 44102931</td>
                        <td>Evaluación postoperatoria</td>
                        <td>Reserva Web</td>
                        <td><span class="badge-status badge-espera">• En Espera</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=2" class="btn-open-tbl" style="background: var(--primary); color: #ffffff;">Atender</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">10:00 - 10:20</td>
                        <td style="font-weight: 600;">Carlos Fernando Soto</td>
                        <td style="color: var(--text-gray);">DNI 10928374</td>
                        <td>Control de arritmia</td>
                        <td>Lista de Espera</td>
                        <td><span class="badge-status badge-confirmada">Confirmada</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=3" class="btn-open-tbl">Abrir</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">10:30 - 10:50</td>
                        <td style="font-weight: 600;">Lucía Alejandra Vega</td>
                        <td style="color: var(--text-gray);">DNI 55829104</td>
                        <td>Lectura de holter</td>
                        <td>Reserva Web</td>
                        <td><span class="badge-status badge-confirmada">Confirmada</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=4" class="btn-open-tbl">Abrir</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">11:00 - 11:20</td>
                        <td style="font-weight: 600;">Roberto Mario Castro</td>
                        <td style="color: var(--text-gray);">DNI 06291823</td>
                        <td>Evaluación prequirúrgica</td>
                        <td>Cupo Recuperado</td>
                        <td><span class="badge-status badge-confirmada">Confirmada</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=5" class="btn-open-tbl">Abrir</a>
                        </td>
                    </tr>
                    <tr>
                        <td style="font-weight: 700;">11:30 - 11:50</td>
                        <td style="font-weight: 600;">Patricia Elena Domínguez</td>
                        <td style="color: var(--text-gray);">DNI 32918231</td>
                        <td>Hipertensión arterial control</td>
                        <td>Reserva Web</td>
                        <td><span class="badge-status badge-confirmada">Confirmada</span></td>
                        <td style="text-align: right;">
                            <a href="${pageContext.request.contextPath}/medico/atencion?idCita=6" class="btn-open-tbl">Abrir</a>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- FOOTER ACTIONS -->
        <div class="footer-action-bar">
            <div style="display: flex; gap: 12px;">
                <button class="btn-action-foot" onclick="alert('Exportando agenda en PDF/Texto...');">
                    <i data-lucide="download" style="width: 16px;"></i> Exportar Agenda (PDF)
                </button>
                <button class="btn-action-foot" onclick="window.print();">
                    <i data-lucide="printer" style="width: 16px;"></i> Imprimir Hoja de Trabajo
                </button>
            </div>
            <div style="font-size: 13px; color: var(--text-gray); font-weight: 500;">
                Mostrando 6 de 6 citas para la jornada
            </div>
        </div>
    </main>

    <script>
        lucide.createIcons();
    </script>
</body>
</html>
