const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/disponibilidad.js', 'utf8');

function setup(fetch, step = 1000) {
    let now = 0;
    let submitted = 0;
    let listener;
    let pageshow;
    const element = () => ({textContent: '', attrs: {}, childNodes: [],
        setAttribute(name, value) { this.attrs[name] = value; },
        removeAttribute(name) { delete this.attrs[name]; },
        replaceChildren(...nodes) { this.childNodes = nodes; }});
    const button = {...element(), disabled: false, childNodes: ['Ingresar']};
    const message = element();
    const form = {
        attrs: {},
        append() {},
        checkValidity() { return true; },
        reportValidity() {},
        addEventListener(name, callback) { listener = callback; },
        querySelectorAll() { return [button]; },
        setAttribute(name, value) { this.attrs[name] = value; },
        removeAttribute(name) { delete this.attrs[name]; },
        requestSubmit(submitter) {
            let prevented = false;
            listener({submitter, preventDefault() { prevented = true; }});
            if (!prevented) submitted++;
        }
    };
    vm.runInNewContext(source, {
        document: {currentScript: {dataset: {readyUrl: '/health/ready'}},
            querySelectorAll: () => [form], createElement: (name) => name === 'p' ? message : element()},
        window: {addEventListener(name, callback) { if (name === 'pageshow') pageshow = callback; }},
        fetch, AbortController,
        Date: {now: () => { now += step; return now; }},
        setTimeout(fn, ms) { if (ms === 3000) queueMicrotask(fn); return 1; },
        clearTimeout() {}
    });
    return {button, message, form, pageshow: (persisted = true) => pageshow({persisted}), submitted: () => submitted,
        submit: () => listener({submitter: button, preventDefault() {}})};
}

const ready = {ok: true, json: async () => ({code: 'SISTEMA_DISPONIBLE'})};

test('waits for DB availability and sends exactly one normal login submission', async () => {
    let calls = 0;
    const ui = setup(async (url, options) => {
        assert.equal(url, '/health/ready');
        assert.equal(options.method, 'GET');
        assert.equal(options.body, undefined);
        return ++calls < 3 ? {ok: false} : ready;
    });
    await ui.submit();
    assert.equal(calls, 3);
    assert.equal(ui.submitted(), 1);
    assert.equal(ui.button.disabled, true);
    ui.pageshow(false);
    assert.equal(ui.button.disabled, true);
    assert.equal(ui.button.childNodes[1].textContent, 'Iniciando sesión…');
    assert.match(ui.message.textContent, /Validando/);
    await ui.submit();
    assert.equal(ui.submitted(), 1);
    ui.pageshow();
    assert.equal(ui.button.disabled, false);
    assert.deepEqual(ui.button.childNodes, ['Ingresar']);
});

test('does not send credentials when DB stays down or returns an unrelated successful page', async () => {
    const ui = setup(async () => ({ok: true, json: async () => ({code: 'OTRO'})}), 10000);
    await ui.submit();
    assert.equal(ui.submitted(), 0);
    assert.match(ui.message.textContent, /no se han enviado/);
    assert.equal(ui.button.disabled, false);
    assert.deepEqual(ui.button.childNodes, ['Ingresar']);
    assert.equal(ui.form.attrs['aria-busy'], undefined);
});

test('prevents double clicks while checking and recovers from a network error', async () => {
    let release;
    const ui = setup(() => new Promise((resolve) => { release = resolve; }));
    const first = ui.submit();
    await ui.submit();
    assert.equal(ui.button.disabled, true);
    release(ready);
    await first;
    assert.equal(ui.submitted(), 1);
    const offline = setup(async () => { throw new Error('offline'); }, 10000);
    await offline.submit();
    assert.equal(offline.submitted(), 0);
    assert.equal(offline.button.disabled, false);
});
