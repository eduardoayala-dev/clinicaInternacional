const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const coleccion = JSON.parse(fs.readFileSync('docs/postman/Rendimiento-Medico-Local.postman_collection.json', 'utf8'));
const guardia = coleccion.event.find(event => event.listen === 'prerequest').script.exec.join('\n');

function validar(baseUrl) {
    const datos = {baseUrl, correo: 'carga.medico.001@example.test', clave: 'Prueba', fecha: '2026-10-27', idMedico: 10000, idPaciente: 100000};
    vm.runInNewContext(guardia, {pm: {
        execution: {skipRequest() {}},
        variables: {get: clave => datos[clave], replaceIn: url => url.replace('{{baseUrl}}', baseUrl)},
        request: {url: {toString: () => '{{baseUrl}}/medico/agenda'}}
    }});
}

test('la carga admite el laboratorio y rechaza producción o la aplicación habitual antes de enviar peticiones', () => {
    assert.doesNotThrow(() => validar('http://127.0.0.1:8083'));
    assert.throws(() => validar('http://localhost:8080'), /laboratorio local/);
    assert.throws(() => validar('https://clinica.example.test'), /laboratorio local/);
});
