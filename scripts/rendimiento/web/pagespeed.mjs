import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const paginas = JSON.parse(fs.readFileSync(new URL('./paginas.json', import.meta.url), 'utf8'));
async function main() {
    const pagina = paginas.find(p => p.id === process.env.AUDIT_PAGINA && p.rol === 'publico');
    const dispositivo = process.env.AUDIT_DISPOSITIVO || 'mobile';
    const fase = process.env.AUDIT_FASE || 'antes';
    const commit = process.env.AUDIT_COMMIT_DESPLEGADO;
    const repeticiones = Number(process.env.AUDIT_REPETICIONES || 3);
    if (!pagina || !['mobile', 'desktop'].includes(dispositivo) || !['antes', 'despues'].includes(fase)
        || !/^[a-f0-9]{40}$/.test(commit || '') || !Number.isInteger(repeticiones) || repeticiones<1 || repeticiones>3) {
        throw Error('Revisa página pública, dispositivo, fase, commit y repeticiones (1–3).');
    }
    const base = new URL(process.env.AUDIT_URL || 'https://clinicainternacional-production.up.railway.app');
    if (base.protocol !== 'https:' || base.username || base.password) throw Error('PageSpeed necesita una URL pública HTTPS sin credenciales.');
    const salida = path.join(raiz, 'target/auditoria-web/reportes', fase, `pagespeed-${pagina.id}-${dispositivo}-${Date.now()}`);
    fs.mkdirSync(salida, {recursive:true});
    const resultados=[];
    for(let n=1;n<=repeticiones;n++) {
        const url=new URL('https://www.googleapis.com/pagespeedonline/v5/runPagespeed');
        url.searchParams.set('url',new URL(pagina.ruta,base).href);
        url.searchParams.set('strategy',dispositivo);
        for(const categoria of ['performance','accessibility','best-practices','seo'])url.searchParams.append('category',categoria);
        if(process.env.PAGESPEED_API_KEY)url.searchParams.set('key',process.env.PAGESPEED_API_KEY);
        const respuesta=await fetch(url,{signal:AbortSignal.timeout(120000)});
        const datos=await respuesta.json();
        fs.writeFileSync(path.join(salida,`${n}.json`),JSON.stringify(datos,null,2));
        if(!respuesta.ok) {
            resultados.push({repeticion:n,valida:false,http:respuesta.status,error:datos.error?.status || 'ERROR_API'});
            console.log(`PageSpeed respondió HTTP ${respuesta.status}; no se asigna puntuación. No se repetirá automáticamente.`);
            break;
        }
        const lhr=datos.lighthouseResult;
        const destino=new URL(lhr.finalDisplayedUrl || lhr.finalUrl);
        const valida=!lhr.runtimeError && destino.origin===base.origin && destino.pathname===pagina.ruta
            && lhr.audits['http-status-code']?.score!==0;
        const puntuaciones=Object.fromEntries(Object.entries(lhr.categories).map(([k,v])=>[k,v.score===null?null:Math.round(v.score*100)]));
        const metricas=Object.fromEntries(['first-contentful-paint','largest-contentful-paint','total-blocking-time',
            'cumulative-layout-shift','speed-index'].map(k=>[k,lhr.audits[k]?.numericValue ?? null]));
        resultados.push({repeticion:n,valida,fecha:lhr.fetchTime,versionLighthouse:lhr.lighthouseVersion,puntuaciones,metricas,
            datosCampo:datos.loadingExperience?.overall_category || 'SIN_DATOS_SUFICIENTES',advertencias:lhr.runWarnings});
        console.log(JSON.stringify({pagina:pagina.id,dispositivo,repeticion:n,valida,puntuaciones}));
        if(!valida)break;
    }
    fs.writeFileSync(path.join(salida,'resumen.json'),JSON.stringify({herramienta:'PageSpeed Insights',commitDeclarado:commit,
        fase,pagina:pagina.id,ruta:pagina.ruta,dispositivo,resultados},null,2));
    if(resultados.some(r=>!r.valida))process.exitCode=1;
}
main().catch(()=>{console.error('No se completó PageSpeed. Revisa configuración, conexión o cuota. No hay puntuación válida.');process.exitCode=1;});
