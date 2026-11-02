const {test}=require('node:test');
const assert=require('node:assert/strict');
const {disponibles,poblar,sugerido}=require('../src/main/webapp/assets/js/motivos-cita.js');
const catalogo=[{idMotivo:71,idEspecialidad:1,descripcion:'Control cardíaco'},{idMotivo:84,idEspecialidad:2,descripcion:'Revisión de piel'}];
globalThis.Option=class {constructor(text,value){this.textContent=text;this.value=value;}};
function select(){return {options:[],value:'',replaceChildren(){this.options=[];},appendChild(option){this.options.push(option);}};}
test('filtra por especialidad conservando los identificadores reales',()=>{
    assert.deepEqual(disponibles(catalogo,'2').map(m=>m.idMotivo),[84]);
});
test('no conserva un motivo de otra especialidad ni inventa opciones',()=>{
    const campo=select();poblar(campo,catalogo,2,'71');
    assert.equal(campo.value,'');assert.equal(campo.options[1].value,'84');
    assert.equal(campo.options[0].disabled,true);
    poblar(campo,catalogo,2,'84');assert.equal(campo.value,'84');
    poblar(campo,catalogo,99);assert.equal(campo.disabled,true);assert.equal(campo.options.length,1);
});
test('solo acepta una sugerencia que exista en la especialidad actual',()=>{
    assert.equal(sugerido(catalogo,2,84).descripcion,'Revisión de piel');
    assert.equal(sugerido(catalogo,1,84),null);
    assert.equal(sugerido(catalogo,2,999),null);
    assert.equal(sugerido(catalogo,2,0),null);
});
