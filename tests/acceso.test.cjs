const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
test('mostrar contraseña conserva el valor y comunica el estado', () => {
    const input = { type: 'password', value: 'valor-de-prueba' };
    let click;
    const button = { dataset: { passwordToggle: 'password' }, setAttribute(name, value) { this[name] = value; }, addEventListener(name, fn) { click = fn; } };
    vm.runInNewContext(fs.readFileSync('src/main/webapp/assets/js/acceso.js', 'utf8'), { document: { querySelectorAll: () => [button], getElementById: () => input } });
    click();assert.equal(input.type, 'text');assert.equal(button['aria-pressed'], 'true');assert.equal(input.value, 'valor-de-prueba');
    click();assert.equal(input.type, 'password');assert.equal(button['aria-pressed'], 'false');
});
