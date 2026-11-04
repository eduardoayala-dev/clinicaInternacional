import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const modulos = path.join(raiz, 'target/auditoria-web/node_modules');
const {default: lighthouse} = await import(pathToFileURL(path.join(modulos, 'lighthouse/core/index.js')));
const {default: escritorio} = await import(pathToFileURL(path.join(modulos, 'lighthouse/core/config/desktop-config.js')));
const {default: puppeteer} = await import(pathToFileURL(path.join(modulos, 'puppeteer-core/lib/puppeteer/puppeteer-core.js')));
const paginas = JSON.parse(fs.readFileSync(new URL('./paginas.json', import.meta.url), 'utf8'));

async function main() {
    const id = process.env.AUDIT_PAGINA;
    const pagina = paginas.find(p => p.id === id);
    const dispositivo = process.env.AUDIT_DISPOSITIVO || 'desktop';
    const fase = process.env.AUDIT_FASE || 'antes';
    const commit = process.env.AUDIT_COMMIT_DESPLEGADO;
    const repeticiones = Number(process.env.AUDIT_REPETICIONES || 3);
    if (!pagina || !['desktop', 'mobile'].includes(dispositivo) || !['antes', 'despues'].includes(fase)
        || !/^[a-f0-9]{40}$/.test(commit || '') || !Number.isInteger(repeticiones) || repeticiones < 1 || repeticiones > 3) {
        throw Error('Define página válida, fase, dispositivo, commit desplegado y entre 1 y 3 repeticiones.');
    }
    const base = new URL(process.env.AUDIT_URL || 'https://clinicainternacional-production.up.railway.app');
    const privada = pagina.rol !== 'publico';
    if (privada && (!process.env.AUDIT_USUARIO || !process.env.AUDIT_PASSWORD)) throw Error('Faltan credenciales de prueba en el entorno.');
    const chrome = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
    const salida = path.join(raiz, 'target/auditoria-web/reportes', fase, `${id}-${dispositivo}-${Date.now()}`);
    fs.mkdirSync(salida, {recursive: true});
    const perfil = fs.mkdtempSync(path.join(raiz, 'target/auditoria-web/perfil-'));
    const browser = await puppeteer.launch({executablePath: chrome, headless: true, userDataDir: perfil,
        args: ['--remote-debugging-port=0', '--disable-extensions']});
    try {
        const page = await browser.newPage();
        if (privada) {
            const login = pagina.rol === 'paciente' ? '/login/paciente' : '/login/personal';
            await page.goto(new URL(login, base).href, {waitUntil: 'networkidle2', timeout: 60000});
            await page.type('input[name="identificador"]', process.env.AUDIT_USUARIO);
            await page.type('input[name="password"]', process.env.AUDIT_PASSWORD);
            await Promise.all([
                page.waitForNavigation({waitUntil: 'domcontentloaded', timeout: 60000}),
                page.click('form:has(input[name="identificador"]) button[type="submit"]')
            ]);
            if (new URL(page.url()).pathname.startsWith('/login')) throw Error('La cuenta no pudo iniciar sesión.');
        }
        const url = new URL(pagina.ruta, base).href;
        const resultados = [];
        for (let n = 1; n <= repeticiones; n++) {
            const opciones = {port: Number(new URL(browser.wsEndpoint()).port), output: ['html', 'json'],
                logLevel: 'error', disableStorageReset: privada, onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo']};
            const resultado = await lighthouse(url, opciones, dispositivo === 'desktop' ? escritorio : undefined);
            const lhr = resultado.lhr;
            const destino = new URL(lhr.finalDisplayedUrl || lhr.finalUrl);
            const valida = !lhr.runtimeError && destino.origin === base.origin && destino.pathname === pagina.ruta
                && lhr.audits['http-status-code']?.score !== 0;
            fs.writeFileSync(path.join(salida, `${n}.html`), resultado.report[0]);
            fs.writeFileSync(path.join(salida, `${n}.json`), resultado.report[1]);
            const metricas = Object.fromEntries(['first-contentful-paint', 'largest-contentful-paint',
                'total-blocking-time', 'cumulative-layout-shift', 'speed-index'].map(k => [k, lhr.audits[k]?.numericValue ?? null]));
            const puntuaciones = Object.fromEntries(Object.entries(lhr.categories).map(([k,v]) => [k, v.score === null ? null : Math.round(v.score*100)]));
            resultados.push({repeticion: n, valida, fecha: lhr.fetchTime, urlFinal: destino.href,
                versionLighthouse: lhr.lighthouseVersion, userAgent: lhr.userAgent, puntuaciones, metricas,
                advertencias: lhr.runWarnings, error: lhr.runtimeError?.code || null});
            console.log(JSON.stringify({pagina: id, dispositivo, repeticion: n, valida, puntuaciones}));
            if (!valida) break;
        }
        fs.writeFileSync(path.join(salida, 'resumen.json'), JSON.stringify({herramienta: 'Lighthouse local',
            commitDeclarado: commit, fase, pagina: id, ruta: pagina.ruta, dispositivo,
            almacenamientoConservado: privada, resultados}, null, 2));
        if (resultados.some(r => !r.valida)) throw Error('La auditoría no midió correctamente la vista solicitada; no usar sus puntuaciones.');
        console.log('Reportes guardados en ' + salida);
    } finally {
        await browser.close();
        if (!perfil.startsWith(path.resolve(raiz, 'target/auditoria-web') + path.sep)) throw Error('Perfil fuera de la carpeta de auditoría.');
        fs.rmSync(perfil, {recursive: true, force: true});
    }
}
main().catch(e => {console.error(e.message); process.exitCode=1;});
