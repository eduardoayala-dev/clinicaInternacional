'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {spawnSync}=require('node:child_process');
const raiz=path.resolve(__dirname,'../..');
const carpeta=path.join(raiz,'target/demo-san-borja');
const esquema='clinica_agendas_test_carga_'+crypto.randomBytes(6).toString('hex');
function comando(programa,args,opciones={}) {
    const r=spawnSync(programa,args,{encoding:'utf8',maxBuffer:32*1024*1024,timeout:120000,...opciones});
    if(r.status!==0)throw Error((r.stderr||r.error?.message||'Error de proceso').slice(0,2000));
    return r.stdout;
}
function mysql(consulta,esperarError=false) {
    const r=spawnSync('docker',['exec','-i','clinicainternacional-db-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -u root -N -B'],{input:consulta,encoding:'utf8',maxBuffer:32*1024*1024,timeout:120000});
    if(esperarError){if(r.status===0)throw Error('La carga debió rechazarse');return;}
    if(r.status!==0)throw Error((r.stderr||'No se pudo completar MySQL').slice(0,2000));
    return r.stdout.trim();
}
let creado=false;
try {
    const estructura=comando('docker',['exec','clinicainternacional-db-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --no-data --skip-add-drop-table --skip-comments --set-gtid-purged=OFF clinica_db']);
    mysql('CREATE DATABASE `'+esquema+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;');creado=true;
    mysql('USE `'+esquema+'`;\n'+estructura);
    comando(process.execPath,[path.join(__dirname,'preparar-paquete.cjs'),esquema]);
    const archivo=path.join(carpeta,'carga-san-borja-'+esquema+'.sql');
    const contenido=fs.readFileSync(archivo,'utf8');
    mysql(contenido);
    const resumen=JSON.parse(fs.readFileSync(path.join(carpeta,'resultado.json'),'utf8'));
    const conteo=mysql('SELECT COUNT(*) FROM `'+esquema+'`.cita;');
    if(Number(conteo)!==resumen.tablas.cita)throw Error('El paquete no produjo el conteo esperado');
    mysql('UPDATE `'+esquema+'`.usuario SET nombres=\'Debe conservarse\' WHERE id_usuario=1;');
    const fallo=contenido.replace("    IF (SELECT COUNT(*) FROM sede)<>1", "    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo provocado para comprobar rollback';\n    IF (SELECT COUNT(*) FROM sede)<>1");
    mysql(fallo,true);
    if(mysql('SELECT nombres FROM `'+esquema+'`.usuario WHERE id_usuario=1;')!=='Debe conservarse')throw Error('No se conservó el registro al revertir');
    if(Number(mysql('SELECT COUNT(*) FROM `'+esquema+'`.cita;'))!==resumen.tablas.cita)throw Error('No se revirtieron las citas');
    mysql('CREATE TABLE `'+esquema+'`.tabla_ajena(id INT PRIMARY KEY) ENGINE=InnoDB;');
    mysql(contenido,true);
    if(mysql('SELECT nombres FROM `'+esquema+'`.usuario WHERE id_usuario=1;')!=='Debe conservarse')throw Error('Se modificaron datos pese a la diferencia estructural');
    const resultado={cargaCompleta:true,reversionAnteError:true,rechazoEstructuraDiferente:true,citas:resumen.tablas.cita};
    fs.writeFileSync(path.join(carpeta,'verificacion-paquete.json'),JSON.stringify(resultado,null,2));
    console.log(JSON.stringify(resultado));
} finally {
    if(creado)mysql('DROP DATABASE `'+esquema+'`;');
}
