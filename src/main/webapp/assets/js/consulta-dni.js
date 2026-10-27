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
    function hasValidDni() { return type.value === 'DNI' && /^\d{8}$/.test(input.value); }
    function isCurrent(currentVersion, numero) {
        return currentVersion === version && input.value === numero && type.value === 'DNI';
    }
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
        input.pattern = type.value === 'DNI' ? '\\d{8}' : '[A-Za-z0-9]+';
        input.maxLength = type.value === 'DNI' ? 8 : 15;
        input.inputMode = type.value === 'DNI' ? 'numeric' : 'text';
        status.textContent = '';
        status.removeAttribute('aria-busy');
    }
    async function lookup() {
        documentChanged();
        const numero = input.value;
        if (!hasValidDni() || shouldSkip(numero)) return;
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
            if (!isCurrent(currentVersion, numero)) return;
            applyResponse(response, result, numero);
        } catch (error) {
            if (isCurrent(currentVersion, numero)) {
                lock(false);
                status.textContent = error.name === 'AbortError'
                    ? 'La consulta demoró demasiado. Completa tus nombres y apellidos manualmente.'
                    : 'No se pudo consultar. Completa tus nombres y apellidos manualmente.';
            }
        } finally {
            clearTimeout(timeout);
            if (currentVersion === version) {
                active = null;
                status.removeAttribute('aria-busy');
            }
        }
    }
    function shouldSkip(numero) {
        if (lastAttempt !== numero) return false;
        if (retryAt === null) return true;
        const remaining = retryAt - Date.now();
        if (remaining <= 0) return false;
        showRetry(Math.ceil(remaining / 1000));
        return true;
    }
    function showFailure(response, result) {
        lock(false);
        const wait = Number(response.headers?.get('Retry-After'));
        if (response.status === 429 && Number.isInteger(wait) && wait > 0 && wait <= 86400) {
            retryAt = Date.now() + wait * 1000;
            showRetry(wait);
        } else status.textContent = result.message || 'Completa tus nombres y apellidos manualmente.';
    }
    function isValidData(data, numero) {
        return data && data.numero === numero && typeof data.nombres === 'string'
            && typeof data.apellidoPaterno === 'string' && typeof data.apellidoMaterno === 'string';
    }
    function applyResponse(response, result, numero) {
        if (!response.ok) {
            showFailure(response, result);
            return;
        }
        if (!isValidData(result.data, numero)) throw new Error('Respuesta inválida');
        names[0].value = result.data.nombres;
        names[1].value = [result.data.apellidoPaterno, result.data.apellidoMaterno].filter(Boolean).join(' ');
        filled = true;
        lock(true);
        status.textContent = 'Datos del DNI completados.';
    }
    function showRetry(seconds) {
        let amount = seconds;
        let unit = 'segundo';
        if (seconds >= 3600) {
            amount = Math.ceil(seconds / 3600);
            unit = 'hora';
        } else if (seconds >= 60) {
            amount = Math.ceil(seconds / 60);
            unit = 'minuto';
        }
        if (amount !== 1) unit += 's';
        status.textContent = 'Se alcanzó el límite de consultas. Podrás reintentar en aproximadamente '
            + amount + ' ' + unit + '. Puedes completar tus datos manualmente.';
    }
    input.addEventListener('input', documentChanged);
    type.addEventListener('change', documentChanged);
    input.addEventListener('blur', lookup);
    form.addEventListener('submit', event => {
        if (active || (hasValidDni() && lastAttempt !== input.value)) {
            event.preventDefault();
            if (!active) void lookup();
        }
    });
    documentChanged();
})();
