const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/reservas-api.js', 'utf8');
function api(fetch) {
    const context = {fetch};
    vm.runInNewContext(source, context);
    return context.ClinicaReservas;
}
function reply(status, body) { return {status, ok: status >= 200 && status < 300, json: async () => body}; }

test('solo confirma la reserva con identificador y código devueltos por el servidor', async () => {
    const reservas = api(async (url, options) => {
        assert.equal(url, '/paciente/reservar');
        assert.equal(options.method, 'POST');
        assert.equal(options.headers['X-CSRF-Token'], 'csrf');
        assert.equal(options.credentials, 'same-origin');
        return reply(201, {code: 'CITA_RESERVADA', data: {idCita: 72, codigoCita: 'CI-2026-000072'}});
    });
    const data = await reservas.reservar('/paciente/reservar', 'params', 'csrf');
    assert.equal(data.idCita, 72);
});

test('un conflicto o error de sesión nunca se transforma en comprobante', async () => {
    for (const status of [400, 403, 409, 500, 503]) {
        const reservas = api(async () => reply(status, {message: 'No disponible', data: null}));
        await assert.rejects(reservas.reservar('', '', ''), /No disponible/);
    }
});

test('una respuesta antigua, inválida o sin identidad no confirma la reserva', async () => {
    for (const body of [{success: true}, {code: 'CITA_RESERVADA', data: {idCita: 0, codigoCita: 'CI-2026-000000'}},
        {code: 'CITA_RESERVADA', data: {idCita: 1}}, {code: 'OTRO', data: {idCita: 1, codigoCita: 'CI-2026-1'}}]) {
        await assert.rejects(api(async () => reply(201, body)).reservar('', '', ''), /Revisa Mis citas/);
    }
});

test('ante fallos de red o HTML pide revisar citas sin inventar un éxito', async () => {
    for (const fetch of [async () => { throw new Error('offline'); }, async () => ({json: async () => { throw new Error('HTML'); }})]) {
        await assert.rejects(api(fetch).reservar('', '', ''), /Revisa Mis citas/);
    }
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/paciente/reservar_cita.jsp', 'utf8');
    assert.ok(jsp.includes('ClinicaReservas.reservar'));
    assert.ok(!jsp.includes('Math.random'));
    assert.ok(!jsp.includes('codFallback'));
});
