const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const source=fs.readFileSync('src/main/webapp/assets/js/sesion.js','utf8');
function setup(fetch,context='session-one',portalLogin='paciente') {
    const listeners={}; let submits=0; const navigation=[]; const fields=[{value:'old'}];
    const csrfMeta={content:'old'};const children=[{hidden:false}];
    const el=()=>({attrs:{},nodes:[],open:false,textContent:'',setAttribute(n,v){this.attrs[n]=v;},removeAttribute(n){delete this.attrs[n];},append(...n){this.nodes.push(...n);},addEventListener(){},showModal(){this.open=true;},close(){this.open=false;},querySelector(n){return this.nodes[n==='p'?0:1];}});
    class Form {
        constructor(){this.method='post';this.action='http://localhost/guardar';this.attrs={};}
        setAttribute(n,v){this.attrs[n]=v;} removeAttribute(n){delete this.attrs[n];}
        requestSubmit(button){const event=eventFor(this,button);listeners.submit(event);if(!event.prevented)submits++;}
    }
    function eventFor(form,button={disabled:false}){return {target:form,submitter:button,prevented:false,preventDefault(){this.prevented=true;},stopImmediatePropagation(){}};}
    const form=new Form();const button={disabled:false};
    const document={currentScript:{dataset:{stateUrl:'/auth/estado'}},hidden:false,
        querySelector(n){if(n.includes('login-portal'))return {content:portalLogin};if(n.includes('auth-context'))return {content:context};if(n.includes('csrf-token'))return csrfMeta;return null;},
        querySelectorAll(n){if(n.includes('csrfToken'))return fields;if(n.includes('body >'))return children;return [];},
        createElement:el,body:{append(){}},addEventListener(n,f){listeners[n]=f;}};
    vm.runInNewContext(source,{document,HTMLFormElement:Form,URL,AbortController,setTimeout,clearTimeout,WeakSet,
        fetch,localStorage:{setItem(){}},window:{location:{href:'http://localhost/form',origin:'http://localhost',assign(url){navigation.push(url);}},addEventListener(n,f){listeners[n]=f;}}});
    return {form,button,fields,csrfMeta,children,navigation,submits:()=>submits,submit:()=>listeners.submit(eventFor(form,button))};
}
const token='a'.repeat(43);
const ready=(sid='session-one',code='SESION_ACTIVA')=>({ok:true,json:async()=>({code,data:{sid,csrf:token,portal:'/medico/agenda'}})});
test('renueva el formulario de la misma sesión y envía una sola vez',async()=>{
    const ui=setup(async()=>ready());await ui.submit();assert.equal(ui.submits(),1);assert.equal(ui.fields[0].value,token);assert.equal(ui.csrfMeta.content,token);assert.equal(ui.button.disabled,false);
});
test('un cambio de identidad bloquea el formulario antiguo sin enviarlo como la cuenta nueva',async()=>{
    const ui=setup(async()=>ready('otra-sesion'));await ui.submit();assert.equal(ui.submits(),0);assert.equal(ui.fields[0].value,'old');assert.equal(ui.children[0].hidden,true);
});
test('una sesión cerrada vuelve al login y nunca repite el POST',async()=>{
    const ui=setup(async()=>({ok:true,json:async()=>({code:'SESION_ANONIMA',data:{sid:'anonymous',csrf:'',portal:'/login'}})}));await ui.submit();assert.equal(ui.submits(),0);assert.match(ui.navigation[0],/login\?msg=sesion_cerrada$/);
});
test('un error de red recupera el botón y conserva los datos sin enviar',async()=>{
    const ui=setup(async()=>{throw Error('offline');});await ui.submit();assert.equal(ui.submits(),0);assert.equal(ui.button.disabled,false);assert.equal(ui.fields[0].value,'old');
});
test('dos envíos simultáneos no duplican la operación',async()=>{
    let release;const ui=setup(()=>new Promise(r=>{release=r;}));const first=ui.submit();await ui.submit();assert.equal(ui.button.disabled,true);release(ready());await first;assert.equal(ui.submits(),1);
});
test('no acepta redirecciones a otro origen ni un CSRF inválido',async()=>{
    for(const data of [{sid:'otra',csrf:token,portal:'https://otro.test/'},{sid:'session-one',csrf:'wrong',portal:'/medico/agenda'}]){
        const ui=setup(async()=>({ok:true,json:async()=>({code:'SESION_ACTIVA',data})}));await ui.submit();assert.equal(ui.submits(),0);assert.equal(ui.navigation.length,0);
    }
});

test('una pestaña del personal vuelve a su login cuando termina la sesión',async()=>{
    const ui=setup(async()=>({ok:true,json:async()=>({code:'SESION_ANONIMA',data:{sid:'anonymous',csrf:'',portal:'/clinica/login'}})}),'session-one','personal');
    await ui.submit();assert.equal(ui.submits(),0);assert.equal(ui.navigation[0],'http://localhost/clinica/login/personal?msg=sesion_cerrada');
});
