const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/consulta-dni.js', 'utf8');

function setup(fetch) {
    const element = (value = '') => ({value, listeners: {}, addEventListener(name, callback) { this.listeners[name] = callback; }, focus() { this.focused = true; }});
    const form = {elements: {nombres: element(), apellidos: element(), csrfToken: element('csrf-simulado')}};
    const input = element('99000001'), type = element('DNI');
    const button = element(); button.dataset = {endpoint: '/api/v1/documentos/dni/consulta'};
    const status = element();
    const nodes = {registerForm: form, regDni: input, regTipoDoc: type, btnConsultarDni: button, dniLookupStatus: status};
    vm.runInNewContext(source, {document: {getElementById: id => nodes[id]}, fetch, AbortController, setTimeout, clearTimeout});
    return {form, input, type, button, status, click: () => button.listeners.click()};
}
const data = {numero: '99000001', nombres: 'PACIENTE PRUEBA', apellidoPaterno: 'FICTICIO', apellidoMaterno: 'LOCAL'};
const ok = async () => ({ok: true, json: async () => ({code: 'DNI_ENCONTRADO', data})});

test('envía CSRF al servidor propio y completa los dos apellidos sin HTML', async () => {
    const ui = setup(async (url, options) => {
        assert.equal(url, '/api/v1/documentos/dni/consulta');
        assert.equal(options.headers['X-CSRF-Token'], 'csrf-simulado');
        assert.equal(options.headers.Authorization, undefined);
        assert.equal(options.body, '{"numero":"99000001"}');
        return ok();
    });
    await ui.click();
    assert.equal(ui.form.elements.nombres.value, data.nombres);
    assert.equal(ui.form.elements.apellidos.value, 'FICTICIO LOCAL');
    assert.equal(ui.button.disabled, false);
});

test('errores y ausencia del proveedor conservan el registro manual', async () => {
    for (const fetch of [async () => { throw new Error('sin red'); }, async () => ({ok: false, json: async () => ({message: 'Completa manualmente.'})})]) {
        const ui = setup(fetch); ui.form.elements.nombres.value = 'MANUAL';
        await ui.click();
        assert.equal(ui.form.elements.nombres.value, 'MANUAL');
        assert.equal(ui.button.disabled, false);
        assert.match(ui.status.textContent, /manual/);
    }
});

test('no consulta CE ni DNI incompleto', async () => {
    let calls = 0; const ui = setup(async () => { calls++; return ok(); });
    ui.input.value = '123'; await ui.click();
    ui.type.value = 'CE'; ui.type.listeners.change(); await ui.click();
    assert.equal(calls, 0); assert.equal(ui.button.disabled, true);
    assert.equal(ui.input.maxLength, 15);
});

test('descarta respuestas antiguas al cambiar DNI y limpia solo el autocompletado', async () => {
    let complete;
    const ui = setup(() => new Promise(resolve => { complete = resolve; }));
    const pending = ui.click();
    ui.input.value = '99000002'; ui.input.listeners.input();
    complete(await ok()); await pending;
    assert.equal(ui.form.elements.nombres.value, '');
    const another = setup(ok); await another.click();
    another.form.elements.nombres.value = 'EDITADO';
    another.input.value = '99000002'; another.input.listeners.input();
    assert.equal(another.form.elements.nombres.value, 'EDITADO');
    assert.equal(another.form.elements.apellidos.value, '');
});

test('no sobrescribe lo que el paciente escribe mientras espera', async () => {
    let complete;
    const ui = setup(() => new Promise(resolve => { complete = resolve; }));
    const pending = ui.click(); ui.form.elements.nombres.value = 'EDICIÓN MANUAL';
    complete(await ok()); await pending;
    assert.equal(ui.form.elements.nombres.value, 'EDICIÓN MANUAL');
    assert.match(ui.status.textContent, /Conservamos/);
});
