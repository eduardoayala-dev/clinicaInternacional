const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const jsp = fs.readFileSync(path.join(__dirname, '../src/main/webapp/WEB-INF/views/paciente/reservar_cita.jsp'), 'utf8');
const script = jsp.slice(jsp.indexOf('let sugerenciaOrientacion = null;'), jsp.indexOf('function mostrarPaso('))
    + jsp.slice(jsp.indexOf('function iniciarModoDirecto('), jsp.indexOf('function volverASeleccionModos('))
    + jsp.slice(jsp.indexOf('function actualizarEspecialidadYMotivos('), jsp.indexOf('let medicosRequest ='))
    + jsp.slice(jsp.indexOf('function poblarMotivosPaso3('), jsp.indexOf('let countdownInterval ='));
const motivos = require('../src/main/webapp/assets/js/motivos-cita.js');
globalThis.Option = class {constructor(texto, valor) {this.textContent = texto;this.value = valor;}};

function escenario(respuesta) {
    const campos = new Map();
    const temporizadores = new Map();
    const duraciones = [];
    const solicitudes = [];
    function campo(id) {
        if (!campos.has(id)) campos.set(id, {hidden: false, disabled: false, textContent: '', value: '', checked: false, style: {}, attributes: {}, options: [],
            replaceChildren() {this.options = [];this.value = '';}, appendChild(opcion) {this.options.push(opcion);},
            setAttribute(nombre, valor) {this.attributes[nombre] = valor;}, querySelector() {return campo(id + '-etiqueta');}});
        return campos.get(id);
    }
    campo('aiCasoFicticio').checked = true;
    campo('aiSintomasInput').value = 'Caso ficticio: molestias generales desde hace unos días.';
    campo('selectEspecialidad').options = [{value: '4', textContent: 'Medicina Interna'}, {value: '3', textContent: 'Dermatología'}];
    campo('aiElegirManual').hidden = true;
    const estado = {modo: 'NINGUNO', motivo: '', especialidadId: ''};
    const contexto = vm.createContext({AbortController, URLSearchParams, state: estado, ClinicaMotivos: motivos,
        catalogoMotivos: [{idMotivo: 56, idEspecialidad: 4, descripcion: 'Consulta general'}, {idMotivo: 57, idEspecialidad: 4, descripcion: 'Control'}, {idMotivo: 80, idEspecialidad: 3, descripcion: 'Piel'}],
        Option: function(texto, valor) {this.textContent = texto;this.value = valor;}, mostrarPaso() {}, actualizarMedicosPorEspecialidad() {}, lucide: {createIcons() {}},
        document: {getElementById: campo, querySelector: () => ({content: 'csrf-de-prueba'})},
        setTimeout(fn, ms) {const id = Symbol();temporizadores.set(id, fn);duraciones.push(ms);return id;},
        clearTimeout(id) {temporizadores.delete(id);},
        async fetch(url, opciones) {solicitudes.push(opciones);return {ok: respuesta.ok, async json() {return respuesta.body;}};}});
    vm.runInContext(script, contexto);
    return {campo, contexto, estado, temporizadores, duraciones, solicitudes, ejecutar: () => vm.runInContext('ejecutarAnalisisIA()', contexto)};
}

