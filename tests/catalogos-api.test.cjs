const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/catalogos-api.js', 'utf8');

function api(fetch) {
    const context = {fetch, URLSearchParams};
    vm.runInNewContext(source, context);
    return context.ClinicaCatalogos;
}
function response(data, status = 200, message = 'Consulta completada.') {
    return {ok: status === 200, status, json: async () => ({code: 'CATALOGO', message, data})};
}

test('envía filtros y credenciales de sesión y extrae data del contrato', async () => {
    const catalogos = api(async (url, options) => {
        assert.equal(url, '/clinica/api/medicos?idEspecialidad=2&idSede=3');
        assert.equal(options.credentials, 'same-origin');
        assert.equal(options.cache, 'no-store');
        return response([{idMedico: 42, nombreCompleto: 'Médico Prueba', cmp: ''}]);
    });
    const data = await catalogos.medicos('/clinica', 2, 3);
    assert.equal(data[0].idMedico, 42);
});

test('lista vacía permanece vacía, sin consultas adicionales de otra sede', async () => {
    let calls = 0;
    const catalogos = api(async url => { calls++; assert.equal(url, '/api/medicos?idEspecialidad=1'); return response([]); });
    assert.equal((await catalogos.medicos('', 1, '0')).length, 0);
    assert.equal(calls, 1);
});

test('distingue errores HTTP y no los transforma en listas vacías', async () => {
    for (const status of [400, 403, 500, 503]) {
        const catalogos = api(async () => response(null, status, 'Catálogo no disponible.'));
        await assert.rejects(catalogos.medicos('', 1, 1), error => error.status === status && error.message === 'Catálogo no disponible.');
    }
});

test('rechaza respuestas antiguas, cuerpos inválidos y médicos sin identidad válida', async () => {
    for (const body of [[], {}, {code: 'OK', message: 'OK', data: null},
        {code: 'OK', message: 'OK', data: [null]}, {code: 'OK', message: 'OK', data: [{idMedico: 0}]}]) {
        const catalogos = api(async () => ({ok: true, json: async () => body}));
        await assert.rejects(catalogos.medicos('', 1, 1));
    }
    const invalid = api(async () => ({ok: true, json: async () => { throw new SyntaxError('JSON'); }}));
    await assert.rejects(invalid.medicos('', 1, 1), /interpretar/);
});

test('valida horas y fechas y maneja fallos de red', async () => {
    const hours = api(async url => { assert.match(url, /fecha=2026-10-28/); return response(['09:00', '14:30']); });
    assert.equal((await hours.horarios('', 42, '2026-10-28')).length, 2);
    const dates = api(async () => response(['2026-10-28']));
    assert.equal((await dates.horarios('', 42))[0], '2026-10-28');
    const invalid = api(async () => response(['25:00']));
    await assert.rejects(invalid.horarios('', 42, '2026-10-28'), /formato/);
    const offline = api(async () => { throw new TypeError('Network failure'); });
    await assert.rejects(offline.medicos('', 1, 1), /conectar/);
});

function functions(path, names) {
    const jsp = fs.readFileSync(path, 'utf8');
    return names.map(name => {
        const pattern = new RegExp('^        (?:async )?function ' + name + '\\(', 'm');
        const match = pattern.exec(jsp);
        assert.ok(match, name + ' debe existir');
        const tail = jsp.slice(match.index + match[0].length);
        const end = /^        (?:async )?function |^    <\/script>/m.exec(tail);
        return jsp.slice(match.index, match.index + match[0].length + (end ? end.index : tail.length));
    }).join('\n').replace(/<%[\s\S]*?%>/g, 'null').replace(/\$\{pageContext.request.contextPath\}/g, '');
}

test('reserva muestra solo horarios recibidos y descarta respuestas de búsquedas anteriores', async () => {
    const container = {children: [], replaceChildren() { this.children = []; }, appendChild(child) { this.children.push(child); }};
    const badge = {};
    const context = {
        state: {fecha: '2026-10-28', turno: 'MANANA', medicoId: '0', especialidadId: '1', sedeId: '1'},
        document: {getElementById: () => container, querySelector: () => badge},
        lucide: {createIcons() { this.rendered = true; }},
        ClinicaCatalogos: {medicos: async () => [{idMedico: 42}], horarios: async () => ['09:00', '14:30']},
        mostrarEstadoCatalogo(target, message) { target.replaceChildren(); target.message = message; },
        tarjetaMedico(medico, horas) { return {id: medico.idMedico, horas}; }
    };
    vm.createContext(context);
    vm.runInContext('let grillaRequest = 0;\n' + functions('src/main/webapp/WEB-INF/views/paciente/reservar_cita.jsp',
        ['cargarGrillaHorarios', 'coincideTurno']), context);
    await context.cargarGrillaHorarios();
    assert.equal(container.children.length, 1);
    assert.deepEqual(Array.from(container.children[0].horas), ['09:00']);
    assert.equal(badge.textContent, '1 cupo');
    let complete;
    context.ClinicaCatalogos.medicos = () => new Promise(resolve => { complete = resolve; });
    const old = context.cargarGrillaHorarios();
    context.ClinicaCatalogos.medicos = async () => [];
    await context.cargarGrillaHorarios();
    complete([{idMedico: 42}]); await old;
    assert.equal(container.children.length, 0);
    assert.equal(badge.textContent, '0 cupos');
    context.ClinicaCatalogos.medicos = async () => { throw new Error('Servicio no disponible'); };
    await context.cargarGrillaHorarios();
    assert.equal(container.children.length, 0);
    assert.equal(badge.textContent, 'Sin verificar');
    assert.equal(container.message, 'Servicio no disponible');
});

test('reprogramación permite un médico real y bloquea identificadores vacíos o ficticios', async () => {
    const elements = new Map();
    function element(id) {
        if (!elements.has(id)) elements.set(id, {style: {}, value: '', options: [{text: 'Prueba'}], selectedIndex: 0,
            reportValidity() { return true; }});
        return elements.get(id);
    }
    element('selectMedico').value = '42';
    element('inputFecha').value = '2026-10-28';
    let calls = 0;
    const context = {document: {getElementById: element},
        actualizarPillsStep2() { calls++; },
        async cargarHorariosDisponibles(id, fecha) { assert.equal(id, '42'); assert.equal(fecha, '2026-10-28'); calls++; },
        lucide: {createIcons() { this.rendered = true; }}};
    vm.createContext(context);
    vm.runInContext(functions('src/main/webapp/WEB-INF/views/paciente/reprogramar_cita.jsp', ['irAlPaso2']), context);
    await context.irAlPaso2(); assert.equal(calls, 2);
    element('selectMedico').value = '0'; await context.irAlPaso2(); assert.equal(calls, 2);
    assert.match(element('medicosEstado').textContent, /Selecciona/);
});

test('ambos scripts JSP siguen siendo sintácticamente válidos', () => {
    for (const file of ['reservar_cita.jsp', 'reprogramar_cita.jsp']) {
        const jsp = fs.readFileSync('src/main/webapp/WEB-INF/views/paciente/' + file, 'utf8');
        const scripts = [...jsp.matchAll(/<script>([\s\S]*?)<\/script>/g)];
        for (const script of scripts) {
            const code = script[1].replace(/<%[\s\S]*?%>/g, 'null').replace(/\$\{[^}]*\}/g, '');
            assert.doesNotThrow(() => new vm.Script(code));
        }
    }
});
