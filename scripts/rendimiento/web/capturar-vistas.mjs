import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
const {default:puppeteer}=await import(pathToFileURL(path.resolve('target/auditoria-web/node_modules/puppeteer-core/lib/puppeteer/puppeteer-core.js')));
process.loadEnvFile('.env.auditoria');
const paginas=JSON.parse(fs.readFileSync('scripts/rendimiento/web/paginas.json','utf8'));
const base='https://clinicainternacional-production.up.railway.app';
const salida='target/informe-web/vistas/despues-alta-resolucion';fs.mkdirSync(salida,{recursive:true});
const registro=[];
for(const rol of ['publico','admision','administrador','paciente','medico']) {
  const browser=await puppeteer.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
  try {
    const page=await browser.newPage();await page.setViewport({width:1440,height:1000,deviceScaleFactor:1});
    if(rol!=='publico'){
      const personal=['admision','administrador'].includes(rol);
      const usuario=personal?process.env['CAPTURA_'+rol.toUpperCase()+'_USUARIO']:process.env['AUDIT_'+rol.toUpperCase()+'_USUARIO'];
      const clave=personal?process.env.CAPTURA_PERSONAL_PASSWORD:process.env['AUDIT_'+rol.toUpperCase()+'_PASSWORD'];
      if(!usuario||!clave)throw Error('Faltan accesos privados para '+rol);
      await page.goto(base+(rol==='paciente'?'/login/paciente':'/login/personal'),{waitUntil:'networkidle2'});
      await page.type('#identificador',usuario);await page.type('#password',clave);
      await Promise.all([page.waitForNavigation({waitUntil:'domcontentloaded'}),page.click('form:has(#identificador) button[type="submit"]')]);
      if(new URL(page.url()).pathname.startsWith('/login'))throw Error('No se confirmó el acceso de '+rol);
    }
    for(const p of paginas.filter(p=>p.rol===rol)){
      const r=await page.goto(base+p.ruta,{waitUntil:'networkidle2',timeout:60000});
      if(r.status()!==200||new URL(page.url()).pathname!==p.ruta){registro.push({pagina:p.id,valida:false,http:r.status()});continue;}
      await page.screenshot({path:path.join(salida,p.id+'.png'),fullPage:true});
      registro.push({pagina:p.id,valida:true,url:page.url(),fecha:new Date().toISOString(),commit:'042fd0cde850af4940bc4f66fa0ea8dc477ed7bf'});
      console.log('Vista en línea: '+p.id);
    }
  } finally {await browser.close();}
}
fs.writeFileSync(path.join(salida,'indice.json'),JSON.stringify(registro,null,2));
