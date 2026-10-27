const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/consulta-dni.js', 'utf8');

function setup(fetch, globals = {}) {
    const element = (value = '') => ({value, listeners: {}, attrs: {},
        addEventListener(name, callback) { this.listeners[name] = callback; },
        setAttribute(name, value) { this.attrs[name] = value; },
        removeAttribute(name) { delete this.attrs[name]; }});
    const form = element();
    form.elements = {nombres: element(), apellidos: element(), csrfToken: element('csrf-simulado')};
    const input = element('99000001'), type = element('DNI'), status = element();
    input.dataset = {endpoint: '/api/v1/documentos/dni/consulta'};
    const nodes = {registerForm: form, regDni: input, regTipoDoc: type, dniLookupStatus: status};
    vm.runInNewContext(source, {document: {getElementById: id => nodes[id]}, fetch, AbortController, setTimeout, clearTimeout, ...globals});
    return {form, input, type, status, blur: () => input.listeners.blur()};
}
const data = {numero: '99000001', nombres: 'PACIENTE PRUEBA', apellidoPaterno: 'FICTICIO', apellidoMaterno: 'LOCAL'};
const ok = async () => ({ok: true, json: async () => ({code: 'DNI_ENCONTRADO', data})});

test('consulta al salir del DNI y bloquea nombres y apellidos, sin botón', async () => {
    let calls = 0;
    const ui = setup(async (url, options) => {
        calls++;
        assert.equal(url, '/api/v1/documentos/dni/consulta');
        assert.equal(options.headers['X-CSRF-Token'], 'csrf-simulado');
        assert.equal(options.headers.Authorization, undefined);
        assert.equal(options.body, '{"numero":"99000001"}');
        return ok();
    });
    assert.equal(calls, 0);
    await ui.blur();
    assert.equal(ui.form.elements.nombres.value, data.nombres);
    assert.equal(ui.form.elements.apellidos.value, 'FICTICIO LOCAL');
    assert.equal(ui.form.elements.nombres.readOnly, true);
    assert.equal(ui.form.elements.apellidos.readOnly, true);
    await ui.blur();
    assert.equal(calls, 1);
});

test('errores desbloquean la entrada manual y no repiten consultas al mismo DNI', async () => {
    for (const fetch of [async () => { throw new Error('sin red'); },
        async () => ({ok: false, json: async () => ({message: 'Completa manualmente.'})})]) {
        let calls = 0;
        const ui = setup((...args) => { calls++; return fetch(...args); });
        await ui.blur();
        await ui.blur();
        assert.equal(calls, 1);
        assert.equal(ui.form.elements.nombres.readOnly, false);
        assert.equal(ui.form.elements.apellidos.readOnly, false);
        assert.match(ui.status.textContent, /manual/);
    }
});

test('no consulta CE ni DNI incompleto y permite nombres manuales para CE', async () => {
    let calls = 0; const ui = setup(async () => { calls++; return ok(); });
    ui.input.value = '123'; await ui.blur();
    ui.type.value = 'CE'; ui.type.listeners.change(); await ui.blur();
    assert.equal(calls, 0);
    assert.equal(ui.form.elements.nombres.readOnly, false);
    assert.equal(ui.input.maxLength, 15);
});

test('cambiar DNI descarta respuestas antiguas y limpia los datos autocompletados', async () => {
    let complete;
    const ui = setup(() => new Promise(resolve => { complete = resolve; }));
    const pending = ui.blur();
    ui.input.value = '99000002'; ui.input.listeners.input();
    complete(await ok()); await pending;
    assert.equal(ui.form.elements.nombres.value, '');
    const another = setup(ok); await another.blur();
    another.input.value = '99000002'; another.input.listeners.input();
    assert.equal(another.form.elements.nombres.value, '');
    assert.equal(another.form.elements.apellidos.value, '');
    assert.equal(another.form.elements.nombres.readOnly, true);
});

test('varios blur durante una consulta no duplican la solicitud ni permiten enviar', async () => {
    let calls = 0, complete;
    const ui = setup(() => { calls++; return new Promise(resolve => { complete = resolve; }); });
    const pending = ui.blur();
    await ui.blur();
    let prevented = false;
    ui.form.listeners.submit({preventDefault() { prevented = true; }});
    assert.equal(prevented, true);
    assert.equal(calls, 1);
    complete(await ok()); await pending;
    assert.equal(ui.status.attrs['aria-busy'], undefined);
});

test('el formulario muestra documento antes de nombres y conserva fecha y sexo manuales', () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/login-paciente.jsp', 'utf8');
    assert.ok(jsp.indexOf('id="regTipoDoc"') < jsp.indexOf('name="nombres"'));
    assert.ok(jsp.indexOf('id="regDni"') < jsp.indexOf('name="nombres"'));
    assert.ok(!jsp.includes('btnConsultarDni'));
    assert.match(jsp, /name="fechaNacimiento"/);
    assert.match(jsp, /name="genero"/);
});

test('muestra la espera del límite y permite reintentar el mismo DNI cuando vence', async () => {
    let now = 1000, calls = 0;
    const ui = setup(async () => {
        calls++;
        if (calls > 1) return ok();
        return {ok: false, status: 429, headers: {get: () => '120'},
            json: async () => ({message: 'Límite de consultas'})};
    }, {Date: class extends Date { static now() { return now; } }});
    await ui.blur();
    assert.match(ui.status.textContent, /2 minutos/);
    assert.equal(ui.form.elements.nombres.readOnly, false);
    now += 61000; await ui.blur();
    assert.equal(calls, 1);
    assert.match(ui.status.textContent, /59 segundos/);
    now += 60000; await ui.blur();
    assert.equal(calls, 2);
    assert.equal(ui.form.elements.nombres.value, data.nombres);
    assert.equal(ui.form.elements.nombres.readOnly, true);
});

test('un Retry-After ausente o inválido conserva el mensaje del servidor', async () => {
    for (const header of [null, 'NaN', '-5', '100000000']) {
        const ui = setup(async () => ({ok: false, status: 429, headers: {get: () => header},
            json: async () => ({message: 'Completa manualmente.'})}));
        await ui.blur();
        assert.equal(ui.status.textContent, 'Completa manualmente.');
    }
});
