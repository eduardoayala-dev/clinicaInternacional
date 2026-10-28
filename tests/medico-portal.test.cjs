const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/medico-atencion.js', 'utf8');
function portal(disabled = false) {
    const fields = new Map();
    function element(id) {
        if (!fields.has(id)) fields.set(id, {value: '', defaultValue: '', textContent: ''});
        return fields.get(id);
    }
    const selected = {classList: {add() { selected.active = true; }}};
    const group = {disabled};
    const context = {document: {getElementById: element, querySelector: () => group,
        querySelectorAll: () => [], addEventListener: (event, fn) => { context.init = fn; }}};
    vm.runInNewContext(source, context);
    return {context, element, selected, group};
}
test('restablecer conserva los valores originales sin inventar notas ni fechas', () => {
    const {context, element} = portal();
    element('indicacionesTextarea').defaultValue = 'Seguimiento real';
    element('fechaProximaInput').defaultValue = '2026-12-01';
    context.restablecerFormulario();
    assert.equal(element('indicacionesTextarea').value, 'Seguimiento real');
    assert.equal(element('fechaProximaInput').value, '2026-12-01');
    assert.equal(element('badgeMeses').textContent, '2026-12-01');
});
test('un registro de solo lectura no cambia prioridad ni seguimiento desde los botones', () => {
    const {context, element, selected} = portal(true);
    element('prioridadInput').value = 'ALTA'; element('indicacionesTextarea').value = 'Original';
    context.setPrioridad('BAJA', selected); context.restablecerFormulario();
    assert.equal(element('prioridadInput').value, 'ALTA');
    assert.equal(element('indicacionesTextarea').value, 'Original'); assert.equal(selected.active, undefined);
});
test('sin seguimiento se muestra ausencia de fecha y el contador corresponde al contenido', () => {
    const {context, element} = portal();
    element('indicacionesTextarea').value = 'Niño';context.init();
    assert.equal(element('charCounter').textContent, '4 / 200 caracteres');
    assert.equal(element('badgeMeses').textContent, 'Sin fecha de seguimiento');
});
test('las cuatro vistas protegen su salida y cargan la hoja de estilos del portal', () => {
    for (const file of ['inicio.jsp', 'agenda.jsp', 'atencion.jsp', 'perfil.jsp']) {
        const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/medico/' + file, 'utf8');
        assert.match(jsp, /assets\/css\/portal-medico\.css/);
        for (const expression of jsp.matchAll(/<%=([\s\S]*?)%>/g)) assert.match(expression[1].trim(), /^Html\.texto\(/);
        for (const script of jsp.matchAll(/<script>([\s\S]*?)<\/script>/g)) {
            assert.doesNotThrow(() => new vm.Script(script[1].replace(/<%[\s\S]*?%>/g, '0').replace(/\$\{[^}]*\}/g, '')));
        }
        assert.doesNotMatch(jsp, /Dr\. Carlos Mendoza|Penicilina|2027-01-15|Consultorio 102|LLEGO_TARDE|idx ==|i == 2/);
    }
});
test('la inasistencia usa un formulario propio con sesión y cita, sin alterar el seguimiento', () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/medico/atencion.jsp', 'utf8');
    const form = jsp.match(/<form id="formInasistencia"[\s\S]*?<\/form>/)[0];
    assert.match(form, /name="csrfToken"/); assert.match(form, /name="idCita"/); assert.match(form, /name="accion" value="no_asistio"/);
    assert.doesNotMatch(form, /name="prioridad"|name="fechaProxima"|name="indicaciones"/);
    assert.match(jsp, /form="formInasistencia"/);
});
