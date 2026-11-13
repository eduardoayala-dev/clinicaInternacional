const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const source=fs.readFileSync('src/main/webapp/assets/js/dashboard-operativo.js','utf8');
function entorno() {
    const listeners={};const timers=new Map();let secuencia=0;
    const form={values:{desde:'2026-10-28',hasta:'2026-10-28',estado:''},contains:()=>false};
    const aviso={textContent:''};
    const panel={childNodes:['datos anteriores'],scrollTop:80,attributes:{},addEventListener:(name,fn)=>{listeners[name]=fn;},setAttribute(k,v){this.attributes[k]=v;},removeAttribute(k){delete this.attributes[k];},replaceChildren(...nodes){this.childNodes=nodes;}};
    const doc={hidden:false,activeElement:null,addEventListener:(name,fn)=>{listeners['document:'+name]=fn;},getElementById:id=>({panelOperativo:panel,filtrosOperativo:form,estadoActualizacionOperativa:aviso}[id])};
    const llamadas=[];let nuevo={childNodes:['datos nuevos']};
    const context={document:doc,window:{location:{href:'http://localhost/admin/dashboard-operativo?desde=2026-10-28&hasta=2026-10-28&estado=CONFIRMADA'},addEventListener:(name,fn)=>{listeners[name]=fn;}},URLSearchParams,AbortController,
        FormData:class {constructor(f){return Object.entries(f.values);}},DOMParser:class {parseFromString(){return {getElementById:()=>nuevo};}},
        fetch:async(url,options)=>{llamadas.push({url,options});return {ok:true,status:200,redirected:false,text:async()=>'<main></main>'};},
        queueMicrotask,setInterval:(fn,ms)=>{timers.set('interval',{fn,ms});return 1;},clearInterval:()=>timers.delete('interval'),
        setTimeout:(fn,ms)=>{const id=++secuencia;timers.set(id,{fn,ms});return id;},clearTimeout:id=>timers.delete(id)};
    vm.runInNewContext(source,context);
    return {context,doc,form,panel,aviso,llamadas,listeners,timers,automatico:()=>timers.get('interval').fn(),sinPanel:()=>{nuevo=null;}};
}
test('actualiza cada cinco minutos con filtros, sesión y marcador de consulta automática',async()=>{
    const e=entorno();assert.equal(e.timers.get('interval').ms,300000);await e.automatico();
    assert.equal(e.llamadas.length,1);assert.match(e.llamadas[0].url,/estado=CONFIRMADA/);assert.equal(e.llamadas[0].options.credentials,'same-origin');assert.equal(e.llamadas[0].options.headers['X-Actualizacion-Automatica'],'true');
    assert.equal(e.panel.childNodes[0],'datos nuevos');assert.equal(e.panel.scrollTop,80);assert.match(e.aviso.textContent,/Datos actualizados/);
});
test('un fallo conserva los datos y muestra un aviso',async()=>{
    const e=entorno();e.context.fetch=async()=>({ok:false,status:503});await e.automatico();assert.equal(e.panel.childNodes[0],'datos anteriores');assert.match(e.aviso.textContent,/conservan/);assert.equal(e.panel.attributes['aria-busy'],undefined);
});
test('no consulta en segundo plano ni sustituye filtros sin aplicar',async()=>{
    const e=entorno();e.doc.hidden=true;await e.automatico();assert.equal(e.llamadas.length,0);e.doc.hidden=false;e.form.values.estado='ATENDIDA';await e.automatico();assert.equal(e.llamadas.length,0);assert.match(e.aviso.textContent,/sin consultar/);
});
test('una respuesta de login no sustituye el tablero',async()=>{
    const e=entorno();e.context.fetch=async()=>({ok:true,status:200,redirected:true});await e.automatico();assert.equal(e.panel.childNodes[0],'datos anteriores');assert.match(e.aviso.textContent,/sesión/);
    const otro=entorno();otro.sinPanel();await otro.automatico();assert.equal(otro.panel.childNodes[0],'datos anteriores');assert.match(otro.aviso.textContent,/verificar/);
});
test('actualización manual no se marca como actividad automática',async()=>{
    const e=entorno();e.listeners.click({target:{closest:()=>true}});await new Promise(resolve=>setImmediate(resolve));assert.equal(e.llamadas[0].options.headers['X-Actualizacion-Automatica'],undefined);
});
test('al salir se detiene la actualización periódica',()=>{const e=entorno();e.listeners.pagehide();assert.equal(e.timers.has('interval'),false);});

test('si editas filtros mientras llega la respuesta, conserva tu edición y la tabla anterior',async()=>{
    const e=entorno();let resolver;e.context.fetch=()=>new Promise(resolve=>{resolver=resolve;});
    const pendiente=e.automatico();e.form.values.estado='CANCELADA';resolver({ok:true,status:200,redirected:false,text:async()=>'<main></main>'});await pendiente;
    assert.equal(e.form.values.estado,'CANCELADA');assert.equal(e.panel.childNodes[0],'datos anteriores');assert.match(e.aviso.textContent,/pospuesta/);
});
test('no inicia dos consultas automáticas superpuestas',async()=>{
    const e=entorno();let resolver;let llamadas=0;e.context.fetch=()=>{llamadas++;return new Promise(resolve=>{resolver=resolve;});};
    const pendiente=e.automatico();await e.automatico();assert.equal(llamadas,1);resolver({ok:true,status:200,redirected:false,text:async()=>'<main></main>'});await pendiente;
});

test('la vista carga el módulo que inicializa el refresco del tablero',()=>{
    const vista=fs.readFileSync('src/main/webapp/WEB-INF/views/admin/dashboard_operativo.jsp','utf8');
    const scripts=[...vista.matchAll(/<script\s+src="([^"]+)"/g)].map(m=>m[1]);
    assert.equal(scripts.filter(src=>src.endsWith('/assets/js/dashboard-operativo.js')).length,1);
});
