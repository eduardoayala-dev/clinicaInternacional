const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/recuperacion.js', 'utf8');
function pantalla() {
    const campos = new Map(); const listeners = []; const eventos = {};
    const boton = { disabled: false, textContent: 'Guardar', dataset: {} };
    const campo = (value) => ({ value, error: '', setCustomValidity(message) { this.error = message; }, addEventListener() {} });
    campos.set('nuevaPassword', campo('NuevaClave!2026Segura')); campos.set('confirmarPassword', campo('NuevaClave!2026Segura'));
    const form = { closest() { return null; }, addEventListener(name, listener) { listeners.push(listener); }, reportValidity() { return !campos.get('nuevaPassword').error && !campos.get('confirmarPassword').error; }, querySelector() { return boton; } };
    campos.set('restablecerForm', form);
    vm.runInNewContext(source, { TextEncoder, window: { addEventListener(n,fn) { eventos[n] = fn; } }, document: { getElementById: (id) => campos.get(id), querySelectorAll: (selector) => selector.startsWith('button') ? [boton] : [form] } });
    return { campos, form, boton, listeners, eventos };
}
const evento = () => ({ defaultPrevented: false, preventDefault() { this.defaultPrevented = true; } });
test('contraseñas diferentes impiden envío sin bloquear el botón', () => {
    const p = pantalla(); p.campos.get('confirmarPassword').value = 'diferente'; const e = evento(); p.listeners.forEach(fn => fn(e)); assert.equal(e.defaultPrevented,true); assert.equal(p.boton.disabled,false);
});
test('envío válido muestra espera y bloquea botón', () => {
    const p = pantalla(); p.listeners.forEach(fn => fn(evento())); assert.equal(p.boton.disabled,true); assert.equal(p.boton.textContent,'Procesando…');
});
test('límite de bytes UTF-8 evita truncado de BCrypt', () => {
    const p = pantalla(); p.campos.get('nuevaPassword').value = 'Á'.repeat(40); p.campos.get('confirmarPassword').value = 'Á'.repeat(40); const e = evento(); p.listeners.forEach(fn => fn(e)); assert.equal(e.defaultPrevented,true);
});
test('regreso desde caché del navegador recupera botón', () => {
    const p = pantalla(); p.listeners.forEach(fn => fn(evento())); p.eventos.pageshow(); assert.equal(p.boton.disabled,false); assert.equal(p.boton.textContent,'Guardar');
});
