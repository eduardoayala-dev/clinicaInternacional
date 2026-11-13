(() => {
    'use strict';
    function preparar({medico, motivo, fecha, hora, retencion}) {
        if (!/^[1-9]\d*$/.test(String(medico ?? ''))) throw new Error('Vuelve a seleccionar el médico y el horario.');
        if (!/^[1-9]\d*$/.test(String(motivo ?? ''))) throw new Error('Selecciona un motivo de consulta del catálogo.');
        if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha ?? '') || !/^([01]\d|2[0-3]):[0-5]\d$/.test(hora ?? ''))
            throw new Error('Vuelve a seleccionar la fecha y el horario.');
        if (!/^[a-f0-9-]{36}$/.test(retencion ?? '')) throw new Error('Selecciona el horario nuevamente para retener el cupo.');
        return new URLSearchParams({idMedico:String(medico), idMotivo:String(motivo), fecha, hora, retencion});
    }
    async function reservar(url, params, csrf) {
        let response;
        let result;
        try {
            response = await fetch(url, {
                method: 'POST', credentials: 'same-origin', body: params,
                headers: {'X-CSRF-Token': csrf, Accept: 'application/json'}
            });
            result = await response.json();
        } catch (cause) {
            throw new Error('No se pudo confirmar el resultado. Revisa Mis citas antes de volver a intentar.', {cause});
        }
        if (!response.ok) throw new Error(result?.message || 'No se pudo completar la reserva.');
        if (response.status !== 201 || result?.code !== 'CITA_RESERVADA'
            || !Number.isInteger(result.data?.idCita) || result.data.idCita <= 0
            || typeof result.data.codigoCita !== 'string' || !/^CI-\d{4}-\d+$/.test(result.data.codigoCita)) {
            throw new Error('No se pudo verificar el comprobante. Revisa Mis citas antes de volver a intentar.');
        }
        return result.data;
    }
    async function retencion(url, params, csrf) {
        const response = await fetch(url, {method:'POST',credentials:'same-origin',body:params,
            headers:{'X-CSRF-Token':csrf,Accept:'application/json'}});
        const result=await response.json();
        if(!response.ok)throw new Error(result?.message || 'No se pudo retener el cupo.');
        if(params.get('accion')==='liberar')return null;
        if(result?.code!=='CUPO_RETENIDO' || typeof result.data?.identificador!=='string'
            || !/^[a-f0-9-]{36}$/.test(result.data.identificador)
            || !Number.isInteger(result.data.segundosRestantes) || result.data.segundosRestantes<=0 || result.data.segundosRestantes>300)
            throw new Error('No se pudo verificar la retención. Vuelve a seleccionar el horario.');
        return result.data;
    }
    globalThis.ClinicaReservas = Object.freeze({preparar,reservar,retencion});
})();
