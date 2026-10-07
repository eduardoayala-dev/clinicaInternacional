<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pe.edu.utp.clinica.model.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
    String nombrePaciente = (String) request.getAttribute("nombrePaciente");
    if (nombrePaciente == null && usuario != null) {
        nombrePaciente = usuario.getNombres() + " " + usuario.getApellidos();
    }
    if (nombrePaciente == null) nombrePaciente = "Paciente Registrado";

    String inicialesPaciente = "P";
    if (nombrePaciente != null && !nombrePaciente.trim().isEmpty()) {
        String[] partes = nombrePaciente.trim().split("\\s+");
        if (partes.length >= 2) {
            inicialesPaciente = partes[0].substring(0, 1).toUpperCase() + partes[1].substring(0, 1).toUpperCase();
        } else {
            inicialesPaciente = partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Directorio de Especialistas - Clínica Internacional</title>
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
        
        .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
        .header-title { font-size: 26px; font-weight: 700; color: var(--text-dark); margin-bottom: 4px; letter-spacing: -0.5px; }
        .header-subtitle { font-size: 14px; color: var(--text-gray); }

        .btn-bell {
            width: 44px; height: 44px; border-radius: 12px; background: #ffffff; border: 1px solid var(--border-light);
            display: flex; align-items: center; justify-content: center; color: var(--text-gray); position: relative;
            text-decoration: none; transition: all 0.2s;
        }
        .btn-bell:hover { background: #f8fafc; color: var(--text-dark); }

        /* SEARCH & CATS */
        .pricing-banner {
            background: #ffffff; border: 1px solid #bfdbfe; border-left: 4px solid var(--primary);
            padding: 12px 18px; border-radius: var(--radius-md); margin-bottom: 20px;
            display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px; color: #1e40af;
        }
        .pricing-banner-text { font-weight: 500; }
        .pricing-badge { background: #eff6ff; padding: 4px 10px; border-radius: 20px; font-weight: 700; color: var(--primary); font-size: 12px; }

        .search-container { position: relative; max-width: 480px; margin-bottom: 20px; }
        .search-container input {
            width: 100%; padding: 12px 16px 12px 42px; border-radius: 30px; border: 1px solid var(--border-light);
            background: #ffffff; font-size: 14px; outline: none; transition: all 0.2s;
        }
        .search-container input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
        .search-container i, .search-container svg { position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: var(--text-light); pointer-events: none; }

        .cat-pills { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 24px; }
        .cat-btn {
            padding: 8px 16px; border-radius: 20px; border: 1px solid var(--border-light); background: #ffffff;
            font-size: 13px; font-weight: 500; color: var(--text-gray); cursor: pointer; white-space: nowrap; transition: all 0.2s;
        }
        .cat-btn.active, .cat-btn:hover { background: var(--primary-light); color: var(--primary); border-color: var(--primary-light); font-weight: 600; }

        /* DOCTORS GRID */
        .doctors-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
        .doctor-card {
            background: #ffffff; border-radius: var(--radius-lg); border: 1px solid var(--border-light);
            overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.02); transition: all 0.2s; display: flex; flex-direction: column;
        }
        .doctor-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.06); border-color: #cbd5e1; }

        .doc-banner {
            height: 100px; background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
            position: relative; padding: 12px; display: flex; justify-content: flex-end;
        }
        .badge-avail {
            padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; height: fit-content;
        }
        .badge-green { background: #dcfce7; color: #166534; }
        .badge-gray { background: #f1f5f9; color: #475569; }

        .doc-avatar-large {
            width: 52px; height: 52px; border-radius: 12px; background: #ffffff;
            border: 3px solid #ffffff; position: absolute; left: 16px; bottom: -18px;
            display: flex; align-items: center; justify-content: center; font-weight: 700; color: var(--primary);
            box-shadow: 0 4px 10px rgba(0,0,0,0.08); font-size: 18px;
        }

        .doc-card-body { padding: 28px 18px 18px 18px; flex: 1; display: flex; flex-direction: column; }
        .doc-rating { display: flex; align-items: center; gap: 4px; font-size: 12px; font-weight: 700; color: var(--text-dark); margin-bottom: 6px; }
        .doc-rating i { color: #f59e0b; width: 14px; fill: #f59e0b; }
        
        .doc-card-name { font-size: 15px; font-weight: 700; color: var(--text-dark); margin-bottom: 2px; }
        .doc-card-spec { font-size: 13px; font-weight: 600; color: var(--primary); margin-bottom: 4px; }
        .doc-card-location { font-size: 12px; color: var(--text-gray); margin-bottom: 12px; }

        .doc-card-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-gray); margin-bottom: 16px; border-top: 1px dashed var(--border-light); padding-top: 10px; }
        .spec-price-tag { font-weight: 700; color: #166534; background: #f0fdf4; padding: 4px 8px; border-radius: 6px; font-size: 12px; border: 1px solid #bbf7d0; }

        .btn-reserve {
            width: 100%; padding: 10px; border-radius: 20px; background: var(--primary-light); color: var(--primary);
            font-size: 13px; font-weight: 600; border: none; cursor: pointer; text-decoration: none; text-align: center;
            transition: all 0.2s; display: block; margin-top: auto;
        }
        .btn-reserve:hover { background: var(--primary); color: #ffffff; }

        @media(max-width:768px) { body { flex-direction: column; overflow-y: auto; } .sidebar { width:100%; } .main-content { padding:24px 16px; } }
    </style>
</head>
<body>

    <aside class="sidebar">
        <div class="brand-container">
            <img src="${pageContext.request.contextPath}/assets/images/logoClinical.png" alt="Clínica Internacional">
        </div>
        <nav class="nav-menu">
            <a href="${pageContext.request.contextPath}/paciente/dashboard" class="nav-item">
                <i data-lucide="home" style="width: 18px;"></i> Inicio
            </a>
            <a href="${pageContext.request.contextPath}/paciente/panel" class="nav-item">
                <i data-lucide="layout-dashboard" style="width: 18px;"></i> Panel
            </a>
            <a href="${pageContext.request.contextPath}/paciente/medicos" class="nav-item active">
                <i data-lucide="users" style="width: 18px;"></i> Médicos
            </a>
            <a href="${pageContext.request.contextPath}/paciente/citas" class="nav-item">
                <i data-lucide="calendar" style="width: 18px;"></i> Citas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/historial" class="nav-item">
                <i data-lucide="clipboard-list" style="width: 18px;"></i> Historial
            </a>
            <a href="${pageContext.request.contextPath}/paciente/reportes" class="nav-item">
                <i data-lucide="bar-chart-2" style="width: 18px;"></i> Reportes
            </a>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="nav-item">
                <i data-lucide="bell" style="width: 18px;"></i> Alertas
            </a>
            <a href="${pageContext.request.contextPath}/paciente/perfil" class="nav-item">
                <i data-lucide="user" style="width: 18px;"></i> Mi perfil
            </a>
        </nav>
        <a href="${pageContext.request.contextPath}/logout" class="user-profile-bottom">
            <div class="avatar-initials"><%= inicialesPaciente %></div>
            <div style="flex: 1;">
                <div class="user-name-sm"><%= nombrePaciente %></div>
                <div class="user-role-sm">Paciente</div>
            </div>
            <i data-lucide="log-out" style="width: 16px; color: var(--text-gray);"></i>
        </a>
    </aside>

    <main class="main-content">
        <div class="header-row">
            <div>
                <h1 class="header-title">Directorio de Especialistas</h1>
                <p class="header-subtitle">Encuentra el médico de tu elección con tarifas estandarizadas por especialidad.</p>
            </div>
            <a href="${pageContext.request.contextPath}/paciente/notificaciones" class="btn-bell" title="Alertas">
                <i data-lucide="bell" style="width: 20px;"></i>
            </a>
        </div>

        <div class="pricing-banner">
            <div class="pricing-banner-text">
                <i data-lucide="tag" style="width: 16px; display: inline; vertical-align: middle; margin-right: 6px;"></i>
                <strong>Tarifas Transparentes por Especialidad:</strong> El costo de tu consulta se define por la especialidad seleccionada, garantizando igualdad de tarifas entre médicos.
            </div>
            <span class="pricing-badge">Sin recargos por médico</span>
        </div>

        <div class="search-container" role="search">
            <i data-lucide="search" style="width: 18px;"></i>
            <input type="text" id="medicoFilterInput" placeholder="Buscar médico por nombre o especialidad..." aria-label="Buscar médico por nombre o especialidad">
        </div>

        <div class="cat-pills" role="tablist">
            <button class="cat-btn active" onclick="filterCat(this, 'ALL')">Todas las Especialidades</button>
            <button class="cat-btn" onclick="filterCat(this, 'Cardiología')">Cardiología (S/ 80.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Neurología')">Neurología (S/ 85.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Medicina General')">Medicina General (S/ 60.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Traumatología')">Traumatología (S/ 80.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Dermatología')">Dermatología (S/ 75.00)</button>
            <button class="cat-btn" onclick="filterCat(this, 'Ginecología')">Ginecología (S/ 80.00)</button>
        </div>

        <div class="doctors-grid" id="medicosCardsContainer">
            
            <!-- Doctor 1 -->
            <div class="doctor-card" data-spec="Cardiología">
                <div class="doc-banner">
                    <span class="badge-avail badge-green">• Disponible hoy</span>
                    <div class="doc-avatar-large">MR</div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating"><i data-lucide="star"></i> 4.9 <span style="color: var(--text-light); font-weight: normal;">(312)</span></div>
                    <div class="doc-card-name">Dra. María Rodríguez</div>
                    <div class="doc-card-spec">Cardiología</div>
                    <div class="doc-card-location">Clínica Internacional — Sede Central</div>
                    
                    <div class="doc-card-meta">
                        <span>14 años exp.</span>
                        <span class="spec-price-tag">S/ 80.00 / consulta</span>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-reserve">Reservar cita - Hoy 15:00</a>
                </div>
            </div>

            <!-- Doctor 2 -->
            <div class="doctor-card" data-spec="Cardiología">
                <div class="doc-banner">
                    <span class="badge-avail badge-green">• Disponible hoy</span>
                    <div class="doc-avatar-large">CM</div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating"><i data-lucide="star"></i> 4.8 <span style="color: var(--text-light); font-weight: normal;">(241)</span></div>
                    <div class="doc-card-name">Dr. Carlos Mendoza</div>
                    <div class="doc-card-spec">Cardiología</div>
                    <div class="doc-card-location">Clínica Internacional — San Borja</div>
                    
                    <div class="doc-card-meta">
                        <span>10 años exp.</span>
                        <span class="spec-price-tag">S/ 80.00 / consulta</span>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-reserve">Reservar cita - Hoy 17:00</a>
                </div>
            </div>

            <!-- Doctor 3 -->
            <div class="doctor-card" data-spec="Neurología">
                <div class="doc-banner">
                    <span class="badge-avail badge-gray">• Próx. disponible</span>
                    <div class="doc-avatar-large">AV</div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating"><i data-lucide="star"></i> 4.7 <span style="color: var(--text-light); font-weight: normal;">(189)</span></div>
                    <div class="doc-card-name">Dr. Alejandro Vargas</div>
                    <div class="doc-card-spec">Neurología</div>
                    <div class="doc-card-location">Clínica Internacional — Miraflores</div>
                    
                    <div class="doc-card-meta">
                        <span>8 años exp.</span>
                        <span class="spec-price-tag">S/ 85.00 / consulta</span>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-reserve">Reservar cita - Mañana 09:00</a>
                </div>
            </div>

            <!-- Doctor 4 -->
            <div class="doctor-card" data-spec="Neurología">
                <div class="doc-banner">
                    <span class="badge-avail badge-green">• Disponible hoy</span>
                    <div class="doc-avatar-large">SB</div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating"><i data-lucide="star"></i> 4.9 <span style="color: var(--text-light); font-weight: normal;">(198)</span></div>
                    <div class="doc-card-name">Dra. Sofia Benítez</div>
                    <div class="doc-card-spec">Neurología</div>
                    <div class="doc-card-location">Clínica Internacional — Sede Central</div>
                    
                    <div class="doc-card-meta">
                        <span>11 años exp.</span>
                        <span class="spec-price-tag">S/ 85.00 / consulta</span>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-reserve">Reservar cita - Hoy 16:30</a>
                </div>
            </div>

            <!-- Doctor 5 -->
            <div class="doctor-card" data-spec="Medicina General">
                <div class="doc-banner">
                    <span class="badge-avail badge-green">• Disponible hoy</span>
                    <div class="doc-avatar-large">FA</div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating"><i data-lucide="star"></i> 4.8 <span style="color: var(--text-light); font-weight: normal;">(156)</span></div>
                    <div class="doc-card-name">Dr. Fernando Alvarez</div>
                    <div class="doc-card-spec">Medicina General</div>
                    <div class="doc-card-location">Clínica Internacional — San Borja</div>
                    
                    <div class="doc-card-meta">
                        <span>9 años exp.</span>
                        <span class="spec-price-tag">S/ 60.00 / consulta</span>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-reserve">Reservar cita - Hoy 14:00</a>
                </div>
            </div>

            <!-- Doctor 6 -->
            <div class="doctor-card" data-spec="Traumatología">
                <div class="doc-banner">
                    <span class="badge-avail badge-gray">• Próx. disponible</span>
                    <div class="doc-avatar-large">JH</div>
                </div>
                <div class="doc-card-body">
                    <div class="doc-rating"><i data-lucide="star"></i> 4.6 <span style="color: var(--text-light); font-weight: normal;">(134)</span></div>
                    <div class="doc-card-name">Dr. Jorge Herrera</div>
                    <div class="doc-card-spec">Traumatología</div>
                    <div class="doc-card-location">Clínica Internacional — Miraflores</div>
                    
                    <div class="doc-card-meta">
                        <span>7 años exp.</span>
                        <span class="spec-price-tag">S/ 80.00 / consulta</span>
                    </div>

                    <a href="${pageContext.request.contextPath}/paciente/citas" class="btn-reserve">Reservar cita - Mañana 11:30</a>
                </div>
            </div>

        </div>
    </main>

    <script>
        lucide.createIcons();

        function filterCat(btn, cat) {
            document.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const cards = document.querySelectorAll('#medicosCardsContainer .doctor-card');
            cards.forEach(card => {
                if (cat === 'ALL' || card.getAttribute('data-spec') === cat) {
                    card.style.display = 'flex';
                } else {
                    card.style.display = 'none';
                }
            });
        }

        document.getElementById('medicoFilterInput').addEventListener('input', function(e) {
            const term = e.target.value.toLowerCase();
            const cards = document.querySelectorAll('#medicosCardsContainer .doctor-card');
            cards.forEach(card => {
                const text = card.textContent.toLowerCase();
                card.style.display = text.includes(term) ? 'flex' : 'none';
            });
        });
    </script>
</body>
</html>
