const test=require('node:test');const assert=require('node:assert/strict');
const {iniciar}=require('../src/main/webapp/assets/js/importar-motivos.js');
function vista(archivo) {
 const eventos={};const boton={dataset:{},textContent:'Revisar archivo',disabled:false};
 const campo=archivo===null?null:{files:[archivo],setAttribute(){},removeAttribute(){},focus(){}};
 const form={dataset:{},addEventListener(n,f){eventos[n]=f},removeAttribute(){},setAttribute(){},querySelector(){return campo},querySelectorAll(){return[boton]}};
 const error={hidden:true,textContent:''};const ventana={addEventListener(n,f){eventos[n]=f}};
 iniciar({querySelectorAll(){return[form]},getElementById(){return error}},ventana);
 return{eventos,boton,error,form};
}
test('bloquea el archivo excesivo antes de iniciar el envío',()=>{const v=vista({name:'motivos.xlsx',size:1048577});let bloqueado=false;v.eventos.submit({preventDefault(){bloqueado=true}});assert.equal(bloqueado,true);assert.equal(v.error.hidden,false);assert.equal(v.boton.disabled,false);assert.equal(v.form.dataset.enviando,undefined)});
test('archivo válido bloquea duplicados y volver a la página recupera el botón',()=>{const v=vista({name:'motivos.XLSX',size:3000});v.eventos.submit({preventDefault(){throw Error('No debería bloquear')}});assert.equal(v.boton.disabled,true);let duplicado=false;v.eventos.submit({preventDefault(){duplicado=true}});assert.equal(duplicado,true);v.eventos.pageshow();assert.equal(v.boton.disabled,false);assert.equal(v.boton.textContent,'Revisar archivo')});
test('confirmación muestra el envío y bloquea un segundo submit',()=>{const v=vista(null);v.eventos.submit({preventDefault(){throw Error('No debería bloquear')}});assert.equal(v.boton.disabled,true);assert.equal(v.boton.textContent,'Importando motivos…')});
