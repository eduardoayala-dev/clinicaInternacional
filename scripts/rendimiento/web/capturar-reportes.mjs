import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
const {default:puppeteer}=await import(pathToFileURL(path.resolve('target/auditoria-web/node_modules/puppeteer-core/lib/puppeteer/puppeteer-core.js')));
const fase=process.env.AUDIT_FASE || 'antes';
if(!['antes','despues'].includes(fase))throw Error('Fase inválida');
const base=path.resolve('target/auditoria-web/reportes',fase);
const salida=path.resolve('target/informe-web/capturas',fase);fs.mkdirSync(salida,{recursive:true});
const browser=await puppeteer.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
const registros=[];
try {
  const page=await browser.newPage();await page.setViewport({width:1366,height:1000,deviceScaleFactor:1});
  for(const carpeta of fs.readdirSync(base)){
    const archivo=path.join(base,carpeta,'resumen.json');if(!fs.existsSync(archivo))continue;
    const resumen=JSON.parse(fs.readFileSync(archivo,'utf8'));
    if(resumen.herramienta!=='Lighthouse local')continue;
    const validas=resumen.resultados.filter(r=>r.valida).sort((a,b)=>a.puntuaciones.performance-b.puntuaciones.performance);
    if(!validas.length)continue;
    const seleccion=validas[Math.floor(validas.length/2)];
    const html=path.join(base,carpeta,seleccion.repeticion+'.html');
    const auditoria=JSON.parse(fs.readFileSync(path.join(base,carpeta,seleccion.repeticion+'.json'),'utf8'));
    const imagen=auditoria.audits['final-screenshot']?.details?.data;
    if(imagen){
      const vistas=path.resolve('target/informe-web/vistas',fase);fs.mkdirSync(vistas,{recursive:true});
      fs.writeFileSync(path.join(vistas,carpeta+'.jpg'),Buffer.from(imagen.split(',')[1],'base64'));
    }
    await page.goto(pathToFileURL(html).href,{waitUntil:'networkidle0'});
    await page.waitForSelector('.lh-scores-wrapper');
    await page.screenshot({path:path.join(salida,carpeta+'.png')});
    registros.push({pagina:resumen.pagina,dispositivo:resumen.dispositivo,repeticion:seleccion.repeticion,
      serieCompleta:validas.length===3,fecha:seleccion.fecha,reporte:html,captura:path.join(salida,carpeta+'.png')});
    console.log('Captura: '+carpeta);
  }
} finally {await browser.close();}
fs.writeFileSync(path.join(salida,'indice.json'),JSON.stringify(registros,null,2));
