const test = require('node:test');
const assert = require('node:assert/strict');
const { iniciar } = require('../src/main/webapp/assets/js/padron-medicos.js');
function vista(cantidad) {
    const filas = Array.from({ length: cantidad }, () => ({ hidden: false }));
    const elementos = {};
    for (const id of ['medicosAnterior', 'medicosSiguiente', 'medicosPagina', 'medicosResumen']) {
        elementos[id] = { disabled: false, textContent: '', addEventListener(tipo, fn) { this[tipo] = fn; } };
    }
    elementos.padronMedicos = { querySelectorAll() { return filas; } };
    iniciar({ getElementById(id) { return elementos[id]; } });
    return { filas, elementos };
}
test('recorre las páginas sin perder ni repetir médicos y respeta los extremos', () => {
    const { filas, elementos: e } = vista(23);
    assert.equal(filas.filter(f => !f.hidden).length, 10);
    assert.equal(e.medicosAnterior.disabled, true);
    e.medicosSiguiente.click();
    assert.deepEqual(filas.map((f, i) => f.hidden ? -1 : i).filter(i => i >= 0), [10,11,12,13,14,15,16,17,18,19]);
    e.medicosSiguiente.click();
    assert.equal(e.medicosPagina.textContent, 'Página 3 de 3');
    assert.equal(filas.filter(f => !f.hidden).length, 3);
    assert.equal(e.medicosSiguiente.disabled, true);
    e.medicosSiguiente.click();
    assert.equal(e.medicosPagina.textContent, 'Página 3 de 3');
    e.medicosAnterior.click();
    assert.equal(e.medicosPagina.textContent, 'Página 2 de 3');
});
test('un resultado vacío o una sola página no permite avanzar', () => {
    for (const cantidad of [0, 1, 10]) {
        const { elementos: e } = vista(cantidad);
        assert.equal(e.medicosAnterior.disabled, true);
        assert.equal(e.medicosSiguiente.disabled, true);
        assert.equal(e.medicosPagina.textContent, 'Página 1 de 1');
    }
});
