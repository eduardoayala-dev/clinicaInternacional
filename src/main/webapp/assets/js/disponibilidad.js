(() => {
    'use strict';
    const endpoint = document.currentScript?.dataset.readyUrl;
    if (!endpoint) return;
    const enEspera = new WeakSet();
    const autorizados = new WeakSet();
    const pausar = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

    async function comprobar() {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 2500);
        try {
            const response = await fetch(endpoint, {
                method: 'GET', cache: 'no-store', credentials: 'same-origin',
                headers: { Accept: 'application/json' }, signal: controller.signal
            });
            if (!response.ok) return false;
            const result = await response.json();
            return result.code === 'SISTEMA_DISPONIBLE';
        } catch {
            return false;
        } finally {
            clearTimeout(timeout);
        }
    }

    async function esperarDisponibilidad(limite) {
        if (await comprobar()) return true;
        if (Date.now() >= limite) return false;
        await pausar(3000);
        return Date.now() < limite ? esperarDisponibilidad(limite) : false;
    }

    document.querySelectorAll('form[data-comprobar-disponibilidad]').forEach((form) => {
        const botones = [...form.querySelectorAll('[type="submit"]')];
        let originales;
        let etiquetas = [];
        function bloquear(texto) {
            if (!originales) {
                originales = botones.map((button) => ({disabled: button.disabled, nodos: [...button.childNodes]}));
                etiquetas = botones.map((button) => {
                    const spinner = document.createElement('span');
                    spinner.className = 'indicador-carga-login';
                    spinner.setAttribute('aria-hidden', 'true');
                    const etiqueta = document.createElement('span');
                    button.replaceChildren(spinner, etiqueta);
                    return etiqueta;
                });
            }
            etiquetas.forEach((etiqueta) => { etiqueta.textContent = texto; });
            botones.forEach((button) => {
                button.disabled = true;
                button.setAttribute('aria-busy', 'true');
            });
            form.setAttribute('aria-busy', 'true');
        }
        function restaurar() {
            if (originales) {
                botones.forEach((button, index) => {
                    button.disabled = originales[index].disabled;
                    button.replaceChildren(...originales[index].nodos);
                    button.removeAttribute('aria-busy');
                });
            }
            originales = undefined;
            form.removeAttribute('aria-busy');
            enEspera.delete(form);
            autorizados.delete(form);
        }
        const mensaje = document.createElement('p');
        mensaje.className = 'estado-disponibilidad';
        mensaje.setAttribute('role', 'status');
        mensaje.setAttribute('aria-live', 'polite');
        form.append(mensaje);
        window.addEventListener('pageshow', (event) => {
            if (!event.persisted) return;
            restaurar();
            mensaje.textContent = '';
        });
        form.addEventListener('submit', async (event) => {
            if (autorizados.has(form)) {
                autorizados.delete(form);
                bloquear('Iniciando sesión…');
                mensaje.textContent = 'Validando tus credenciales. Espera un momento.';
                return;
            }
            event.preventDefault();
            if (enEspera.has(form)) return;
            enEspera.add(form);
            const submitter = event.submitter;
            bloquear('Conectando…');
            mensaje.textContent = 'Conectando con el sistema. Esto puede tardar unos segundos.';
            let listo = false;
            try {
                listo = await esperarDisponibilidad(Date.now() + 30000);
            } finally {
                if (!listo) restaurar();
            }
            if (listo) {
                if (!form.checkValidity()) {
                    restaurar();
                    mensaje.textContent = '';
                    form.reportValidity();
                    return;
                }
                autorizados.add(form);
                if (submitter) submitter.disabled = false;
                if (submitter) form.requestSubmit(submitter);
                else form.requestSubmit();
            } else {
                mensaje.textContent = 'El sistema sigue sin estar disponible. Tus datos no se han enviado. Intenta ingresar de nuevo en unos momentos.';
            }
        });
    });
})();
