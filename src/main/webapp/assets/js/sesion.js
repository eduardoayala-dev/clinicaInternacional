(() => {
    'use strict';
    const endpoint = document.currentScript?.dataset.stateUrl;
    const context = document.querySelector('meta[name="auth-context"]')?.content;
    if (!endpoint || !context) return;
    const passing = new WeakSet();
    const waiting = new WeakSet();
    let pending;
    let blocked = false;
    let dialog;

    function notice(message, portal) {
        if (!dialog) {
            dialog = document.createElement('dialog');
            const text = document.createElement('p');
            const action = document.createElement('button');
            dialog.append(text, action);
            document.body.append(dialog);
            dialog.addEventListener('cancel', (event) => { if (blocked) event.preventDefault(); });
        }
        dialog.querySelector('p').textContent = message;
        const button = dialog.querySelector('button');
        button.textContent = portal ? 'Volver a mi portal' : 'Continuar';
        button.onclick = () => { if (portal) window.location.assign(portal); else dialog.close(); };
        if (!dialog.open) dialog.showModal();
    }
    async function state() {
        if (pending) return pending;
        pending = (async () => {
            const controller = new AbortController();
            const timer = setTimeout(() => controller.abort(), 6000);
            try {
                const response = await fetch(endpoint, {cache: 'no-store', credentials: 'same-origin',
                    headers: {Accept: 'application/json', 'X-Session-Context': context}, signal: controller.signal});
                if (!response.ok) throw new Error('unavailable');
                const result = await response.json();
                if (!['SESION_ACTIVA', 'SESION_ANONIMA'].includes(result.code)) throw new Error('invalid');
                const data = result.data;
                if (!data || typeof data.sid !== 'string' || typeof data.portal !== 'string') throw new Error('invalid');
                const portal = new URL(data.portal, window.location.origin);
                if (portal.origin !== window.location.origin) throw new Error('invalid');
                if (data.sid !== context) {
                    blocked = true;
                    if (result.code === 'SESION_ANONIMA' || (context === 'anonymous' && result.code === 'SESION_ACTIVA')) {
                        document.querySelectorAll('input[type="password"]').forEach((field) => { field.value = ''; });
                        window.location.assign(result.code === 'SESION_ANONIMA' ? portal.href + '?msg=sesion_cerrada' : portal.href);
                    } else {
                        document.querySelectorAll('body > :not(dialog)').forEach((element) => { element.hidden = true; });
                        notice('Tu sesión cambió o terminó en otra pestaña. No se envió este formulario.', portal.href);
                    }
                    return false;
                }
                if (!/^[A-Za-z0-9_-]{43}$/.test(data.csrf)) throw new Error('invalid');
                document.querySelectorAll('input[name="csrfToken"]').forEach((field) => { field.value = data.csrf; });
                const meta = document.querySelector('meta[name="csrf-token"]');
                if (meta) meta.content = data.csrf;
                return true;
            } finally { clearTimeout(timer); }
        })();
        try { return await pending; } finally { pending = undefined; }
    }
    document.addEventListener('submit', async (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() === 'get') return;
        if (new URL(form.action, window.location.href).origin !== window.location.origin) return;
        if (passing.has(form)) { passing.delete(form); return; }
        event.preventDefault();
        event.stopImmediatePropagation();
        if (blocked || waiting.has(form)) return;
        waiting.add(form);
        const button = event.submitter;
        const disabled = button?.disabled;
        if (button) button.disabled = true;
        form.setAttribute('aria-busy', 'true');
        let ready = false;
        try { ready = await state(); }
        catch { notice('No pudimos comprobar tu sesión. Tus datos no se enviaron. Inténtalo de nuevo.'); }
        finally {
            waiting.delete(form);
            form.removeAttribute('aria-busy');
            if (button) button.disabled = disabled;
        }
        if (ready && !blocked) {
            passing.add(form);
            if (button) form.requestSubmit(button); else form.requestSubmit();
        }
    }, true);
    const check = () => { if (!blocked) state().catch(() => {}); };
    window.addEventListener('focus', check);
    window.addEventListener('pageshow', check);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) check(); });
    window.addEventListener('storage', (event) => { if (event.key === 'clinica-auth-event') check(); });
    try { localStorage.setItem('clinica-auth-event', String(Date.now())); } catch {}
})();
