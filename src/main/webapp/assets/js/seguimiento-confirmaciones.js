(() => {
    'use strict';
    function iniciar({document: doc, fetch: consultar, DOMParser: Parser, timers, AbortController: Control, EventSource: FuenteEventos, icons}) {
        const panel = doc.querySelector('[data-seguimiento-panel]');
        if (!panel) return null;
        const boton = doc.getElementById('seguimientoActualizar');
        const estado = doc.getElementById('seguimientoEstado');
        const error = doc.getElementById('seguimientoError');
        const descartar = doc.getElementById('seguimientoDescartar');
        let temporizador;
        let peticion = null;
        let detenido = false;
        let sesionVencida = false;
        let canal = null;
        let conectado = false;
        let cambioPendiente = false;

        function editando() {
            return !!doc.activeElement?.closest?.('.seguimiento-form')
                || [...panel.querySelectorAll('.seguimiento-form select')].some(select => select.value !== '');
        }
        function programar() {
            timers.clearTimeout(temporizador);
            if (!detenido && !doc.hidden && !sesionVencida) {
                temporizador = timers.setTimeout(actualizar, conectado ? 60000 : 15000);
            }
        }
        function pausar() {
            estado.textContent = 'Actualización pausada mientras preparas una gestión.';
            descartar.hidden = false;
        }
        async function obtenerDatos(controller) {
            const respuesta = await consultar(boton.href, {
                credentials: 'same-origin', cache: 'no-store',
                headers: {Accept: 'text/html', 'X-Requested-With': 'XMLHttpRequest'}, signal: controller.signal
            });
            if (respuesta.redirected || respuesta.status === 401 || respuesta.status === 403) {
                sesionVencida = true;
                cerrarCanal();
                throw new Error('La sesión cambió o venció. Vuelve a iniciar sesión para actualizar.');
            }
            if (!respuesta.ok) throw new Error('No se pudo actualizar. Se conservan los datos anteriores.');
            const html = await respuesta.text();
            const pagina = new Parser().parseFromString(html, 'text/html');
            const datos = pagina.getElementById('seguimientoDatos');
            if (!datos) throw new Error('La respuesta no es válida. Se conservan los datos anteriores.');
            return datos;
        }
        function aplicarDatos(nuevos, controller) {
            if (detenido || doc.hidden || controller.signal.aborted) return;
            if (editando()) { pausar(); return; }
            const actuales = doc.getElementById('seguimientoDatos');
            actuales.replaceChildren(...[...nuevos.childNodes].map(node => doc.importNode(node, true)));
            icons?.();
            error.hidden = true;
            descartar.hidden = true;
            estado.textContent = 'Actualizado a las ' + new Date().toLocaleTimeString('es-PE', {hour: '2-digit', minute: '2-digit', second: '2-digit'}) + (conectado ? ' · En vivo' : ' · Respaldo cada 15 s');
        }
        function mostrarFallo(fallo) {
            if (detenido || doc.hidden) return;
            estado.textContent = 'Actualización pendiente';
            error.textContent = fallo instanceof Error && fallo.name !== 'AbortError'
                ? fallo.message : 'La actualización tardó demasiado. Se reintentará automáticamente.';
            error.hidden = false;
        }
        function puedeConsultar() { return !detenido && !doc.hidden && !sesionVencida && !peticion; }
        async function actualizar() {
            if (!puedeConsultar()) return;
            timers.clearTimeout(temporizador);
            if (editando()) { pausar(); programar(); return; }
            const controller = new Control();
            peticion = controller;
            cambioPendiente = false;
            const limite = timers.setTimeout(() => controller.abort(), 10000);
            boton.setAttribute('aria-disabled', 'true');
            panel.setAttribute('aria-busy', 'true');
            estado.textContent = 'Actualizando…';
            try { aplicarDatos(await obtenerDatos(controller), controller); }
            catch (error_) { mostrarFallo(error_); }
            finally {
                timers.clearTimeout(limite);
                peticion = null;
                boton.removeAttribute('aria-disabled');
                panel.removeAttribute('aria-busy');
                if (cambioPendiente && !editando() && puedeConsultar()) temporizador = timers.setTimeout(actualizar, 100);
                else programar();
            }
        }
        function cerrarCanal() {
            canal?.close();
            canal = null;
            conectado = false;
        }
        function recibirCambio() {
            cambioPendiente = true;
            void actualizar();
        }
        function abrirCanal() {
            if (!FuenteEventos || canal || doc.hidden || detenido || sesionVencida) return;
            try { canal = new FuenteEventos(boton.dataset.eventosUrl); }
            catch (error_) { mostrarFallo(error_); programar(); return; }
            const fuenteActual = canal;
            const vigente = () => canal === fuenteActual && !detenido && !doc.hidden;
            canal.addEventListener('open', () => {
                if (!vigente()) return;
                conectado = true;
                programar();
                if (!peticion && !editando()) estado.textContent = 'Actualización en vivo conectada';
            });
            const cambio = () => { if (vigente()) recibirCambio(); };
            canal.addEventListener('sincronizar', cambio);
            canal.addEventListener('cambio', cambio);
            canal.addEventListener('error', () => {
                if (!vigente()) return;
                conectado = false;
                if (!peticion && !editando()) estado.textContent = 'Reconectando actualización en vivo · Respaldo cada 15 s';
                programar();
            });
        }
        function visibilidad() {
            timers.clearTimeout(temporizador);
            if (doc.hidden) { cerrarCanal(); peticion?.abort(); }
            else { abrirCanal(); recibirCambio(); }
        }
        function cambios() {
            if (editando()) pausar();
            else if (!doc.hidden) void actualizar();
        }
        function detener() {
            detenido = true;
            timers.clearTimeout(temporizador);
            peticion?.abort();
            cerrarCanal();
        }
        boton.addEventListener('click', event => { event.preventDefault(); void actualizar(); });
        panel.addEventListener('change', cambios);
        descartar.addEventListener('click', () => {
            panel.querySelectorAll('.seguimiento-form').forEach(form => form.reset());
            descartar.hidden = true;
            void actualizar();
        });
        panel.addEventListener('focusout', () => {
            timers.setTimeout(() => { if (!detenido) cambios(); }, 0);
        });
        panel.addEventListener('submit', detener);
        doc.addEventListener('visibilitychange', visibilidad);
        programar();
        abrirCanal();
        return {actualizar, detener, reanudar() { detenido = false; abrirCanal(); recibirCambio(); }};
    }
    globalThis.ClinicaSeguimiento = Object.freeze({iniciar});
    if (typeof document !== 'undefined') {
        const instancia = iniciar({document, fetch: globalThis.fetch.bind(globalThis), DOMParser,
            timers: globalThis, AbortController, EventSource: globalThis.EventSource, icons: () => globalThis.lucide?.createIcons()});
        globalThis.addEventListener('pagehide', () => instancia?.detener());
        globalThis.addEventListener('pageshow', event => { if (event.persisted) instancia?.reanudar(); });
    }
})();
