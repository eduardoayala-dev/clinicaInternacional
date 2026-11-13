document.addEventListener('DOMContentLoaded', () => {
    const dias = [...document.querySelectorAll('input[name="dias"]')];
    const validarDias = () => {
        if (dias.length) dias[0].setCustomValidity(dias.some(dia => dia.checked) ? '' : 'Selecciona al menos un día de atención.');
    };
    dias.forEach(dia => dia.addEventListener('change', validarDias));
    validarDias();
    const desde = document.getElementById('desde');
    const hasta = document.getElementById('hasta');
    const acotarVigencia = () => {
        if (!desde?.valueAsDate || !hasta) return;
        hasta.min = desde.value;
        const limite = new Date(desde.valueAsDate);
        limite.setUTCDate(limite.getUTCDate() + 89);
        hasta.max = limite.toISOString().slice(0, 10);
    };
    desde?.addEventListener('change', acotarVigencia);
    acotarVigencia();

    document.querySelectorAll('.hor-revision').forEach(form => {
        const motivo = form.querySelector('textarea');
        const validarMotivo = accion => motivo.setCustomValidity(accion === 'rechazar' && motivo.value.trim().length < 5 ? 'Explica el motivo del rechazo (mínimo 5 caracteres).' : '');
        form.querySelectorAll('button').forEach(boton => boton.addEventListener('click', () => validarMotivo(boton.value)));
        form.addEventListener('submit', event => {
            const boton = event.submitter;
            if (!boton || form.dataset.enviando === 'true') { event.preventDefault(); return; }
            validarMotivo(boton.value);
            if (!form.reportValidity()) { event.preventDefault(); return; }
            if (!globalThis.confirm(boton.value === 'aprobar' ? '¿Aprobar y publicar todos los turnos futuros de esta propuesta?' : '¿Rechazar esta propuesta y comunicar el motivo al médico?')) {
                event.preventDefault(); return;
            }
            const accion = document.createElement('input');
            accion.type = 'hidden'; accion.name = 'accion'; accion.value = boton.value; accion.dataset.accionEnviada = 'true';
            form.append(accion);
            form.dataset.enviando = 'true'; form.setAttribute('aria-busy', 'true');
            form.querySelectorAll('button').forEach(b => { b.disabled = true; });
        });
        form.querySelector('textarea').addEventListener('input', event => event.target.setCustomValidity(''));
    });
    globalThis.addEventListener('pageshow', () => {
        document.querySelectorAll('.hor-revision').forEach(form => {
            delete form.dataset.enviando; form.removeAttribute('aria-busy');
            form.querySelectorAll('[data-accion-enviada]').forEach(input => input.remove());
            form.querySelectorAll('button').forEach(boton => { boton.disabled = false; });
        });
    });
});