test('un fallo muestra una salida manual y permite reintentar sin dejar el botón bloqueado', async () => {
    const x = escenario({ok: false, body: {code: 'ORIENTACION_OCUPADA', message: 'Gemini está temporalmente ocupado.'}});
    await x.ejecutar();
    assert.equal(x.campo('aiErrorOrientacion').hidden, false);
    assert.equal(x.campo('aiElegirManual').hidden, false);
    assert.equal(x.campo('btnAnalizarIA').disabled, false);
    assert.equal(x.campo('btnAnalizarIA').attributes['aria-busy'], 'false');
    assert.equal(x.campo('btnAnalizarIA-etiqueta').textContent, 'Reintentar análisis');
    assert.equal(x.solicitudes.length, 1);
    assert.equal(x.temporizadores.size, 0);
    assert.deepEqual(x.duraciones, [25000]);
});
test('una sugerencia válida conserva sesión y CSRF y presenta únicamente una especialidad del catálogo', async () => {
    const x = escenario({ok: true, body: {data: {tipo: 'SUGERENCIA', proveedor: 'GEMINI', idEspecialidad: 4, especialidad: 'Medicina Interna', explicacion: 'Orientación por especialidad.'}}});
    await x.ejecutar();
    assert.equal(x.campo('aiFormulario').hidden, true);
    assert.equal(x.campo('aiResultadoBox').style.display, 'block');
    assert.equal(x.campo('aiEspecialidadNombre').textContent, 'Medicina Interna');
    assert.equal(x.campo('aiElegirManual').hidden, true);
    assert.equal(x.solicitudes[0].credentials, 'same-origin');
    assert.equal(x.solicitudes[0].body.get('csrfToken'), 'csrf-de-prueba');
    assert.equal(x.temporizadores.size, 0);
});
test('editar el caso tras un fallo elimina el error anterior y restaura el botón', async () => {
    const x = escenario({ok: false, body: {message: 'Demora del proveedor.'}});
    await x.ejecutar();
    x.campo('aiSintomasInput').value = 'Otro caso ficticio con una descripción diferente.';
    vm.runInContext('invalidarOrientacion()', x.contexto);
    assert.equal(x.campo('aiErrorOrientacion').hidden, true);
    assert.equal(x.campo('aiElegirManual').hidden, true);
    assert.equal(x.campo('btnAnalizarIA-etiqueta').textContent, 'Analizar síntomas');
});
test('una solicitud de aclaración no fabrica una sugerencia', async () => {
    const x = escenario({ok: true, body: {data: {tipo: 'ACLARAR', explicacion: 'Describe mejor las molestias.'}}});
    await x.ejecutar();
    assert.equal(x.campo('aiFormulario').hidden, false);
    assert.equal(x.campo('aiResultadoBox').style.display, 'none');
    assert.equal(x.campo('aiElegirManual').hidden, false);
});
test('continuar aplica el motivo compatible y permite cambiarlo sin conservarlo en otra especialidad', async () => {
    const x = escenario({ok: true, body: {data: {tipo: 'SUGERENCIA', proveedor: 'GEMINI', idEspecialidad: 4, especialidad: 'Medicina Interna', idMotivo: 56, motivoConsulta: 'Consulta general', explicacion: 'Orientación.'}}});
    await x.ejecutar();
    vm.runInContext('aplicarRecomendacionIA()', x.contexto);
    assert.equal(x.estado.modo, 'IA');assert.equal(x.estado.motivo, '56');assert.equal(x.campo('selectMotivo').value, '56');
    assert.match(x.campo('motivoOrientacionAyuda').textContent, /Sugerido/);
    x.campo('selectMotivo').value = '57';vm.runInContext('actualizarMotivoElegido()', x.contexto);assert.equal(x.estado.motivo, '57');
    vm.runInContext('poblarMotivosPaso3()', x.contexto);assert.equal(x.campo('selectMotivoPaso3').value, '57');
    x.campo('selectEspecialidad').value = '3';vm.runInContext('actualizarEspecialidadYMotivos()', x.contexto);
    assert.equal(x.estado.modo, 'DIRECTO');assert.equal(x.estado.motivo, '');assert.equal(x.campo('aiSugerenciaBuscador').hidden, true);
});
test('una sugerencia sin motivo no elige automáticamente la primera opción', async () => {
    const x = escenario({ok: true, body: {data: {tipo: 'SUGERENCIA', proveedor: 'GEMINI', idEspecialidad: 4, especialidad: 'Medicina Interna', explicacion: 'Orientación.'}}});
    await x.ejecutar();vm.runInContext('aplicarRecomendacionIA()', x.contexto);
    assert.equal(x.estado.motivo, '');assert.equal(x.campo('selectMotivo').value, '');
});
