(() => {
    'use strict';

    async function obtener(url, signal) {
        let response;
        try {
            response = await fetch(url, {
                credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'}, signal
            });
        } catch (cause) {
            if (cause.name === 'AbortError') throw cause;
            throw new Error('No se pudo conectar con el servidor.', {cause});
        }
        let result;
        try {
            result = await response.json();
        } catch (cause) {
            throw new Error('No se pudo interpretar la respuesta del servidor.', {cause});
        }
        if (!response.ok) {
            const error = new Error(result?.message || 'No se pudo consultar el catálogo.');
            error.status = response.status;
            error.code = result?.code;
            throw error;
        }
        if (!result || typeof result.code !== 'string' || typeof result.message !== 'string'
            || !Array.isArray(result.data)) throw new Error('El catálogo tiene un formato de respuesta inválido.');
        return result.data;
    }

    async function medicos(base, especialidad, sede, signal) {
        const query = new URLSearchParams({idEspecialidad: String(especialidad)});
        if (sede && String(sede) !== '0') query.set('idSede', String(sede));
        const data = await obtener(base + '/api/medicos?' + query, signal);
        if (!data.every(medico => medico && Number.isInteger(medico.idMedico) && medico.idMedico > 0
            && typeof medico.nombreCompleto === 'string' && typeof medico.cmp === 'string')) {
            throw new Error('La lista de médicos tiene un formato inválido.');
        }
        return data;
    }

    async function horarios(base, medico, fecha, signal) {
        const query = new URLSearchParams({idMedico: String(medico)});
        if (fecha) query.set('fecha', fecha);
        const data = await obtener(base + '/api/horarios?' + query, signal);
        const pattern = fecha ? /^([01]\d|2[0-3]):[0-5]\d$/ : /^\d{4}-\d{2}-\d{2}$/;
        if (!data.every(value => typeof value === 'string' && pattern.test(value))) {
            throw new Error('La disponibilidad tiene un formato inválido.');
        }
        return data;
    }

    globalThis.ClinicaCatalogos = Object.freeze({obtener, medicos, horarios});
})();
