(() => {
    'use strict';
    const abrir = document.querySelector('[data-abrir-recuperacion]');
    const login = document.querySelector('[data-acceso-login]');
    const panel = document.querySelector('[data-recuperacion-inline]');
    if (!abrir || !login || !panel) return;
    const inicial = new URL(abrir.href, location.href);
    const base = inicial.pathname.slice(0, -'/recuperar'.length);
    const rutas = new Set(['/recuperar', '/verificar-codigo', '/restablecer'].map(ruta => base + ruta));
    const reducirMovimiento = matchMedia('(prefers-reduced-motion: reduce)');
    let ocupado = false;

    function destinoSeguro(href) {
        const url = new URL(href, location.href);
        if (url.origin !== location.origin || !rutas.has(url.pathname)) throw new Error('Ruta de recuperación inválida.');
        return url;
    }

    async function ocultar(elemento) {
        if (reducirMovimiento.matches) return;
        await elemento.animate([{opacity: 1, transform: 'translateY(0)'}, {opacity: 0, transform: 'translateY(-6px)'}], {duration: 160, easing: 'ease-out'}).finished;
    }

    function aparecer(elemento) {
        if (!reducirMovimiento.matches) elemento.animate([{opacity: 0, transform: 'translateY(12px)'}, {opacity: 1, transform: 'translateY(0)'}], {duration: 280, easing: 'cubic-bezier(.16,1,.3,1)'});
    }

    async function volver() {
        if (ocupado) return;
        ocupado = true;
        try {
            await ocultar(panel);
            panel.hidden = true;
            panel.replaceChildren();
            login.hidden = false;
            aparecer(login);
            abrir.focus({preventScroll: true});
        } finally { ocupado = false; }
    }

    function fallo(mensaje) {
        const anterior = panel.querySelector('.recuperacion-fallo');
        anterior?.remove();
        const aviso = document.createElement('div');
        aviso.className = 'recuperacion-fallo';
        aviso.setAttribute('role', 'alert');
        const texto = document.createElement('p');
        texto.textContent = mensaje;
        aviso.append(texto);
        if (panel.hidden) {
            const boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'btn-create-account';
            boton.textContent = 'Volver a iniciar sesión';
            boton.addEventListener('click', () => { void volver(); });
            aviso.append(boton);
            login.hidden = true;
            panel.hidden = false;
        }
        panel.prepend(aviso);
    }

    async function cargar(href, formulario) {
        if (ocupado) return;
        ocupado = true;
        const boton = formulario?.querySelector('button[type="submit"]');
        const texto = boton?.textContent;
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 30000);
        panel.setAttribute('aria-busy', 'true');
        abrir.setAttribute('aria-busy', 'true');
        if (boton) { boton.disabled = true; boton.textContent = 'Procesando…'; }
        try {
            const url = destinoSeguro(href);
            const opciones = {credentials: 'same-origin', cache: 'no-store', signal: controller.signal, headers: {'X-Recuperacion-Inline': '1', Accept: 'text/html'}};
            if (formulario) {
                opciones.method = 'POST';
                opciones.body = new URLSearchParams(new FormData(formulario));
            }
            const respuesta = await fetch(url, opciones);
            destinoSeguro(respuesta.url);
            if (respuesta.headers.get('X-Recuperacion-Vista') !== '1') {
                throw new Error(respuesta.status === 403 ? 'Tu sesión cambió o el formulario venció. Vuelve a abrir la recuperación desde el login.' : 'No se pudo confirmar la operación. Inténtalo más tarde; la solicitud no se repetirá automáticamente.');
            }
            const documento = new DOMParser().parseFromString(await respuesta.text(), 'text/html');
            const contenido = documento.querySelector('.contenido-seguridad');
            if (!contenido || contenido.querySelector('script')) throw new Error('No se pudo mostrar la recuperación. Inténtalo más tarde.');
            await ocultar(panel.hidden ? login : panel);
            panel.replaceChildren(document.importNode(contenido, true));
            login.hidden = true;
            panel.hidden = false;
            window.dispatchEvent(new Event('recuperacion:renderizada'));
            aparecer(panel);
            const encabezado = panel.querySelector('h1');
            if (encabezado) { encabezado.tabIndex = -1; encabezado.focus({preventScroll: true}); }
        } catch (error) {
            let mensaje = error.message;
            if (error.name === 'AbortError') mensaje = 'La operación demoró demasiado. No se repetirá automáticamente. Comprueba tu correo y vuelve a intentarlo si es necesario.';
            if (error.name === 'TypeError') mensaje = 'No se pudo confirmar la operación por un problema de conexión. La solicitud no se repetirá automáticamente.';
            fallo(mensaje);
        } finally {
            clearTimeout(timeout);
            ocupado = false;
            panel.removeAttribute('aria-busy');
            abrir.removeAttribute('aria-busy');
            if (boton?.isConnected) { boton.disabled = false; boton.textContent = texto; }
        }
    }

    abrir.addEventListener('click', event => {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        void cargar(inicial.href);
    });
    panel.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        event.preventDefault();
        const formulario = event.target;
        if (!(formulario instanceof HTMLFormElement) || !formulario.reportValidity()) return;
        void cargar(formulario.action, formulario);
    });
    panel.addEventListener('click', event => {
        const enlace = event.target.closest('a');
        if (!enlace || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const url = new URL(enlace.href, location.href);
        if (url.origin !== location.origin) return;
        if (url.pathname === location.pathname) {
            event.preventDefault();
            void volver();
        } else if (rutas.has(url.pathname)) {
            event.preventDefault();
            void cargar(url.href);
        }
    });
})();
