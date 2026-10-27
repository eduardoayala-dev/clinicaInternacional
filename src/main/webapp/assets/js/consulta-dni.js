/* Consulta opcional; nunca bloquea el registro ni representa verificación de identidad. */
(() => {
    'use strict';
    const form = document.getElementById('registerForm');
    const input = document.getElementById('regDni');
    const type = document.getElementById('regTipoDoc');
    const button = document.getElementById('btnConsultarDni');
    const status = document.getElementById('dniLookupStatus');
    if (!form || !input || !type || !button || !status) return;
    let active = null;
    let version = 0;
    // Solo se borran valores autocompletados que el paciente aún no haya editado.
    let filled = null;
    function clearAutofill() {
        if (!filled) return;
        for (const name of ['nombres', 'apellidos']) {
            if (form.elements[name].value === filled[name]) form.elements[name].value = '';
        }
        filled = null;
    }
    function documentChanged() {
        version++;
        if (active) active.abort();
        active = null;
        clearAutofill();
        button.disabled = type.value !== 'DNI';
        button.textContent = 'Consultar DNI';
        input.pattern = type.value === 'DNI' ? '[0-9]{8}' : '[A-Za-z0-9]+';
        input.maxLength = type.value === 'DNI' ? 8 : 15;
        input.inputMode = type.value === 'DNI' ? 'numeric' : 'text';
        status.textContent = type.value === 'DNI'
            ? 'Puedes consultar tu DNI o completar tus nombres y apellidos manualmente.'
            : 'Para Carné de Extranjería completa tus datos manualmente.';
    }
    input.addEventListener('input', documentChanged);
    type.addEventListener('change', documentChanged);
    button.addEventListener('click', async () => {
        const numero = input.value;
        if (type.value !== 'DNI' || !/^[0-9]{8}$/.test(numero)) {
            status.textContent = 'Ingresa un DNI de exactamente 8 dígitos para consultar.';
            input.focus();
            return;
        }
        const currentVersion = ++version;
        const before = {nombres: form.elements.nombres.value, apellidos: form.elements.apellidos.value};
        active = new AbortController();
        const controller = active;
        const timeout = setTimeout(() => controller.abort(), 9000);
        button.disabled = true;
        button.textContent = 'Consultando…';
        status.textContent = 'Consultando los datos del DNI…';
        try {
            const response = await fetch(button.dataset.endpoint, {
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {'Content-Type': 'application/json', 'X-CSRF-Token': form.elements.csrfToken.value},
                body: JSON.stringify({numero}), signal: controller.signal
            });
            const result = await response.json();
            if (currentVersion !== version || input.value !== numero || type.value !== 'DNI') return;
            if (!response.ok) {
                status.textContent = result.message || 'No se pudo consultar. Completa tus datos manualmente.';
                return;
            }
            const data = result.data;
            if (!data || data.numero !== numero || typeof data.nombres !== 'string'
                || typeof data.apellidoPaterno !== 'string' || typeof data.apellidoMaterno !== 'string') {
                throw new Error('Respuesta inválida');
            }
            // No pisar cambios escritos mientras la consulta estaba en curso.
            if (form.elements.nombres.value !== before.nombres || form.elements.apellidos.value !== before.apellidos) {
                status.textContent = 'Conservamos los datos que editaste. Revísalos antes de registrarte.';
                return;
            }
            filled = {nombres: data.nombres, apellidos: [data.apellidoPaterno, data.apellidoMaterno].filter(Boolean).join(' ')};
            form.elements.nombres.value = filled.nombres;
            form.elements.apellidos.value = filled.apellidos;
            status.textContent = 'Nombres y apellidos completados. Revisa tus datos antes de registrarte.';
        } catch (error) {
            if (currentVersion === version) status.textContent = 'No se pudo consultar. Puedes completar tus datos manualmente.';
        } finally {
            clearTimeout(timeout);
            if (currentVersion === version) {
                active = null;
                button.disabled = type.value !== 'DNI';
                button.textContent = 'Consultar DNI';
            }
        }
    });
    documentChanged();
})();
