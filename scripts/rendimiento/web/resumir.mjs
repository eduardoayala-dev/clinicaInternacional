import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve('target/auditoria-web/reportes');
const filas=[];
const mediana=valores=>{
    const validos=valores.filter(v=>typeof v==='number'&&Number.isFinite(v)).sort((a,b)=>a-b);
    if(validos.length!==3)return null;
    return validos[1];
};
for(const fase of ['antes','despues']){
    const carpeta=path.join(root,fase);if(!fs.existsSync(carpeta))continue;
    for(const entrada of fs.readdirSync(carpeta)){
        const archivo=path.join(carpeta,entrada,'resumen.json');if(!fs.existsSync(archivo))continue;
        const resumen=JSON.parse(fs.readFileSync(archivo,'utf8'));
        const r=resumen.resultados.filter(x=>x.valida);
        const fila={herramienta:resumen.herramienta,fase,pagina:resumen.pagina,dispositivo:resumen.dispositivo,
            commit:resumen.commitDeclarado,validas:r.length,completa:r.length===3&&resumen.resultados.length===3,
            fecha:r[0]?.fecha || '',reporte:path.relative(process.cwd(),archivo)};
        for(const categoria of ['performance','accessibility','best-practices','seo'])fila[categoria]=mediana(r.map(x=>x.puntuaciones?.[categoria]));
        for(const metrica of ['first-contentful-paint','largest-contentful-paint','total-blocking-time','cumulative-layout-shift','speed-index'])fila[metrica]=mediana(r.map(x=>x.metricas?.[metrica]));
        filas.push(fila);
    }
}
fs.mkdirSync('target/informe-web',{recursive:true});
fs.writeFileSync('target/informe-web/metricas.json',JSON.stringify(filas,null,2));
const columnas=['herramienta','fase','pagina','dispositivo','commit','validas','completa','performance','accessibility','best-practices','seo','first-contentful-paint','largest-contentful-paint','total-blocking-time','cumulative-layout-shift','speed-index','fecha','reporte'];
const celda=v=>'"'+String(v??'').replaceAll('"','""')+'"';
fs.writeFileSync('target/informe-web/metricas.csv','\uFEFF'+[columnas.join(';'),...filas.map(f=>columnas.map(c=>celda(f[c])).join(';'))].join('\n'));
console.log(JSON.stringify(filas.map(f=>({fase:f.fase,pagina:f.pagina,dispositivo:f.dispositivo,validas:f.validas,rendimiento:f.performance,accesibilidad:f.accessibility,LCP:f['largest-contentful-paint'],CLS:f['cumulative-layout-shift']})),null,2));
