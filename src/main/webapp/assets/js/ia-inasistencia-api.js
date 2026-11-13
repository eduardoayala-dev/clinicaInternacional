(() => {
    'use strict';
    const variables = new Set(['inasistencias_previas', 'atenciones_previas', 'anticipacion_dias', 'confirmada_por_paciente']);
    const texto = valor => typeof valor === 'string' && valor.length > 0 && valor.length <= 1200;
    function validar(p) {
        if (!p || p.experimental !== true || p.origen !== 'SINTETICO'
            || typeof p.version !== 'string' || !/^[a-z0-9-]{1,80}$/.test(p.version)
            || !Number.isFinite(p.probabilidad) || p.probabilidad < 0 || p.probabilidad > 1
            || !['BAJO', 'MEDIO', 'ALTO'].includes(p.nivel) || !texto(p.accionSugerida)
            || !Array.isArray(p.advertencias) || p.advertencias.length === 0 || !p.advertencias.every(texto)
            || !Array.isArray(p.factores) || p.factores.length !== variables.size) return false;
        const vistos = new Set();
        for (const f of p.factores) {
            if (!f || !variables.has(f.variable) || vistos.has(f.variable) || !texto(f.descripcion)
                || !Number.isFinite(f.valor) || !Number.isFinite(f.referencia) || !Number.isFinite(f.contribucionLogit)) return false;
            const influencia = f.contribucionLogit > 0 ? 'AUMENTA' : f.contribucionLogit < 0 ? 'REDUCE' : 'NEUTRA';
            if (f.influencia !== influencia) return false;
            vistos.add(f.variable);
        }
        return true;
    }
    async function simular(url, parametros, signal) {
        let respuesta;
        let contenido;
        try {
            respuesta = await fetch(url, {method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: {Accept: 'application/json'}, body: parametros, signal});
            contenido = await respuesta.json();
        } catch (cause) {
            if (signal?.aborted) throw cause;
            throw new Error('No se pudo completar la simulación. Inténtalo nuevamente.', {cause});
        }
        if (!respuesta.ok) throw new Error(texto(contenido?.message) ? contenido.message : 'No se pudo completar la simulación.');
        if (respuesta.status !== 200 || contenido?.code !== 'PREDICCION_EXPERIMENTAL_OBTENIDA' || !validar(contenido.data)) {
            throw new Error('No se pudo verificar el resultado del modelo. No se mostrará una predicción.');
        }
        return contenido.data;
    }
    globalThis.ClinicaInasistencia = Object.freeze({simular});
})();
