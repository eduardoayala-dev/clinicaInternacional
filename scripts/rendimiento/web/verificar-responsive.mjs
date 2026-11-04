import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
const raiz=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../../..');
const {default:puppeteer}=await import(pathToFileURL(path.join(raiz,'target/auditoria-web/node_modules/puppeteer-core/lib/puppeteer/puppeteer-core.js')));
const base=new URL(process.env.QA_URL || 'http://127.0.0.1:8084');
if(!['127.0.0.1','localhost'].includes(base.hostname))throw Error('Esta comprobación se limita al ensayo local.');
if(!process.env.QA_PACIENTE_PASSWORD)throw Error('Define QA_PACIENTE_PASSWORD para la cuenta sintética de ensayo.');
const salida=path.join(raiz,'target/qa-responsive');fs.mkdirSync(salida,{recursive:true});
const browser=await puppeteer.launch({executablePath:process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
const resultados=[];
try {
    const page=await browser.newPage();
    page.on('pageerror',error=>console.error('Error en vista: '+error.message));
    await page.goto(new URL('/login/paciente',base).href,{waitUntil:'domcontentloaded',timeout:60000});
    await page.type('#identificador',process.env.QA_PACIENTE_DNI || '99000001');
    await page.type('#password',process.env.QA_PACIENTE_PASSWORD);
    await Promise.all([page.waitForNavigation({waitUntil:'domcontentloaded',timeout:60000}),page.click('form:has(#identificador) button[type="submit"]')]);
    assert.equal(new URL(page.url()).pathname,'/paciente/dashboard','Debe iniciar sesión en la copia de ensayo.');
    for(const ancho of [320,390,768,1280]) {
        await page.setViewport({width:ancho,height:844,deviceScaleFactor:1});
        for(const ruta of ['dashboard','citas','historial','lista-espera','notificaciones','perfil']) {
            const respuesta=await page.goto(new URL('/paciente/'+ruta,base).href,{waitUntil:'networkidle2',timeout:60000});
            assert.equal(respuesta.status(),200);
            await page.waitForSelector('#patientNavigation');
            const dimensiones=await page.evaluate(()=>({ancho:innerWidth,documento:document.documentElement.scrollWidth,
                main:document.querySelector('main').getBoundingClientRect().right,
                campos:[...document.querySelectorAll('main input,main select,main textarea')].filter(e=>e.getClientRects().length&&e.type!=='hidden').map(e=>({id:e.id,derecha:e.getBoundingClientRect().right}))}));
            assert.ok(dimensiones.documento<=ancho+1,`${ruta} desborda a ${ancho}px: ${JSON.stringify(dimensiones)}`);
            assert.ok(dimensiones.main<=ancho+1);
            assert.ok(dimensiones.campos.every(c=>c.derecha<=ancho+1),`Hay campos fuera de pantalla en ${ruta}.`);
            if(ruta==='citas') {
                await page.click('button[onclick="iniciarModoDirecto()"]');
                assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'El buscador desborda.');
                await page.screenshot({path:path.join(salida,`buscador-${ancho}.png`),fullPage:true});
                await page.select('#selectSede','1');
                await page.select('#selectEspecialidad','1');
                await page.waitForFunction(()=>[...document.querySelector('#selectMotivo').options].some(o=>o.value&&!o.disabled));
                const motivo=await page.$eval('#selectMotivo',e=>[...e.options].find(o=>o.value&&!o.disabled).value);
                await page.select('#selectMotivo',motivo);
                await page.click('.btn-buscar-disponibilidad');
                await page.waitForSelector('#weekDaysGrid',{visible:true});
                await page.waitForFunction(()=>![...document.querySelectorAll('#weekDaysGrid .day-cupos')].some(e=>e.textContent.includes('Consultando')));
                assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'El calendario desborda.');
                await page.screenshot({path:path.join(salida,`calendario-${ancho}.png`),fullPage:true});
            }
            if(ancho<=900) {
                await page.click('.patient-menu-toggle');
                assert.equal(await page.$eval('.patient-menu-toggle',e=>e.getAttribute('aria-expanded')),'true');
                assert.equal(await page.$eval('main',e=>e.inert),true);
                await page.waitForFunction(()=>document.activeElement.classList.contains('patient-menu-close'),{timeout:3000});
                await page.screenshot({path:path.join(salida,`menu-${ancho}.png`)});
                await page.keyboard.down('Shift');
                await page.keyboard.press('Tab');
                await page.keyboard.up('Shift');
                assert.equal(await page.evaluate(()=>document.activeElement.classList.contains('btn-logout')),true,
                    await page.evaluate(()=>document.activeElement.outerHTML.slice(0,250)));
                await page.keyboard.press('Escape');
                assert.equal(await page.$eval('main',e=>e.inert),false);
                assert.equal(await page.evaluate(()=>document.activeElement.classList.contains('patient-menu-toggle')),true);
                await page.waitForFunction(()=>getComputedStyle(document.getElementById('patientNavigation')).visibility==='hidden');
            } else {
                assert.equal(await page.$eval('#patientNavigation',e=>e.inert),false);
            }
            await page.screenshot({path:path.join(salida,`${ruta}-${ancho}.png`),fullPage:true});
            resultados.push({ruta,ancho,http:200,desbordamiento:false});
            console.log(JSON.stringify(resultados.at(-1)));
        }
        await page.goto(new URL('/paciente/historial',base).href,{waitUntil:'networkidle2'});
        const enlace=await page.$eval('a[href*="/paciente/reprogramar?idCita="]',e=>e.href);
        const reprogramacion=await page.goto(enlace,{waitUntil:'networkidle2'});
        assert.equal(reprogramacion.status(),200);
        assert.equal(new URL(page.url()).pathname,'/paciente/reprogramar');
        assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Reprogramación desborda.');
        await page.screenshot({path:path.join(salida,`reprogramar-${ancho}.png`),fullPage:true});
        resultados.push({ruta:'reprogramar',ancho,http:200,desbordamiento:false});
    }
} finally {
    fs.writeFileSync(path.join(salida,'resultados.json'),JSON.stringify({fecha:new Date().toISOString(),resultados},null,2));
    await browser.close();
}
