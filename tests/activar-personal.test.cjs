const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const codigo = fs.readFileSync('src/main/webapp/assets/js/activar-personal.js','utf8');
const esperar = () => new Promise(resolve => setImmediate(resolve));
function entorno(hash='',respuesta={ok:true,code:'PERSONAL_ACTIVADO',message:'Cuenta activada'}) {
    const eventos=[]; const boton={disabled:false,textContent:''};const mensaje={textContent:''};
    const form={removed:false,remove(){this.removed=true;},addEventListener(tipo,fn){this.enviar=fn;}};
    const location={pathname:'/activar-personal',hash,reload(){eventos.push('recarga');}};
    const llamadas=[];
    const contexto={URLSearchParams,AbortController,FormData:class { *[Symbol.iterator](){yield ['action','activar'];yield ['csrfToken','csrf'];}},
        setTimeout(){return 1;},clearTimeout(){},location,history:{replaceState(){eventos.push('ocultar-fragmento');location.hash='';}},
        document:{getElementById(id){return id==='activacionMensaje'?mensaje:form;},querySelector(selector){return selector.includes('meta')?{content:'csrf'}:boton;}},
        async fetch(url,opciones){eventos.push('fetch');llamadas.push({url,opciones});return {ok:respuesta.ok,json:async()=>respuesta};}};
    vm.runInNewContext(codigo,contexto);return {eventos,boton,mensaje,form,llamadas};
}
test('elimina el secreto de la dirección antes de enviarlo por POST protegido',async()=>{
    const e=entorno('#'+'a'.repeat(43),{ok:true,code:'INVITACION_PREPARADA',message:'Preparada'});await esperar();
    assert.deepEqual(e.eventos,['ocultar-fragmento','fetch','recarga']);assert.equal(e.form.removed,true);
    assert.equal(e.llamadas[0].url,'/activar-personal');assert.equal(e.llamadas[0].opciones.method,'POST');
    assert.equal(e.llamadas[0].opciones.body.get('token'),'a'.repeat(43));assert.equal(e.llamadas[0].opciones.headers['X-CSRF-Token'],'csrf');
});
test('bloquea el doble envío y retira el formulario solo al activar',async()=>{
    const e=entorno();const evento={preventDefault(){},currentTarget:e.form};e.form.enviar(evento);e.form.enviar(evento);
    assert.equal(e.boton.disabled,true);await esperar();assert.equal(e.llamadas.length,1);assert.equal(e.form.removed,true);assert.equal(e.mensaje.textContent,'Cuenta activada');
});
test('un error conserva el formulario y permite reintentar sin usar HTML del servidor',async()=>{
    const e=entorno('',{ok:false,code:'PASSWORD_INVALIDA',message:'<b>Revisa la contraseña</b>'});e.form.enviar({preventDefault(){},currentTarget:e.form});await esperar();
    assert.equal(e.form.removed,false);assert.equal(e.boton.disabled,false);assert.equal(e.mensaje.textContent,'<b>Revisa la contraseña</b>');
});
