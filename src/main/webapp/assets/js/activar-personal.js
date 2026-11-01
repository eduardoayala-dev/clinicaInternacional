(() => {
    'use strict';
    const mensaje = document.getElementById('activacionMensaje');
    const csrf = document.querySelector('meta[name="csrf-token"]')?.content;
    const reintentar = document.getElementById('reintentarInvitacion');
    let ocupada = false;
    async function enviar(params) {
        if (ocupada) return;
        ocupada = true;
        if (reintentar) reintentar.hidden = true;
        const boton = document.querySelector('#formActivacion button');
        if (boton) { boton.disabled = true; boton.textContent = 'Procesando…'; }
        const controller = new AbortController();
        const limite = setTimeout(() => controller.abort(), 15000);
        try {
            const respuesta = await fetch(location.pathname, {method: 'POST', credentials: 'same-origin',
                headers: {Accept: 'application/json', 'X-CSRF-Token': csrf, 'Content-Type': 'application/x-www-form-urlencoded'},
                body: params, signal: controller.signal});
            const datos = await respuesta.json();
            if (!datos || typeof datos.message !== 'string') throw new Error('Respuesta inválida');
            mensaje.textContent = datos.message;
            if (respuesta.ok && datos.code === 'INVITACION_PREPARADA') location.reload();
            else if (respuesta.ok && datos.code === 'PERSONAL_ACTIVADO') document.getElementById('formActivacion')?.remove();
            else if (params.get('action') === 'preparar' && respuesta.status >= 500 && reintentar) reintentar.hidden = false;
        } catch {
            mensaje.textContent = 'No se pudo comprobar el resultado. Inténtalo nuevamente; si ya activaste la cuenta, inicia sesión.';
            if (params.get('action') === 'preparar' && reintentar) reintentar.hidden = false;
        }
        finally {
            clearTimeout(limite); ocupada = false;
            if (boton) { boton.disabled = false; boton.textContent = 'Activar mi cuenta'; }
        }
    }
    const token = location.hash.slice(1);
    if (token) {
        history.replaceState(null, '', location.pathname);
        document.getElementById('formActivacion')?.remove();
        mensaje.textContent = 'Comprobando tu invitación…';
        void enviar(new URLSearchParams({action: 'preparar', token, csrfToken: csrf}));
    }
    reintentar?.addEventListener('click', () => {
        if (token) void enviar(new URLSearchParams({action: 'preparar', token, csrfToken: csrf}));
    });
    document.getElementById('formActivacion')?.addEventListener('submit', event => {
        event.preventDefault();
        void enviar(new URLSearchParams(new FormData(event.currentTarget)));
    });
})();
