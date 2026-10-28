const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/ofertas-timer.js', 'utf8');
function reloj(presente = true) {
    let ahora = 0, tick;
    const timer = {}, button = {}, destinos = [];
    const context = {
        performance: {now: () => ahora},
        document: {getElementById: () => presente ? timer : null, querySelectorAll: () => [button]},
        window: {location: {replace: destino => destinos.push(destino)}},
        setInterval: callback => { tick = callback; return 1; },
        clearInterval: () => {}
    };
    vm.runInNewContext(source, context);
    return {timer, button, destinos, iniciar: context.ClinicaOfertas.iniciar, avanzar: ms => { ahora = ms; tick(); }};
}
test('usa el tiempo real del servidor y compensa una pestaña suspendida', () => {
    const r = reloj(); r.iniciar(120, '/paciente/notificaciones');
    assert.equal(r.timer.textContent, '02:00 min');
    r.avanzar(90500); assert.equal(r.timer.textContent, '00:30 min');
    r.avanzar(121000); assert.equal(r.timer.textContent, '00:00 min');
    assert.equal(r.button.disabled, true);
    assert.deepEqual(r.destinos, ['/paciente/notificaciones']);
});
test('sin una oferta visible no inicia una cuenta ni redirige', () => {
    const r = reloj(false); r.iniciar(0, '/paciente/notificaciones');
    assert.deepEqual(r.destinos, []);
});
test('una oferta vencida bloquea los botones y vuelve a consultar el estado', () => {
    const r = reloj(); r.iniciar(-1, '/paciente/notificaciones');
    assert.equal(r.button.disabled, true);
    assert.deepEqual(r.destinos, ['/paciente/notificaciones']);
});
test('el médico preferido se filtra por especialidad y sede y descarta una selección incompatible', () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/paciente/lista_espera.jsp', 'utf8');
    const start = jsp.indexOf('function filtrarMedicosPorEspecialidad()');
    const options = [{value: '', dataset: {}}, {value: '1', dataset: {esp: '1', sede: '1'}},
        {value: '2', dataset: {esp: '1', sede: '2'}}, {value: '3', dataset: {esp: '2', sede: '1'}}];
    const medico = {value: '2', selectedIndex: 2, options, querySelectorAll: () => options};
    const sede = {value: '1'};
    const elements = {selectEspecialidadEspera: {value: '1'}, selectMedicoEspera: medico, inputSedeEspera: sede};
    const context = {document: {getElementById: id => elements[id]}};
    vm.runInNewContext(jsp.slice(start, jsp.indexOf('</script>', start)), context);
    context.filtrarMedicosPorEspecialidad();
    assert.equal(options[1].disabled, false); assert.equal(options[2].disabled, true);
    assert.equal(options[3].disabled, true); assert.equal(medico.value, '');
    sede.value = 'cualquiera'; context.filtrarMedicosPorEspecialidad();
    assert.equal(options[2].disabled, false); assert.equal(options[3].disabled, true);
});
test('los scripts de lista y notificaciones mantienen sintaxis válida', () => {
    for (const file of ['lista_espera.jsp', 'notificaciones.jsp']) {
        const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/paciente/' + file, 'utf8');
        for (const script of jsp.matchAll(/<script>([\s\S]*?)<\/script>/g)) {
            const code = script[1].replace(/<%[\s\S]*?%>/g, 'null').replace(/\$\{[^}]*\}/g, '0');
            assert.doesNotThrow(() => new vm.Script(code));
        }
    }
});
