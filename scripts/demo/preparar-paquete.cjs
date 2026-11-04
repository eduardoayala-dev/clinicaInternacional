'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const zlib=require('node:zlib');

const carpeta=path.resolve(__dirname,'../../target/demo-san-borja');
const informe=JSON.parse(fs.readFileSync(path.join(carpeta,'resultado.json'),'utf8'));
const destino=process.argv[2];
if(!destino || !/^[A-Za-z0-9_]+$/.test(destino))throw Error('Indica el nombre de la base destino, por ejemplo railway');
if(!informe.baseOriginalConservada||!informe.verificaciones.loginAdmin||!informe.verificaciones.loginAdmision)throw Error('Primero debe aprobarse el ensayo local');
const original=fs.readFileSync(path.join(carpeta,'carga.sql'),'utf8');
const cuerpo=original.split('\n').filter(l=>!/^SET NAMES|^USE |^SET FOREIGN_KEY_CHECKS|^START TRANSACTION;|^COMMIT;/.test(l)).join('\n');
const lista=informe.tablasInventario.map(t=>"'"+t+"'").join(',');
const comprobaciones=informe.relaciones.map(([tabla,columna,padre,referencia])=>{
    if(![tabla,columna,padre,referencia].every(v=>/^[a-z_]+$/.test(v)))throw Error('Relación inválida');
    return "IF EXISTS(SELECT 1 FROM `"+tabla+"` h LEFT JOIN `"+padre+"` p ON h.`"+columna+"`=p.`"+referencia+"` WHERE h.`"+columna+"` IS NOT NULL AND p.`"+referencia+"` IS NULL) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Relacion invalida: "+tabla+'.'+columna+"'; END IF;";
}).join('\n');
const sql=`SET NAMES utf8mb4;
USE \`${destino}\`;
DROP PROCEDURE IF EXISTS cargar_demo_san_borja;
DELIMITER $$
CREATE PROCEDURE cargar_demo_san_borja()
BEGIN
    DECLARE fk_anterior INT DEFAULT @@FOREIGN_KEY_CHECKS;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET FOREIGN_KEY_CHECKS=fk_anterior;
        RESIGNAL;
    END;
    IF DATABASE()<>'${destino}' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Base destino incorrecta'; END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE' AND TABLE_NAME IN (${lista}) AND ENGINE='InnoDB')<>${informe.tablasInventario.length}
       OR (SELECT COUNT(*) FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE')<>${informe.tablasInventario.length}
    THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Estructura distinta al ensayo: no se borraron datos'; END IF;
    SET FOREIGN_KEY_CHECKS=0;
    START TRANSACTION;
${cuerpo}
${comprobaciones}
    IF (SELECT COUNT(*) FROM sede)<>1 OR (SELECT nombre FROM sede LIMIT 1)<>'Sede San Borja' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Sede no valida'; END IF;
    IF (SELECT COUNT(*) FROM usuario WHERE rol='ADMINISTRADOR')<>1 OR (SELECT COUNT(*) FROM usuario WHERE rol='PERSONAL_ADMINISTRATIVO')<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cuentas de gestion no validas'; END IF;
    COMMIT;
    SET FOREIGN_KEY_CHECKS=fk_anterior;
END$$
DELIMITER ;
CALL cargar_demo_san_borja();
DROP PROCEDURE cargar_demo_san_borja;
SELECT rol,COUNT(*) AS cuentas FROM usuario GROUP BY rol;
SELECT estado,COUNT(*) AS citas FROM cita GROUP BY estado;
`;
const nombre='carga-san-borja-'+destino+'.sql';
fs.writeFileSync(path.join(carpeta,nombre),sql);
const comprimido=zlib.gzipSync(Buffer.from(sql,'utf8'),{level:9});
fs.writeFileSync(path.join(carpeta,nombre+'.gz'),comprimido);
const sha=crypto.createHash('sha256').update(sql).digest('hex');
const lineas=comprimido.toString('base64').match(/.{1,100}/g).join('\n');
const preparacion=`#!/usr/bin/env bash
set -euo pipefail
umask 077
destino=$(mktemp /var/lib/mysql/demo-san-borja.XXXXXXXX.sql)
base64 -d --ignore-garbage <<'DATOS_CLINICA' | gzip -d > "$destino"
${lineas}
DATOS_CLINICA
printf '%s  %s\\n' '${sha}' "$destino" | sha256sum -c -
printf '\\nArchivo preparado, todavia NO se ha modificado la base de datos: %s\\n' "$destino"
printf 'Antes de cargar: respalda la base ${destino} y detén temporalmente el servicio Java.\\n'
`;
fs.writeFileSync(path.join(carpeta,'preparar-archivo-railway.sh'),preparacion);
fs.writeFileSync(path.join(carpeta,nombre+'.sha256'),sha+'  '+nombre+'\n');
console.log(JSON.stringify({archivo:nombre,bytes:Buffer.byteLength(sql),bytesComprimidos:comprimido.length,sha256:sha,baseDestino:destino}));
