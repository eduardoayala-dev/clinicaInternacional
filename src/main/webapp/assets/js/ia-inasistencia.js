document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formIa');
    if (!form) return;
    const boton = document.getElementById('iaCalcular');
    const estado = document.getElementById('iaEstado');
    const error = document.getElementById('iaError');
    const resultado = document.getElementById('iaPrediccion');
    const influencias = {AUMENTA: 'aumenta', REDUCE: 'reduce', NEUTRA: 'influencia neutra'};
    let version = 0;
    let activa = null;
    const ocupado = valor => {
        boton.disabled = valor; boton.textContent = valor ? 'Calculando…' : 'Simular predicción';
        if (valor) form.setAttribute('aria-busy', 'true'); else form.removeAttribute('aria-busy');
    };
    const invalidar = () => {
        version++;
        activa?.abort(); activa = null;
        resultado.hidden = true; error.hidden = true;
        estado.textContent = 'Datos modificados. Vuelve a simular para obtener un resultado actualizado.';
        ocupado(false);
    };
    form.addEventListener('input', invalidar);
    form.addEventListener('change', invalidar);
    const listar = (id, textos) => {
        const elementos = textos.map(texto => { const item = document.createElement('li'); item.textContent = texto; return item; });
        document.getElementById(id).replaceChildren(...elementos);
    };
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (boton.disabled || !form.reportValidity()) return;
        const actual = ++version;
        const solicitud = new AbortController(); activa = solicitud;
        const limite = globalThis.setTimeout(() => solicitud.abort(), 15000);
        ocupado(true);
        error.hidden = true; resultado.hidden = true; estado.textContent = 'Evaluando el modelo experimental…';
        try {
            const p = await ClinicaInasistencia.simular(form.action, new URLSearchParams(new FormData(form)), solicitud.signal);
            if (actual !== version) return;
            document.getElementById('iaProbabilidad').textContent = (p.probabilidad * 100).toFixed(1) + '% de inasistencia estimada';
            document.getElementById('iaNivel').textContent = 'Nivel del escenario sintético: ' + p.nivel;
            document.getElementById('iaAccion').textContent = p.accionSugerida;
            document.getElementById('iaVersion').textContent = 'Modelo: ' + p.version + ' · Origen: ' + p.origen;
            listar('iaFactores', p.factores.map(f => f.descripcion + ' (valor ' + f.valor + '; promedio sintético ' + f.referencia.toFixed(2) + '): ' + influencias[f.influencia] + ' · contribución ' + f.contribucionLogit.toFixed(3)));
            listar('iaAdvertencias', p.advertencias);
            resultado.hidden = false; estado.textContent = 'Simulación completada. No se modificó ninguna cita.';
        } catch (fallo) {
            if (actual !== version) return;
            if (solicitud.signal.aborted) error.textContent = 'La simulación tardó demasiado. Inténtalo nuevamente.';
            else error.textContent = fallo instanceof Error ? fallo.message : 'No se pudo completar la simulación. Inténtalo nuevamente.';
            error.hidden = false; estado.textContent = '';
        } finally {
            globalThis.clearTimeout(limite);
            if (actual === version) { activa = null; ocupado(false); }
        }
    });
    globalThis.addEventListener('pagehide', invalidar);
});
