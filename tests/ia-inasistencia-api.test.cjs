const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const fuente = fs.readFileSync('src/main/webapp/assets/js/ia-inasistencia-api.js', 'utf8');
function api(fetch) { const context = {fetch}; vm.runInNewContext(fuente, context); return context.ClinicaInasistencia; }
function prediccion() {
    return {version:'inasistencia-sintetico-v1', origen:'SINTETICO', experimental:true, probabilidad:0.4, nivel:'MEDIO',
        accionSugerida:'Revisar confirmación.', advertencias:['Datos sintéticos.'], factores:
            ['inasistencias_previas','atenciones_previas','anticipacion_dias','confirmada_por_paciente'].map(variable =>
                ({variable, descripcion:'Factor de prueba', valor:1, referencia:0.5, contribucionLogit:0.2, influencia:'AUMENTA'}))};
}
const respuesta = (data, status = 200, code = 'PREDICCION_EXPERIMENTAL_OBTENIDA') =>
    ({status, ok:status >= 200 && status < 300, json:async () => ({code, data})});

test('preserva sesión, CSRF en el formulario y cancelación sin caché', async () => {
    const signal = new AbortController().signal;
    const parametros = new URLSearchParams({csrfToken:'prueba'});
    const p = prediccion();
    const cliente = api(async (url, options) => {
        assert.equal(url, '/sysadmin/ia/inasistencia'); assert.equal(options.method, 'POST');
        assert.equal(options.credentials, 'same-origin'); assert.equal(options.cache, 'no-store');
        assert.equal(options.body, parametros); assert.equal(options.signal, signal);
        return respuesta(p);
    });
    assert.equal((await cliente.simular('/sysadmin/ia/inasistencia', parametros, signal)).probabilidad, 0.4);
});

test('no muestra números inválidos, una procedencia falsa ni contratos ajenos', async () => {
    for (const cambio of [{probabilidad:NaN}, {probabilidad:Infinity}, {probabilidad:1.1}, {probabilidad:-0.1},
        {probabilidad:'0.4'}, {experimental:false}, {origen:'REAL'}, {version:''}, {nivel:'URGENTE'}, {factores:[]}, {advertencias:[]}]) {
        await assert.rejects(api(async () => respuesta({...prediccion(), ...cambio})).simular('', ''), /verificar el resultado/);
    }
    for (const body of [respuesta(prediccion(), 201), respuesta(prediccion(), 200, 'OTRA_RESPUESTA'), respuesta(null)]) {
        await assert.rejects(api(async () => body).simular('', ''), /verificar el resultado/);
    }
});

test('rechaza factores repetidos, desconocidos, mal tipados o con signo incoherente', async () => {
    for (const cambio of [{variable:'no_existe'}, {variable:'atenciones_previas'}, {contribucionLogit:NaN},
        {referencia:'0.5'}, {valor:null}, {descripcion:''}, {influencia:'REDUCE'}]) {
        const p = prediccion(); Object.assign(p.factores[0], cambio);
        await assert.rejects(api(async () => respuesta(p)).simular('', ''), /verificar el resultado/);
    }
});

test('los fallos de sesión, red y HTML nunca se transforman en predicción', async () => {
    const denied = api(async () => ({status:403, ok:false, json:async () => ({message:'Sesión vencida.'})}));
    await assert.rejects(denied.simular('', ''), /Sesión vencida/);
    for (const fetch of [async () => {throw new Error('offline');}, async () => ({json:async () => {throw new Error('HTML');}})]) {
        await assert.rejects(api(fetch).simular('', ''), /No se pudo completar/);
    }
    const control = new AbortController(); control.abort();
    const fallo = new Error('cancelada');
    await assert.rejects(api(async () => {throw fallo;}).simular('', '', control.signal), e => e === fallo);
});
