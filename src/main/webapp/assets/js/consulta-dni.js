(() => {
    'use strict';
    const form = document.getElementById('registerForm');
    const input = document.getElementById('regDni');
    const type = document.getElementById('regTipoDoc');
    const status = document.getElementById('dniLookupStatus');
    if (!form || !input || !type || !status) return;
    let active = null, version = 0, filled = false, lastAttempt = null, identity = null;
    let retryAt = null;
    const names = ['nombres', 'apellidos'].map(name => form.elements[name]);
    function lock(value) { names.forEach(field => { field.readOnly = value; }); }
    function hasValidDni() { return type.value === 'DNI' && /^[0-9]{8}$/.test(input.value); }
    function documentChanged() {
        const next = type.value + ':' + input.value;
        if (identity === next) return;
        identity = next;
        version++;
        if (active) active.abort();
        active = null;
        lastAttempt = null;
        retryAt = null;
        if (filled) names.forEach(field => { field.value = ''; });
        filled = false;
        lock(type.value === 'DNI');
        input.pattern = type.value === 'DNI' ? '[0-9]{8}' : '[A-Za-z0-9]+';
        input.maxLength = type.value === 'DNI' ? 8 : 15;
        input.inputMode = type.value === 'DNI' ? 'numeric' : 'text';
        status.textContent = '';
        status.removeAttribute('aria-busy');
    }
    async function lookup() {
        documentChanged();
        const numero = input.value;
        if (!hasValidDni()) return;
        if (lastAttempt === numero) {
            if (retryAt === null) return;
            if (Date.now() < retryAt) {
                showRetry(Math.ceil((retryAt - Date.now()) / 1000));
                return;
            }
        }
        lastAttempt = numero;
        retryAt = null;
        const currentVersion = ++version;
        active = new AbortController();
        const controller = active;
        const timeout = setTimeout(() => controller.abort(), 9000);
        lock(true);
        status.setAttribute('aria-busy', 'true');
        status.textContent = 'Buscando datos del DNI…';
        try {
            const response = await fetch(input.dataset.endpoint, {
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {'Content-Type': 'application/json', 'X-CSRF-Token': form.elements.csrfToken.value},
                body: JSON.stringify({numero}), signal: controller.signal
            });
            const result = await response.json();
            if (currentVersion !== version || input.value !== numero || type.value !== 'DNI') return;
            if (!response.ok) {
                lock(false);
                const wait = Number(response.headers?.get('Retry-After'));
                if (response.status === 429 && Number.isInteger(wait) && wait > 0 && wait <= 86400) {
                    retryAt = Date.now() + wait * 1000;
                    showRetry(wait);
                } else status.textContent = result.message || 'Completa tus nombres y apellidos manualmente.';
                return;
            }
            const data = result.data;
            if (!data || data.numero !== numero || typeof data.nombres !== 'string'
                || typeof data.apellidoPaterno !== 'string' || typeof data.apellidoMaterno !== 'string') {
                throw new Error('Respuesta inválida');
            }
            names[0].value = data.nombres;
            names[1].value = [data.apellidoPaterno, data.apellidoMaterno].filter(Boolean).join(' ');
            filled = true;
            lock(true);
            status.textContent = 'Datos del DNI completados.';
        } catch (error) {
            if (currentVersion === version) {
                lock(false);
                status.textContent = 'No se pudo consultar. Completa tus nombres y apellidos manualmente.';
            }
        } finally {
            clearTimeout(timeout);
            if (currentVersion === version) {
                active = null;
                status.removeAttribute('aria-busy');
            }
        }
    }
    function showRetry(seconds) {
        const amount = seconds < 60 ? seconds : seconds < 3600 ? Math.ceil(seconds / 60) : Math.ceil(seconds / 3600);
        const unit = seconds < 60 ? (amount === 1 ? 'segundo' : 'segundos')
            : seconds < 3600 ? (amount === 1 ? 'minuto' : 'minutos') : (amount === 1 ? 'hora' : 'horas');
        status.textContent = 'Se alcanzó el límite de consultas. Podrás reintentar en aproximadamente '
            + amount + ' ' + unit + '. Puedes completar tus datos manualmente.';
    }
    input.addEventListener('input', documentChanged);
    type.addEventListener('change', documentChanged);
    input.addEventListener('blur', lookup);
    form.addEventListener('submit', event => {
        if (active || (hasValidDni() && lastAttempt !== input.value)) {
            event.preventDefault();
            if (!active) lookup();
        }
    });
    documentChanged();
})();
