const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/medico-atencion.js', 'utf8');
function portal(disabled = false, prioridad = 'MEDIA') {
    const fields = new Map();
    function element(id) {
        if (!fields.has(id)) fields.set(id, {value: '', defaultValue: '', textContent: ''});
        return fields.get(id);
    }
    element('prioridadInput').value = prioridad;
    const cards = ['ALTA', 'MEDIA', 'BAJA'].map(value => {
        const card = {dataset: {prioridad: value}, active: value === prioridad, attributes: {},
            setAttribute(name, content) { this.attributes[name] = content; }};
        card.classList = {add() { card.active = true; }, remove() { card.active = false; }};
        return card;
    });
    const selected = cards[2];
    const group = {disabled};
    const context = {document: {getElementById: element,
        querySelector: selector => selector.includes('fieldset') ? group : cards.find(card => selector.includes('"'+card.dataset.prioridad+'"')),
        querySelectorAll: () => cards, addEventListener: (event, fn) => { context.init = fn; }}};
    vm.runInNewContext(source, context);
    return {context, element, selected, group, cards};
}

test('restablecer conserva los valores originales sin inventar notas ni fechas', () => {
    const {context, element} = portal();
    element('indicacionesTextarea').defaultValue = 'Seguimiento real';
    element('fechaProximaInput').defaultValue = '2026-12-01';
    context.restablecerFormulario();
    assert.equal(element('indicacionesTextarea').value, 'Seguimiento real');
    assert.equal(element('fechaProximaInput').value, '2026-12-01');
    assert.equal(element('badgeMeses').textContent, '01/12/2026');
});
test('un registro de solo lectura no cambia prioridad ni seguimiento desde los botones', () => {
    const {context, element, selected} = portal(true);
    element('prioridadInput').value = 'ALTA'; element('indicacionesTextarea').value = 'Original';
    context.setPrioridad('BAJA', selected); context.restablecerFormulario();
    assert.equal(element('prioridadInput').value, 'ALTA');
    assert.equal(element('indicacionesTextarea').value, 'Original'); assert.equal(selected.active, false);
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
test('iniciar y no atender tienen formularios propios protegidos; la inasistencia corresponde a admisión', () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/medico/atencion.jsp', 'utf8');
    for (const [id, accion] of [['formIniciarAtencion', 'iniciar'], ['formNoAtendida', 'no_atender']]) {
        const form = jsp.match(new RegExp('<form id="'+id+'"[\\s\\S]*?<\\/form>'))[0];
        assert.match(form, /name="csrfToken"/); assert.match(form, /name="idCita"/);
        assert.ok(form.includes('value="'+accion+'"'));
        assert.doesNotMatch(form, /name="prioridad"|name="fechaProxima"|name="indicaciones"/);
    }
    assert.doesNotMatch(jsp, /formInasistencia|value="no_asistio"/);
    assert.match(jsp, /name="motivo"/);
});

test('el historial muestra resultados reales y protege los nombres al abrir el detalle', () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/paciente/historial.jsp', 'utf8');
    for (const expression of jsp.matchAll(/<%=([\s\S]*?)%>/g)) assert.match(expression[1].trim(), /^Html\.texto\(/);
    assert.doesNotMatch(jsp, /Ver constancia|abrirModalDetalle\('\s*<%=/);
    assert.match(jsp, /data-medico=/); assert.match(jsp, /this\.dataset\.medico/);
    for (const script of jsp.matchAll(/<script>([\s\S]*?)<\/script>/g)) {
        assert.doesNotThrow(() => new vm.Script(script[1].replace(/<%[\s\S]*?%>/g, '0').replace(/\$\{[^}]*\}/g, '')));
    }
});

test('restablecer recupera la prioridad original y mantiene su estado accesible', () => {
    const {context, element, selected, cards} = portal(false, 'ALTA');
    context.setPrioridad('BAJA', selected);
    assert.equal(element('prioridadInput').value, 'BAJA');
    assert.equal(selected.attributes['aria-pressed'], 'true');
    context.restablecerFormulario();
    assert.equal(element('prioridadInput').value, 'ALTA');
    assert.deepEqual(cards.map(card => card.active), [true, false, false]);
    assert.equal(cards[0].attributes['aria-pressed'], 'true');
    assert.equal(selected.attributes['aria-pressed'], 'false');
});

test('restablecer recupera también el contenido original del registro clínico', () => {
    const {context, element} = portal();
    for (const id of ['motivoClinico', 'antecedentesClinicos', 'hallazgosClinicos', 'evaluacionClinica', 'diagnosticoClinico', 'planClinico']) {
        element(id).defaultValue = 'Original: niño & revisión';
        element(id).value = 'Modificado';
    }
    context.restablecerFormulario();
    for (const id of ['motivoClinico', 'antecedentesClinicos', 'hallazgosClinicos', 'evaluacionClinica', 'diagnosticoClinico', 'planClinico']) assert.equal(element(id).value, 'Original: niño & revisión');
});
