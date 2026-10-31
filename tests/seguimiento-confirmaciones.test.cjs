const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const code = fs.readFileSync('src/main/webapp/assets/js/seguimiento-confirmaciones.js', 'utf8');

function entorno(conEventos = false) {
    const pendientes = new Map();
    const listeners = {};
    let secuencia = 0;
    const elements = new Map();
    const select = {value: ''};
    function element(id) {
        if (!elements.has(id)) elements.set(id, {
            dataset: {eventosUrl: 'http://localhost/admin/eventos-confirmaciones'}, hidden: false, textContent: '', href: 'http://localhost/admin/seguimiento-confirmaciones',
            childNodes: ['anterior'], attributes: {},
            addEventListener(name, callback) { listeners[id + ':' + name] = callback; },
            setAttribute(name, value) { this.attributes[name] = value; },
            removeAttribute(name) { delete this.attributes[name]; },
            replaceChildren(...nodes) { this.childNodes = nodes; }
        });
        return elements.get(id);
    }
    const panel = element('panel');
    panel.querySelectorAll = selector => selector.endsWith('select') ? [select] : [{reset() { select.value = ''; }}];
    const document = {
        hidden: false, activeElement: null,
        querySelector() { return panel; }, getElementById: element,
        importNode(node) { return node; },
        addEventListener(name, callback) { listeners['document:' + name] = callback; }
    };
    const timers = {
        setTimeout(fn, ms) { const id = ++secuencia; pendientes.set(id, {fn, ms}); return id; },
        clearTimeout(id) { pendientes.delete(id); }
    };
    class DOMParser {
        parseFromString(html) { return {getElementById() { return html.startsWith('valido:') ? {childNodes: [html]} : null; }}; }
    }
    const fuentes = [];
    class FuenteEventos {
        constructor(url) { this.url = url; this.listeners = {}; this.closed = false; fuentes.push(this); }
        addEventListener(name, callback) { this.listeners[name] = callback; }
        close() { this.closed = true; }
        emit(name) { this.listeners[name]?.(); }
    }
    const requests = [];
    let responder = async () => ({ok: true, status: 200, redirected: false, text: async () => 'valido:nuevo'});
    const context = vm.createContext({AbortController});
    vm.runInContext(code, context);
    const controller = context.ClinicaSeguimiento.iniciar({document, DOMParser, AbortController, EventSource: conEventos ? FuenteEventos : undefined, timers,
        fetch: (...args) => { requests.push(args); return responder(...args); }});
    return {controller, document, element, select, listeners, requests, pendientes, fuentes,
        responder(fn) { responder = fn; }};
}

test('actualiza automáticamente los datos sin recargar la página', async () => {
    const e = entorno();
    const tick = [...e.pendientes.values()].find(timer => timer.ms === 15000);
    assert.ok(tick);
    await tick.fn();
    assert.equal(e.requests.length, 1);
    assert.deepEqual(e.element('seguimientoDatos').childNodes, ['valido:nuevo']);
    assert.equal(e.requests[0][1].cache, 'no-store');
    assert.equal(e.requests[0][1].headers['X-Requested-With'], 'XMLHttpRequest');
});

test('conserva la selección pendiente y permite descartarla explícitamente', async () => {
    const e = entorno();
    e.select.value = 'CORREO';
    await e.controller.actualizar();
    assert.equal(e.requests.length, 0);
    assert.equal(e.select.value, 'CORREO');
    assert.equal(e.element('seguimientoDescartar').hidden, false);
    e.listeners['seguimientoDescartar:click']();
    assert.equal(e.select.value, '');
    assert.equal(e.requests.length, 1);
});

test('no sobrescribe una gestión que comienza durante la petición', async () => {
    const e = entorno();
    let resolver;
    e.responder(() => new Promise(resolve => { resolver = resolve; }));
    const primera = e.controller.actualizar();
    await e.controller.actualizar();
    assert.equal(e.requests.length, 1);
    e.select.value = 'SIN_RESPUESTA';
    resolver({ok: true, status: 200, text: async () => 'valido:nuevo'});
    await primera;
    assert.deepEqual(e.element('seguimientoDatos').childNodes, ['anterior']);
    assert.equal(e.select.value, 'SIN_RESPUESTA');
});

test('pausa las consultas con la pestaña oculta y reanuda al volver', async () => {
    const e = entorno();
    e.document.hidden = true;
    e.listeners['document:visibilitychange']();
    await e.controller.actualizar();
    assert.equal(e.requests.length, 0);
    assert.equal(e.pendientes.size, 0);
    e.document.hidden = false;
    e.listeners['document:visibilitychange']();
    assert.equal(e.requests.length, 1);
});

test('un error o una respuesta inválida conserva la tabla y permite reintentar', async () => {
    const e = entorno();
    e.responder(async () => ({ok: true, status: 200, text: async () => '<html>Error</html>'}));
    await e.controller.actualizar();
    assert.deepEqual(e.element('seguimientoDatos').childNodes, ['anterior']);
    assert.equal(e.element('seguimientoError').hidden, false);
    assert.ok([...e.pendientes.values()].some(timer => timer.ms === 15000));
});

test('detiene consultas al vencer la sesión y no muestra el login dentro de la tabla', async () => {
    const e = entorno();
    e.responder(async () => ({ok: true, status: 200, redirected: true}));
    await e.controller.actualizar();
    await e.controller.actualizar();
    assert.equal(e.requests.length, 1);
    assert.deepEqual(e.element('seguimientoDatos').childNodes, ['anterior']);
    assert.equal(e.pendientes.size, 0);
    assert.equal(e.element('seguimientoError').hidden, false);
});

 test('un evento de sede actualiza la tabla inmediatamente sin esperar el respaldo', async () => {
    const e = entorno(true);
    e.fuentes[0].emit('open');
    assert.ok([...e.pendientes.values()].some(t => t.ms === 60000));
    e.fuentes[0].emit('cambio');
    await new Promise(setImmediate);
    assert.equal(e.requests.length, 1);
    assert.deepEqual(e.element('seguimientoDatos').childNodes, ['valido:nuevo']);
 });
 test('una desconexión activa el respaldo y una reconexión recupera el modo en vivo', () => {
    const e = entorno(true);
    e.fuentes[0].emit('open');
    e.fuentes[0].emit('error');
    assert.ok([...e.pendientes.values()].some(t => t.ms === 15000));
    e.fuentes[0].emit('open');
    assert.equal([...e.pendientes.values()].some(t => t.ms === 15000), false);
    assert.ok([...e.pendientes.values()].some(t => t.ms === 60000));
 });
 test('los eventos no borran selecciones y el canal se cierra al ocultar la pestaña', () => {
    const e = entorno(true);
    e.select.value = 'CORREO';
    e.fuentes[0].emit('cambio');
    assert.equal(e.requests.length, 0);
    assert.equal(e.select.value, 'CORREO');
    e.document.hidden = true;
    e.listeners['document:visibilitychange']();
    assert.equal(e.fuentes[0].closed, true);
    e.document.hidden = false;
    e.listeners['document:visibilitychange']();
    assert.equal(e.fuentes.length, 2);
 });
 test('detiene y reanuda el canal al regresar desde la caché de navegación', () => {
    const e = entorno(true);
    e.controller.detener();
    assert.equal(e.fuentes[0].closed, true);
    e.controller.reanudar();
    assert.equal(e.fuentes.length, 2);
 });
