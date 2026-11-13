'use strict';
const fs=require('node:fs');
const path=require('node:path');
const {spawnSync}=require('node:child_process');
const archivo=path.resolve(__dirname,'../../target/demo-san-borja/resultado.json');
const resultado=JSON.parse(fs.readFileSync(archivo,'utf8'));
const sufijo=resultado.baseDatos.match(/^clinica_agendas_test_demo_([a-f0-9]{12})$/)?.[1];
if(!sufijo || resultado.contenedor!=='clinica-demo-'+sufijo || resultado.usuarioTemporal!=='qa_demo_'+sufijo)throw Error('El manifiesto no corresponde a un ensayo aislado');
function ejecutar(args,input) {
    const r=spawnSync('docker',args,{input,encoding:'utf8',timeout:120000});
    if(r.status!==0)throw Error(r.stderr||'No se pudo limpiar el ensayo');
}
ejecutar(['rm','-f',resultado.contenedor]);
ejecutar(['exec','-i','clinicainternacional-db-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root'],"DROP DATABASE IF EXISTS `"+resultado.baseDatos+"`; DROP USER IF EXISTS '"+resultado.usuarioTemporal+"'@'%';");
console.log('Eliminados solo el contenedor, la base y el usuario temporales del ensayo.');
