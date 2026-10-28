document.addEventListener('DOMContentLoaded', () => {
    const filas = [...document.querySelectorAll('#tablaAgenda tr[data-cita]')];
    const buscar = document.getElementById('searchPaciente');
    const anterior = document.getElementById('medPaginaAnterior');
    const siguiente = document.getElementById('medPaginaSiguiente');
    const estado = document.getElementById('medPaginaEstado');
    const vacio = document.getElementById('medAgendaSinResultados');
    const formulario = document.getElementById('medAgendaFiltros');
    const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
    const registros = filas.map(fila => ({fila, texto: normalizar(`${fila.dataset.cita} ${fila.textContent}`)}));
    const calendario = document.getElementById('medCalendario');
    const vista = calendario.dataset.vista;
    const esCalendario = vista !== 'dia';
    const dias = document.getElementById('medCalendarioDias');
    const fechaUTC = valor => new Date(`${valor}T12:00:00Z`);
    const iso = fecha => fecha.toISOString().slice(0, 10);
    const sumarDias = (valor, cantidad) => {
        const fecha = fechaUTC(valor);
        fecha.setUTCDate(fecha.getUTCDate() + cantidad);
        return iso(fecha);
    };
    const formato = opciones => new Intl.DateTimeFormat('es-PE', {...opciones, timeZone: 'UTC'});
    const fechaLarga = formato({weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'});
    let pagina = 1;

    function navegar(fecha, modo = vista) {
        document.getElementById('fechaAgenda').value = fecha;
        document.getElementById('medVista').value = modo;
        formulario.requestSubmit();
    }

    function elemento(tipo, clase, texto) {
        const nodo = document.createElement(tipo);
        nodo.className = clase;
        if (texto !== undefined) nodo.textContent = texto;
        return nodo;
    }

    function crearDia(fecha, citas, fuera) {
        const celda = elemento('section', 'med-calendar-day');
        celda.classList.toggle('is-outside', fuera);
        celda.classList.toggle('is-today', fecha === calendario.dataset.hoy);
        const fechaTexto = fechaLarga.format(fechaUTC(fecha));
        celda.setAttribute('aria-label', fechaTexto);
        const boton = elemento('button', 'med-calendar-date', formato({weekday: 'short', day: 'numeric', month: 'short'}).format(fechaUTC(fecha)));
        boton.type = 'button';
        boton.setAttribute('aria-label', `Ver citas del ${fechaTexto}`);
        if (fecha === calendario.dataset.hoy) boton.setAttribute('aria-current', 'date');
        boton.addEventListener('click', () => navegar(fecha, 'dia'));
        celda.append(boton);
        const limite = vista === 'mes' ? 3 : 4;
        citas.slice(0, limite).forEach(({fila}) => {
            const enlace = elemento('a', 'med-calendar-event');
            enlace.href = fila.querySelector('.med-action-cell a').href;
            enlace.append(elemento('strong', '', fila.dataset.hora), elemento('span', '', fila.dataset.paciente), elemento('small', '', fila.dataset.estado));
            celda.append(enlace);
        });
        if (citas.length > limite) {
            const mas = elemento('button', 'med-calendar-more', `Ver las ${citas.length} citas`);
            mas.type = 'button';
            mas.addEventListener('click', () => navegar(fecha, 'dia'));
            celda.append(mas);
        } else if (!citas.length && !fuera) {
            celda.append(elemento('p', 'med-calendar-empty', 'Sin citas'));
        }
        return celda;
    }

    function mostrarCalendario(resultados) {
        dias.replaceChildren();
        dias.classList.toggle('is-month', vista === 'mes');
        dias.classList.toggle('is-week', vista === 'semana');
        const desde = calendario.dataset.desde;
        const hasta = calendario.dataset.hasta;
        const titulo = vista === 'mes' ? formato({month: 'long', year: 'numeric'}).format(fechaUTC(desde))
            : `${formato({day: 'numeric', month: 'short'}).format(fechaUTC(desde))} – ${formato({day: 'numeric', month: 'short', year: 'numeric'}).format(fechaUTC(hasta))}`;
        document.getElementById('medCalendarioTitulo').textContent = titulo;
        document.getElementById('medCalendarioEstado').textContent = `${resultados.length} ${resultados.length === 1 ? 'cita en el período' : 'citas en el período'}${buscar.value.trim() ? ' que coinciden con la búsqueda' : ''}`;
        let inicio = desde;
        let fin = hasta;
        if (vista === 'mes') {
            ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].forEach(nombre => dias.append(elemento('div', 'med-calendar-weekday', nombre)));
            inicio = sumarDias(desde, -((fechaUTC(desde).getUTCDay() + 6) % 7));
            fin = sumarDias(hasta, 6 - ((fechaUTC(hasta).getUTCDay() + 6) % 7));
        }
        const porFecha = new Map();
        resultados.forEach(registro => {
            const fecha = registro.fila.dataset.fecha;
            if (!porFecha.has(fecha)) porFecha.set(fecha, []);
            porFecha.get(fecha).push(registro);
        });
        for (let fecha = inicio; fecha <= fin; fecha = sumarDias(fecha, 1)) {
            dias.append(crearDia(fecha, porFecha.get(fecha) || [], fecha < desde || fecha > hasta));
        }
    }

    calendario.hidden = !esCalendario;
    document.getElementById('medAgendaTablaRegion').hidden = esCalendario;
    document.getElementById('medAgendaTableHint').hidden = esCalendario;
    anterior.closest('nav').hidden = esCalendario;
    document.getElementById('medPeriodoHoy').addEventListener('click', () => navegar(calendario.dataset.hoy));
    function desplazar(cantidad) {
        let fecha;
        if (vista === 'mes') {
            const mes = fechaUTC(calendario.dataset.desde);
            mes.setUTCMonth(mes.getUTCMonth() + cantidad);
            fecha = iso(mes);
        } else fecha = sumarDias(calendario.dataset.desde, cantidad * 7);
        navegar(fecha);
    }
    document.getElementById('medPeriodoAnterior').addEventListener('click', () => desplazar(-1));
    document.getElementById('medPeriodoSiguiente').addEventListener('click', () => desplazar(1));

    function mostrar() {
        const consulta = normalizar(buscar.value);
        const resultados = registros.filter(registro => registro.texto.includes(consulta));
        if (esCalendario) { mostrarCalendario(resultados); return; }
        const paginas = Math.max(1, Math.ceil(resultados.length / 6));
        pagina = Math.min(pagina, paginas);
        filas.forEach(fila => { fila.hidden = true; });
        resultados.slice((pagina - 1) * 6, pagina * 6).forEach(registro => { registro.fila.hidden = false; });
        anterior.disabled = pagina === 1;
        siguiente.disabled = pagina === paginas;
        estado.textContent = `Página ${pagina} de ${paginas} · ${resultados.length} citas`;
        vacio.hidden = resultados.length > 0 || filas.length === 0;
    }

    buscar.addEventListener('input', () => { pagina = 1; mostrar(); });
    anterior.addEventListener('click', () => { pagina--; mostrar(); });
    siguiente.addEventListener('click', () => { pagina++; mostrar(); });
    document.querySelectorAll('.med-view-switcher button[data-vista]').forEach(boton => {
        boton.addEventListener('click', () => {
            document.getElementById('medVista').value = boton.dataset.vista;
            formulario.requestSubmit();
        });
    });
    globalThis.addEventListener('pageshow', mostrar);
    mostrar();
});
