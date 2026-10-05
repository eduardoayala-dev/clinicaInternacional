<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null) nombrePaciente = "Juan Alberto Pérez";
    
    Map<String, String> ofertaAnticipada = (Map<String, String>) request.getAttribute("ofertaAnticipada");
    Map<String, String> proximaCita = (Map<String, String>) request.getAttribute("proximaCita");
    Map<String, String> listaEspera = (Map<String, String>) request.getAttribute("listaEspera");
    List<Map<String, String>> listaProximasCitas = (List<Map<String, String>>) request.getAttribute("listaProximasCitas");
    Map<String, Integer> stats = (Map<String, Integer>) request.getAttribute("stats");
    String ultimoAcceso = (String) request.getAttribute("ultimoAcceso");
    String ipAcceso = (String) request.getAttribute("ipAcceso");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal del Paciente - Clínica Internacional</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    
    <style>
        :root {
            --primary: #0052cc;
            --primary-hover: #0041a8;
            --primary-light: #eef5ff;
            --navy-dark: #0f172a;
            --slate-600: #475569;
            --slate-400: #94a3b8;
            --slate-200: #e2e8f0;
            --slate-100: #f1f5f9;
            --bg-page: #f8fafc;
            --card-bg: #ffffff;
            --emerald: #10b981;
            --emerald-light: #ecfdf5;
            --amber: #f59e0b;
            --amber-light: #fffbeb;
            --rose: #f43f5e;
            --rose-light: #fff1f2;
            --radius-lg: 14px;
            --radius-md: 10px;
            --radius-sm: 6px;
            --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.05);
            --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -1px rgba(0, 0, 0, 0.04);
            --max-width: 1440px;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Inter', system-ui, -apple-system, sans-serif;
        }

        body {
            background-color: var(--bg-page);
            color: var(--navy-dark);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* Layout Main Wrapper */
        .app-layout {
            display: flex;
            min-height: 100vh;
            max-width: var(--max-width);
            margin: 0 auto;
            width: 100%;
            background-color: #ffffff;
            box-shadow: 0 0 30px rgba(0,0,0,0.03);
        }

        /* Sidebar Navigation */
        .sidebar {
            width: 260px;
            background-color: #ffffff;
            border-right: 1px solid var(--slate-200);
            display: flex;
            flex-direction: column;
            padding: 24px 16px;
            flex-shrink: 0;
        }

        .brand-container {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 0 12px 28px 12px;
            border-bottom: 1px solid var(--slate-100);
            margin-bottom: 20px;
        }

        .brand-logo-img {
            height: 38px;
            width: auto;
            object-fit: contain;
        }

        .brand-title {
            font-size: 15px;
            font-weight: 700;
            color: var(--primary);
            line-height: 1.2;
        }

        .brand-subtitle {
            font-size: 11px;
            color: var(--slate-600);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .nav-menu {
            display: flex;
            flex-direction: column;
            gap: 4px;
            flex-grow: 1;
        }

        .nav-item {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 12px 16px;
            border-radius: var(--radius-md);
            color: var(--slate-600);
            text-decoration: none;
            font-size: 14px;
            font-weight: 500;
            transition: all 0.2s ease;
        }

        .nav-item:hover {
            background-color: var(--slate-100);
            color: var(--primary);
        }

        .nav-item.active {
            background-color: var(--primary-light);
            color: var(--primary);
            font-weight: 600;
        }

        .nav-icon {
            width: 20px;
            height: 20px;
            stroke: currentColor;
            stroke-width: 2;
            fill: none;
        }

        .nav-badge {
            margin-left: auto;
            background-color: var(--rose);
            color: white;
            font-size: 11px;
            font-weight: 700;
            padding: 2px 7px;
            border-radius: 10px;
        }

        /* Sidebar Profile Card */
        .sidebar-profile {
            padding-top: 16px;
            border-top: 1px solid var(--slate-200);
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: var(--primary-light);
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 14px;
            border: 2px solid #ffffff;
            box-shadow: 0 0 0 1px var(--slate-200);
        }

        .profile-info {
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .profile-name {
            font-size: 13px;
            font-weight: 600;
            color: var(--navy-dark);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .profile-role {
            font-size: 11px;
            color: var(--slate-600);
        }

        /* Main Content Area */
        .main-content {
            flex-grow: 1;
            background-color: var(--bg-page);
            padding: 32px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 24px;
        }

        /* Top Header */
        .header-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background-color: transparent;
        }

        .header-greeting {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .date-chip {
            font-size: 13px;
            color: var(--slate-600);
            font-weight: 500;
        }

        .user-title {
            font-size: 26px;
            font-weight: 700;
            color: var(--navy-dark);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .user-subtitle {
            font-size: 14px;
            color: var(--slate-600);
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .btn-primary {
            background-color: var(--primary);
            color: white;
            padding: 10px 20px;
            border-radius: var(--radius-md);
            text-decoration: none;
            font-weight: 600;
            font-size: 14px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            border: none;
            cursor: pointer;
            transition: background-color 0.2s ease;
            box-shadow: 0 2px 4px rgba(0, 82, 204, 0.2);
        }

        .btn-primary:hover {
            background-color: var(--primary-hover);
        }

        .btn-outline {
            background-color: white;
            color: var(--navy-dark);
            border: 1px solid var(--slate-200);
            padding: 10px 16px;
            border-radius: var(--radius-md);
            font-weight: 500;
            font-size: 14px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .btn-outline:hover {
            background-color: var(--slate-100);
            border-color: var(--slate-300);
        }

        /* BANNER ALERTA SISTEMA (WIREFRAME SPEC) */
        .system-alert-banner {
            background: linear-gradient(90deg, #1e293b 0%, #0f172a 100%);
            color: white;
            border-radius: var(--radius-lg);
            padding: 16px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            box-shadow: var(--shadow-md);
            border-left: 4px solid var(--amber);
        }

        .alert-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .alert-icon-box {
            background-color: rgba(245, 158, 11, 0.15);
            color: var(--amber);
            width: 36px;
            height: 36px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .alert-text {
            font-size: 13.5px;
            line-height: 1.4;
        }

        .alert-code {
            font-weight: 700;
            color: #fcd34d;
            letter-spacing: 0.5px;
        }

        .alert-action-btn {
            background-color: var(--amber);
            color: #0f172a;
            padding: 8px 16px;
            border-radius: var(--radius-md);
            font-weight: 700;
            font-size: 12px;
            text-decoration: none;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            white-space: nowrap;
            transition: opacity 0.2s ease;
        }

        .alert-action-btn:hover {
            opacity: 0.9;
        }

        /* CARD OFERTA REAGENDAMIENTO ANTICIPADO (WIREFRAME SPEC) */
        .featured-offer-card {
            background-color: #ffffff;
            border: 1px solid #fed7aa;
            border-radius: var(--radius-lg);
            padding: 24px;
            box-shadow: 0 4px 12px rgba(245, 158, 11, 0.08);
            position: relative;
            overflow: hidden;
        }

        .featured-offer-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
            background: linear-gradient(90deg, #f59e0b, #ec4899);
        }

        .offer-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 16px;
            gap: 16px;
        }

        .offer-badge {
            background-color: #fff7ed;
            color: #c2410c;
            border: 1px solid #ffedd5;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.5px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .timer-box {
            background-color: #fef2f2;
            border: 1px solid #fee2e2;
            padding: 8px 14px;
            border-radius: var(--radius-md);
            text-align: right;
        }

        .timer-label {
            font-size: 10px;
            color: #991b1b;
            text-transform: uppercase;
            font-weight: 700;
            letter-spacing: 0.5px;
        }

        .timer-value {
            font-size: 16px;
            font-weight: 800;
            color: #dc2626;
            font-family: monospace;
        }

        .offer-description {
            font-size: 14px;
            color: var(--slate-600);
            margin-bottom: 20px;
            line-height: 1.5;
        }

        .offer-details-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
            background-color: var(--slate-100);
            padding: 16px;
            border-radius: var(--radius-md);
            margin-bottom: 20px;
        }

        .detail-item-label {
            font-size: 11px;
            text-transform: uppercase;
            color: var(--slate-600);
            font-weight: 600;
            margin-bottom: 4px;
        }

        .detail-item-value {
            font-size: 14px;
            font-weight: 700;
            color: var(--navy-dark);
        }

        .offer-actions {
            display: flex;
            justify-content: flex-end;
            gap: 12px;
        }

        .btn-accent {
            background-color: var(--navy-dark);
            color: white;
            padding: 10px 20px;
            border-radius: var(--radius-md);
            font-weight: 600;
            font-size: 13px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .btn-accent:hover {
            background-color: #1e293b;
        }

        /* Metric KPI Summary Cards (Mockup) */
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 16px;
        }

        .metric-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            padding: 20px;
            border: 1px solid var(--slate-200);
            box-shadow: var(--shadow-sm);
            display: flex;
            flex-direction: column;
            gap: 12px;
            position: relative;
        }

        .metric-top {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .metric-icon-wrapper {
            width: 42px;
            height: 42px;
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .metric-icon-wrapper.blue { background-color: #e0f2fe; color: #0284c7; }
        .metric-icon-wrapper.purple { background-color: #f3e8ff; color: #9333ea; }
        .metric-icon-wrapper.amber { background-color: #fef3c7; color: #d97706; }
        .metric-icon-wrapper.emerald { background-color: #d1fae5; color: #059669; }

        .metric-pill {
            font-size: 11px;
            font-weight: 600;
            padding: 3px 8px;
            border-radius: 12px;
            text-transform: lowercase;
        }

        .metric-pill.blue { background-color: #e0f2fe; color: #0369a1; }
        .metric-pill.purple { background-color: #f3e8ff; color: #7e22ce; }
        .metric-pill.amber { background-color: #fef3c7; color: #b45309; }
        .metric-pill.emerald { background-color: #d1fae5; color: #047857; }

        .metric-number {
            font-size: 32px;
            font-weight: 800;
            color: var(--navy-dark);
            line-height: 1;
        }

        .metric-label {
            font-size: 13px;
            color: var(--slate-600);
            font-weight: 500;
        }

        /* 2-Column Dashboard Body Grid */
        .dashboard-grid {
            display: grid;
            grid-template-columns: 1.8fr 1fr;
            gap: 24px;
        }

        /* Modules Section Cards */
        .content-card {
            background-color: var(--card-bg);
            border-radius: var(--radius-lg);
            border: 1px solid var(--slate-200);
            box-shadow: var(--shadow-sm);
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .card-header-flex {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .card-title {
            font-size: 16px;
            font-weight: 700;
            color: var(--navy-dark);
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .card-link {
            color: var(--primary);
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .card-link:hover {
            text-decoration: underline;
        }

        /* Appointment Detail Box (Wireframe + Mockup) */
        .appointment-item {
            background-color: #f8fafc;
            border: 1px solid var(--slate-200);
            border-radius: var(--radius-md);
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            transition: border-color 0.2s ease;
        }

        .appointment-item.featured {
            background-color: #f0f7ff;
            border-color: #bfdbfe;
        }

        .appointment-top {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .doctor-profile-flex {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .doctor-avatar-circle {
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background-color: #e2e8f0;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            color: var(--slate-600);
            border: 2px solid white;
            box-shadow: var(--shadow-sm);
        }

        .doctor-meta {
            display: flex;
            flex-direction: column;
        }

        .doctor-name {
            font-size: 15px;
            font-weight: 700;
            color: var(--navy-dark);
        }

        .doctor-spec {
            font-size: 13px;
            color: var(--primary);
            font-weight: 600;
        }

        .badge-status {
            font-size: 11px;
            font-weight: 700;
            padding: 4px 10px;
            border-radius: 20px;
            text-transform: uppercase;
        }

        .badge-status.confirmed { background-color: #dcfce7; color: #15803d; }
        .badge-status.pending { background-color: #fef3c7; color: #b45309; }

        .appointment-info-row {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 12px;
            background-color: white;
            padding: 12px 16px;
            border-radius: var(--radius-sm);
            border: 1px solid var(--slate-200);
        }

        .info-pill-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 12.5px;
            color: var(--slate-600);
            font-weight: 500;
        }

        .info-pill-item svg {
            width: 16px;
            height: 16px;
            stroke: var(--slate-400);
            flex-shrink: 0;
        }

        .protocol-box {
            background-color: #fffbe6;
            border-left: 3px solid var(--amber);
            padding: 10px 14px;
            border-radius: 4px;
            font-size: 12px;
            color: #78350f;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        /* Waitlist Module (Wireframe Modulo 02) */
        .waitlist-card {
            background-color: #faf5ff;
            border: 1px solid #e9d5ff;
            border-radius: var(--radius-md);
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 14px;
        }

        .waitlist-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .waitlist-tag {
            background-color: #7e22ce;
            color: white;
            font-size: 11px;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 4px;
            letter-spacing: 0.5px;
        }

        .waitlist-turn-highlight {
            font-size: 18px;
            font-weight: 800;
            color: #6b21a8;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .waitlist-params {
            display: flex;
            flex-direction: column;
            gap: 6px;
            font-size: 12.5px;
            color: var(--slate-600);
            border-top: 1px dashed #d8b4fe;
            padding-top: 12px;
        }

        .waitlist-param-line {
            display: flex;
            justify-content: space-between;
        }

        .waitlist-param-val {
            font-weight: 600;
            color: var(--navy-dark);
        }

        /* Quick Actions Grid (Wireframe Spec) */
        .quick-actions-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .quick-action-card {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background-color: #f8fafc;
            border: 1px solid var(--slate-200);
            padding: 16px 20px;
            border-radius: var(--radius-md);
            text-decoration: none;
            color: var(--navy-dark);
            transition: all 0.2s ease;
        }

        .quick-action-card:hover {
            background-color: var(--primary-light);
            border-color: #bfdbfe;
            transform: translateY(-1px);
        }

        .action-left {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .action-icon-circle {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--primary);
            border: 1px solid var(--slate-200);
        }

        .action-title {
            font-size: 14px;
            font-weight: 700;
            display: block;
        }

        .action-desc {
            font-size: 12px;
            color: var(--slate-600);
        }

        .action-arrow {
            width: 18px;
            height: 18px;
            stroke: var(--slate-400);
        }

        /* Privacy Footer Audit */
        .privacy-footer {
            margin-top: 24px;
            padding-top: 20px;
            border-top: 1px solid var(--slate-200);
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
            color: var(--slate-600);
        }

        .privacy-shield {
            display: flex;
            align-items: center;
            gap: 6px;
            color: #059669;
            font-weight: 600;
        }

        /* Responsiveness */
        @media (max-width: 1024px) {
            .app-layout {
                flex-direction: column;
            }

            .sidebar {
                width: 100%;
                border-right: none;
                border-bottom: 1px solid var(--slate-200);
                padding: 16px;
            }

            .nav-menu {
                flex-direction: row;
                overflow-x: auto;
                padding-bottom: 8px;
            }

            .metrics-grid {
                grid-template-columns: repeat(2, 1fr);
            }

            .dashboard-grid {
                grid-template-columns: 1fr;
            }
        }

        @media (max-width: 640px) {
            .main-content {
                padding: 16px;
            }

            .metrics-grid {
                grid-template-columns: 1fr;
            }

            .appointment-info-row {
                grid-template-columns: 1fr;
            }

            .offer-details-grid {
                grid-template-columns: 1fr;
            }

            .header-bar {
                flex-direction: column;
                align-items: flex-start;
                gap: 16px;
            }
        }
    </style>
</head>
<body>

<div class="app-layout">

    <!-- SIDEBAR DE NAVEGACION -->
    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/isotipoClinica.png" alt="Clinica Internacional" class="brand-logo-img" onerror="this.src='${pageContext.request.contextPath}/assets/images/logoClinical.png'">
            <div>
                <div class="brand-title">CLINICA INTERNACIONAL</div>
                <div class="brand-subtitle">Portal del Paciente</div>
            </div>
        </div>

        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item active">
                <svg class="nav-icon" viewBox="0 0 24 24"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                Inicio
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <svg class="nav-icon" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
                Mis Citas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="nav-item">
                <svg class="nav-icon" viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                Lista de Espera
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
                <svg class="nav-icon" viewBox="0 0 24 24"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
                Alertas & Avisos
                <span class="nav-badge">1</span>
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <svg class="nav-icon" viewBox="0 0 24 24"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                Mi Perfil
            </a>
        </nav>

        <div class="sidebar-profile">
            <div class="avatar">JP</div>
            <div class="profile-info">
                <span class="profile-name"><%= nombrePaciente %></span>
                <span class="profile-role">Paciente Titular</span>
            </div>
        </div>
    </aside>

    <!-- AREA PRINCIPAL CONTENIDO -->
    <main class="main-content">

        <!-- CABECERA SUPERIOR -->
        <header class="header-bar">
            <div class="header-greeting">
                <span class="date-chip">Lunes, 07 de Septiembre 2026</span>
                <h1 class="user-title">Buenos días, <%= nombrePaciente.split(" ")[0] %></h1>
                <p class="user-subtitle">Tienes 2 citas programadas y 1 aviso prioritario de cupo disponible.</p>
            </div>
            <div class="header-actions">
                <a href="${pageContext.request.contextPath}/paciente/citas?action=nueva" class="btn-primary">
                    <svg style="width: 18px; height: 18px; stroke: currentColor; fill: none; stroke-width: 2.5;" viewBox="0 0 24 24"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    Programar cita
                </a>
                <a href="${pageContext.request.contextPath}/logout" class="btn-outline">
                    <svg style="width: 16px; height: 16px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                    Cerrar sesión
                </a>
            </div>
        </header>

        <!-- ALERTA SISTEMA (ESPECIFICACION WIREFRAME - BANNER SUPERIOR) -->
        <div class="system-alert-banner">
            <div class="alert-left">
                <div class="alert-icon-box">
                    <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                </div>
                <div class="alert-text">
                    <span class="alert-code">ALERTA_SISTEMA:</span> Tiene 1 oferta de cupo recuperado pendiente de confirmación en su bandeja. Tiempo límite de aceptación activo.
                </div>
            </div>
            <a href="#oferta-anticipada" class="alert-action-btn">Resolver ahora &gt;</a>
        </div>

        <!-- TARJETA DESTACADA: OFERTA CUPO ANTICIPADO POR CANCELACIÓN (WIREFRAME SPEC) -->
        <section id="oferta-anticipada" class="featured-offer-card">
            <div class="offer-header">
                <div>
                    <span class="offer-badge">
                        <svg style="width: 12px; height: 12px; stroke: currentColor; fill: none; stroke-width: 2.5;" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                        PRIORIDAD ALTA ; CUPO ANTICIPADO DISPONIBLE POR CANCELACIÓN!
                    </span>
                    <h3 style="font-size: 16px; font-weight: 700; margin-top: 8px;">¡Oportunidad de adelantamiento de cita!</h3>
                </div>
                <div class="timer-box">
                    <div class="timer-label">Caducidad de la oferta</div>
                    <div class="timer-value" id="countdown-timer">14m 32s</div>
                </div>
            </div>

            <p class="offer-description">
                Se ha liberado un cupo compatible con sus criterios clínicos vigentes. Si decide aceptar, su cita actual programada se reprogramará automáticamente a esta nueva fecha preferente sin costo adicional.
            </p>

            <div class="offer-details-grid">
                <div>
                    <div class="detail-item-label">Especialidad / Médico Asignado</div>
                    <div class="detail-item-value"><%= ofertaAnticipada.get("especialidad") %> - <%= ofertaAnticipada.get("medico") %></div>
                </div>
                <div>
                    <div class="detail-item-label">Nueva Fecha Ofrecida (Anticipada)</div>
                    <div class="detail-item-value" style="color: var(--primary);"><%= ofertaAnticipada.get("fechaOfrecida") %></div>
                </div>
            </div>

            <div class="offer-actions">
                <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-accent">
                    Revisar oferta en notificaciones
                    <svg style="width: 16px; height: 16px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                </a>
            </div>
        </section>

        <!-- KPI STAT CARDS (MOCKUP SPEC) -->
        <section class="metrics-grid">
            <div class="metric-card">
                <div class="metric-top">
                    <div class="metric-icon-wrapper blue">
                        <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
                    </div>
                    <span class="metric-pill blue">este mes</span>
                </div>
                <div class="metric-number"><%= stats.get("proximasCitas") %></div>
                <div class="metric-label">Próximas citas agendadas</div>
            </div>

            <div class="metric-card">
                <div class="metric-top">
                    <div class="metric-icon-wrapper purple">
                        <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
                    </div>
                    <span class="metric-pill purple">activos</span>
                </div>
                <div class="metric-number"><%= stats.get("medicosAsignados") %></div>
                <div class="metric-label">Médicos tratantes</div>
            </div>

            <div class="metric-card">
                <div class="metric-top">
                    <div class="metric-icon-wrapper amber">
                        <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                    </div>
                    <span class="metric-pill amber">revisión requerida</span>
                </div>
                <div class="metric-number"><%= stats.get("estudiosPendientes") %></div>
                <div class="metric-label">Estudios / Exámenes pendientes</div>
            </div>

            <div class="metric-card">
                <div class="metric-top">
                    <div class="metric-icon-wrapper emerald">
                        <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    </div>
                    <span class="metric-pill emerald">historial</span>
                </div>
                <div class="metric-number"><%= stats.get("consultasTotales") %></div>
                <div class="metric-label">Consultas totales realizadas</div>
            </div>
        </section>

        <!-- DASHBOARD GRID: COLUMNA IZQUIERDA (MODULOS DE CITAS) Y DERECHA (ACCIONES Y LISTA ESPERA) -->
        <div class="dashboard-grid">

            <!-- COLUMNA IZQUIERDA: MODULO 01 (PROXIMA CITA AGENDADA) Y LISTADO -->
            <div style="display: flex; flex-direction: column; gap: 24px;">

                <!-- MODULO 01: PROXIMA CITA AGENDADA (WIREFRAME SPEC) -->
                <section class="content-card">
                    <div class="card-header-flex">
                        <div class="card-title">
                            <svg style="width: 20px; height: 20px; stroke: var(--primary); fill: none; stroke-width: 2;" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
                            MÓDULO 01 :: PRÓXIMA CITA AGENDADA
                        </div>
                        <a href="${pageContext.request.contextPath}/paciente/citas" class="card-link">
                            Ver todas
                            <svg style="width: 14px; height: 14px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                        </a>
                    </div>

                    <!-- DETALLE CITA DESTACADA -->
                    <div class="appointment-item featured">
                        <div class="appointment-top">
                            <div class="doctor-profile-flex">
                                <div class="doctor-avatar-circle">CM</div>
                                <div class="doctor-meta">
                                    <span class="doctor-name"><%= proximaCita.get("medico") %></span>
                                    <span class="doctor-spec"><%= proximaCita.get("especialidad") %> &bull; <%= proximaCita.get("cmp") %></span>
                                </div>
                            </div>
                            <span class="badge-status confirmed">Estado: <%= proximaCita.get("estado") %></span>
                        </div>

                        <div class="appointment-info-row">
                            <div class="info-pill-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                                <span><%= proximaCita.get("fechaHora") %></span>
                            </div>
                            <div class="info-pill-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
                                <span><%= proximaCita.get("ubicacion") %></span>
                            </div>
                            <div class="info-pill-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                                <span>Modalidad: <strong><%= proximaCita.get("modalidad") %></strong></span>
                            </div>
                        </div>

                        <div class="protocol-box">
                            <svg style="width: 16px; height: 16px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                            <span><strong>PROTOCOLO:</strong> <%= proximaCita.get("protocolo") %></span>
                        </div>

                        <div style="display: flex; justify-content: flex-end; gap: 10px;">
                            <a href="#" class="btn-outline" style="font-size: 13px; padding: 8px 14px;">Reprogramar</a>
                            <a href="${pageContext.request.contextPath}/paciente/citas?id=<%= proximaCita.get("id") %>" class="btn-primary" style="font-size: 13px; padding: 8px 16px;">Ver detalle de cita</a>
                        </div>
                    </div>

                    <!-- CITA SECUNDARIA (MOCKUP LIST) -->
                    <% for (int i = 1; i < listaProximasCitas.size(); i++) { 
                        Map<String, String> cita = listaProximasCitas.get(i);
                    %>
                    <div class="appointment-item">
                        <div class="appointment-top">
                            <div class="doctor-profile-flex">
                                <div class="doctor-avatar-circle" style="background-color: #ede9fe; color: #6d28d9;">MO</div>
                                <div class="doctor-meta">
                                    <span class="doctor-name"><%= cita.get("medico") %></span>
                                    <span class="doctor-spec"><%= cita.get("especialidad") %> &bull; Telemedicina</span>
                                </div>
                            </div>
                            <span class="badge-status pending">Pendiente</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: var(--slate-600);">
                            <span><%= cita.get("fechaHora") %></span>
                            <a href="#" class="btn-primary" style="font-size: 12px; padding: 6px 14px;">Confirmar</a>
                        </div>
                    </div>
                    <% } %>

                </section>

            </div>

            <!-- COLUMNA DERECHA: MODULO 02 (ESTADO EN LISTA DE ESPERA) Y ACCIONES OPERATIVAS -->
            <div style="display: flex; flex-direction: column; gap: 24px;">

                <!-- MODULO 02: ESTADO SOLICITUDES & LISTA ESPERA (WIREFRAME SPEC) -->
                <section class="content-card">
                    <div class="card-header-flex">
                        <div class="card-title">
                            <svg style="width: 20px; height: 20px; stroke: #9333ea; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                            MÓDULO 02 :: LISTA ESPERA
                        </div>
                        <span style="font-size: 11px; font-weight: 700; color: #15803d; background-color: #dcfce7; padding: 3px 8px; border-radius: 4px;">REGISTRO ACTIVO</span>
                    </div>

                    <div class="waitlist-card">
                        <div class="waitlist-header">
                            <span class="waitlist-tag">SOLICITUD VIGENTE</span>
                            <div class="waitlist-turn-highlight">
                                TURNO ACTIVO <%= listaEspera.get("turnoActivo") %>
                            </div>
                        </div>

                        <div style="font-size: 14px; font-weight: 700; color: var(--navy-dark);">
                            Especialidad: <%= listaEspera.get("especialidad") %>
                        </div>

                        <div class="waitlist-params">
                            <div class="waitlist-param-line">
                                <span>Sede asignada:</span>
                                <span class="waitlist-param-val"><%= listaEspera.get("sede") %></span>
                            </div>
                            <div class="waitlist-param-line">
                                <span>Rango horario preferente:</span>
                                <span class="waitlist-param-val"><%= listaEspera.get("rangoHorario") %></span>
                            </div>
                            <div class="waitlist-param-line">
                                <span>Notificación vía:</span>
                                <span class="waitlist-param-val"><%= listaEspera.get("canalNotificacion") %></span>
                            </div>
                            <div class="waitlist-param-line">
                                <span>Prioridad triaje:</span>
                                <span class="waitlist-param-val"><%= listaEspera.get("prioridadTriaje") %></span>
                            </div>
                        </div>

                        <a href="${pageContext.request.contextPath}/paciente/lista-espera" class="btn-outline" style="justify-content: center; width: 100%; text-align: center; margin-top: 4px;">
                            Ver estado en lista de espera
                        </a>
                    </div>
                </section>

                <!-- ACCIONES OPERATIVAS RÁPIDAS (WIREFRAME SPEC) -->
                <section class="content-card">
                    <div class="card-title" style="font-size: 14px; color: var(--slate-600); text-transform: uppercase; letter-spacing: 0.5px;">
                        // Acciones operativas rápidas
                    </div>

                    <div class="quick-actions-list">
                        <a href="${pageContext.request.contextPath}/paciente/citas?action=nueva" class="quick-action-card">
                            <div class="action-left">
                                <div class="action-icon-circle">
                                    <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="12" x2="11" y1="16" y2="16"/><line x1="12" x2="12" y1="13" y2="19"/></svg>
                                </div>
                                <div>
                                    <span class="action-title">+ Buscar y Agendar Nueva Cita</span>
                                    <span class="action-desc">Explorar especialidades y profesionales</span>
                                </div>
                            </div>
                            <svg class="action-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                        </a>

                        <a href="${pageContext.request.contextPath}/paciente/citas" class="quick-action-card">
                            <div class="action-left">
                                <div class="action-icon-circle">
                                    <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
                                </div>
                                <div>
                                    <span class="action-title">Mis Citas Programadas</span>
                                    <span class="action-desc">Historial y consultas activas</span>
                                </div>
                            </div>
                            <svg class="action-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                        </a>

                        <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="quick-action-card">
                            <div class="action-left">
                                <div class="action-icon-circle">
                                    <svg style="width: 20px; height: 20px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                                </div>
                                <div>
                                    <span class="action-title">Bandeja de Avisos (1)</span>
                                    <span class="action-desc">Ofertas y alertas pendientes</span>
                                </div>
                            </div>
                            <svg class="action-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                        </a>
                    </div>
                </section>

            </div>

        </div>

        <!-- PIE DE PAGINA CON PRIVACIDAD DE DATOS (LEY N° 29733 Y HIPAA) -->
        <footer class="privacy-footer">
            <div class="privacy-shield">
                <svg style="width: 16px; height: 16px; stroke: currentColor; fill: none; stroke-width: 2;" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                <span>Conexión cifrada de grado médico (Ley N° 29733 - Protección de Datos Personales)</span>
            </div>
            <div>
                Última actividad: <%= ultimoAcceso %> (IP: <%= ipAcceso %>)
            </div>
        </footer>

    </main>

</div>

<script>
    // Contador dinámico para la oferta de cupo anticipado (14m 32s)
    (function() {
        let totalSeconds = parseInt("<%= ofertaAnticipada.get("segundosRestantes") %>", 10) || 872;
        const timerElement = document.getElementById("countdown-timer");

        function updateTimer() {
            if (totalSeconds <= 0) {
                timerElement.textContent = "OFERTA EXPIRADA";
                timerElement.style.color = "#991b1b";
                return;
            }
            const minutes = Math.floor(totalSeconds / 60);
            const seconds = totalSeconds % 60;
            const formattedMinutes = minutes < 10 ? "0" + minutes : minutes;
            const formattedSeconds = seconds < 10 ? "0" + seconds : seconds;
            timerElement.textContent = `${formattedMinutes}m ${formattedSeconds}s`;
            totalSeconds--;
        }

        updateTimer();
        setInterval(updateTimer, 1000);
    })();
</script>

</body>
</html>
