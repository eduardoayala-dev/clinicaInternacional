'use strict';
const fs = require('node:fs');
const {performance} = require('node:perf_hooks');

async function main() {
    const base = process.env.DIAGNOSTICO_URL || 'http://localhost:8080';
    const archivo = process.env.DIAGNOSTICO_SALIDA;
    if (!archivo || !process.env.DIAGNOSTICO_PASSWORD || !process.env.DIAGNOSTICO_USUARIO) {
        throw Error('Define DIAGNOSTICO_USUARIO, DIAGNOSTICO_PASSWORD y DIAGNOSTICO_SALIDA.');
    }
    const cookies = new Map();
    const mediciones = [];
    async function pedir(ruta, opciones = {}) {
        const inicio = performance.now();
        const respuesta = await fetch(new URL(ruta, base), {
            ...opciones, redirect: 'manual', signal: AbortSignal.timeout(30000),
            headers: {...opciones.headers, Cookie: [...cookies].map(([k,v]) => k+'='+v).join('; ')}
        });
        for (const cookie of respuesta.headers.getSetCookie()) {
            const par = cookie.split(';')[0], indice = par.indexOf('=');
            cookies.set(par.slice(0, indice), par.slice(indice+1));
        }
        const html = await respuesta.text();
        return {respuesta, html, ms: Math.round(performance.now()-inicio)};
    }
    const formulario = await pedir('/login/personal');
    const csrf = formulario.html.match(/name="csrfToken" value="([^"]+)"/)?.[1];
    if (!csrf) throw Error('No se encontró el formulario de acceso.');
    const login = await pedir('/login/personal', {
        method: 'POST', headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({identificador: process.env.DIAGNOSTICO_USUARIO,
            password: process.env.DIAGNOSTICO_PASSWORD, csrfToken: csrf})
    });
    if (login.respuesta.status !== 302 || !login.respuesta.headers.get('location')?.includes('/admin/dashboard')) {
        throw Error('Acceso no confirmado; no se repetirá el intento.');
    }
    mediciones.push({ruta: 'login-admision', http: login.respuesta.status, ms: login.ms});
    for (const ruta of ['/admin/dashboard', '/admin/medicos', '/admin/pacientes',
        '/admin/seguimiento-confirmaciones?pagina=1', '/admin/reasignaciones', '/admin/dashboard-operativo']) {
        const lectura = await pedir(ruta);
        mediciones.push({ruta, http: lectura.respuesta.status, ms: lectura.ms});
        if (lectura.ms > 20000) break;
    }
    const resultado = {fecha: new Date().toISOString(),
        tipo: 'Diagnóstico secuencial autenticado, no prueba de carga', mediciones};
    fs.writeFileSync(archivo, JSON.stringify(resultado, null, 2));
    console.log(JSON.stringify(resultado, null, 2));
}
main().catch(() => {console.error('No se completó el diagnóstico. Revisa acceso, disponibilidad y ruta de salida.'); process.exitCode=1;});
