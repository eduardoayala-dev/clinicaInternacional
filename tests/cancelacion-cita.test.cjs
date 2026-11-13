const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/cancelacion-cita.js', 'utf8');

function pantalla() {
    const eventos = {};
    const confirmar = { disabled: false, textContent: 'Sí, cancelar cita' };
    const mantener = { disabled: false };
    const atributos = {};
    let enviar;
    const formulario = {
        querySelector: () => confirmar,
        querySelectorAll: () => [confirmar, mantener],
        setAttribute(n, v) { atributos[n] = v; },
        removeAttribute(n) { delete atributos[n]; },
        addEventListener(n, fn) { if (n === 'submit') enviar = fn; }
    };
    vm.runInNewContext(source, {
        document: { getElementById: () => formulario },
        window: { addEventListener(n, fn) { eventos[n] = fn; } }
    });
    return { confirmar, mantener, atributos, enviar, eventos };
}
function evento() {
    return { defaultPrevented: false, preventDefault() { this.defaultPrevented = true; } };
}

test('permite un envío y bloquea clics posteriores mientras responde el servidor', () => {
    const p = pantalla(); const primero = evento(); p.enviar(primero);
    assert.equal(primero.defaultPrevented, false);
    assert.equal(p.confirmar.disabled, true); assert.equal(p.mantener.disabled, true);
    assert.equal(p.confirmar.textContent, 'Cancelando…');
    assert.equal(p.atributos['aria-busy'], 'true');
    const segundo = evento(); p.enviar(segundo);
    assert.equal(segundo.defaultPrevented, true);
});

test('la renovación previa de sesión no bloquea el envío autorizado después', () => {
    const p = pantalla(); const previo = evento(); previo.preventDefault(); p.enviar(previo);
    assert.equal(p.confirmar.disabled, false);
    const autorizado = evento(); p.enviar(autorizado);
    assert.equal(autorizado.defaultPrevented, false); assert.equal(p.confirmar.disabled, true);
});

test('volver desde la caché del navegador recupera el formulario', () => {
    const p = pantalla(); p.enviar(evento()); p.eventos.pageshow();
    assert.equal(p.confirmar.disabled, false); assert.equal(p.mantener.disabled, false);
    assert.equal(p.confirmar.textContent, 'Sí, cancelar cita');
    assert.equal(p.atributos['aria-busy'], undefined);
    const nuevo = evento(); p.enviar(nuevo); assert.equal(nuevo.defaultPrevented, false);
});
