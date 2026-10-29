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

    document.querySelectorAll('form[data-comprobar-disponibilidad]').forEach((form) => {
        const mensaje = document.createElement('p');
        mensaje.className = 'estado-disponibilidad';
        mensaje.setAttribute('role', 'status');
        mensaje.setAttribute('aria-live', 'polite');
        form.append(mensaje);
        form.addEventListener('submit', async (event) => {
            if (autorizados.has(form)) {
                autorizados.delete(form);
                return;
            }
            event.preventDefault();
            if (enEspera.has(form)) return;
            enEspera.add(form);
            const submitter = event.submitter;
            const botones = [...form.querySelectorAll('[type="submit"]')];
            const deshabilitados = botones.map((button) => button.disabled);
            botones.forEach((button) => { button.disabled = true; });
            form.setAttribute('aria-busy', 'true');
            mensaje.textContent = 'Conectando con el sistema. Esto puede tardar unos segundos.';
            let listo = false;
            try {
                const limite = Date.now() + 30000;
                do {
                    listo = await comprobar();
                    if (listo || Date.now() >= limite) break;
                    await pausar(3000);
                } while (Date.now() < limite);
            } finally {
                botones.forEach((button, index) => { button.disabled = deshabilitados[index]; });
                form.removeAttribute('aria-busy');
                enEspera.delete(form);
            }
            if (listo) {
                mensaje.textContent = '';
                autorizados.add(form);
                if (submitter) form.requestSubmit(submitter);
                else form.requestSubmit();
            } else {
                mensaje.textContent = 'El sistema sigue sin estar disponible. Tus datos no se han enviado. Intenta ingresar de nuevo en unos momentos.';
            }
        });
    });
})();
