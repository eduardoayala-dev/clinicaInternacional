'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {spawnSync}=require('node:child_process');
const {generar}=require('./generar-datos.cjs');

const raiz=path.resolve(__dirname,'../..');
const carpeta=path.join(raiz,'target/demo-san-borja');
fs.mkdirSync(carpeta,{recursive:true});
const sufijo=crypto.randomBytes(6).toString('hex');
const esquema='clinica_agendas_test_demo_'+sufijo;
const usuario='qa_demo_'+sufijo;
const contenedor='clinica-demo-'+sufijo;
const clave=crypto.randomBytes(32).toString('hex');
const archivoHash=path.join(carpeta,'hashes.json');
if(!process.env.DEMO_ADMIN_PASSWORD)throw Error('Define DEMO_ADMIN_PASSWORD solo para esta ejecución');
function comando(programa,args,opciones={}) {
    const r=spawnSync(programa,args,{encoding:'utf8',maxBuffer:32*1024*1024,timeout:120000,...opciones});
    if(r.status!==0)throw Error('Falló '+programa+': '+(r.stderr||r.error?.message||'salida '+r.status));
    return r.stdout;
}
function sql(consulta) {
    return comando('docker',['exec','-i','clinicainternacional-db-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -u root -N -B'],{input:consulta});
}
function origen() {return sql('SELECT (SELECT COUNT(*) FROM clinica_db.usuario),(SELECT COUNT(*) FROM clinica_db.cita);').trim();}
const antes=origen();
let creado=false,app=false;
async function main() {
    const classpath=path.join(raiz,'target/classes')+path.delimiter+path.join(raiz,'target/clinicaInternacional/WEB-INF/lib/*');
    comando('java',['-cp',classpath,path.join(__dirname,'PrepararHashes.java'),archivoHash]);
    const tablas=sql("SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA='clinica_db' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;").trim().split(/\r?\n/);
    const estructura=comando('docker',['exec','clinicainternacional-db-1','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -u root --no-data --skip-add-drop-table --skip-comments --set-gtid-purged=OFF clinica_db']);
    if(/^USE\s|^CREATE DATABASE/m.test(estructura))throw Error('El esquema exportado no está aislado');
    sql('CREATE DATABASE `'+esquema+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;');creado=true;
    sql('USE `'+esquema+'`;\n'+estructura);
    const fechaBase=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    const horaBase=new Intl.DateTimeFormat('en-GB',{timeZone:'America/Lima',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).format(new Date());
    const hashes=JSON.parse(fs.readFileSync(archivoHash,'utf8'));
    const resultado=generar({fechaBase,horaBase,hashes,tablas,baseDatos:esquema});
    fs.writeFileSync(path.join(carpeta,'carga.sql'),resultado.sql);
    sql(resultado.sql);
    const fks=sql("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='"+esquema+"' AND REFERENCED_TABLE_NAME IS NOT NULL;").trim().split(/\r?\n/).filter(Boolean);
    for(const linea of fks) {
        const [tabla,columna,padre,referencia]=linea.split('\t');
        if(![tabla,columna,padre,referencia].every(v=>/^[a-z_]+$/.test(v)))throw Error('Identificador inesperado');
        const huerfanos=Number(sql('SELECT COUNT(*) FROM `'+esquema+'`.`'+tabla+'` h LEFT JOIN `'+esquema+'`.`'+padre+'` p ON h.`'+columna+'`=p.`'+referencia+'` WHERE h.`'+columna+'` IS NOT NULL AND p.`'+referencia+'` IS NULL;').trim());
        if(huerfanos)throw Error('Relación inconsistente: '+tabla+'.'+columna);
    }
    const consultas={
        duplicadosMedico:"SELECT COUNT(*) FROM (SELECT id_medico,fecha_hora FROM cita WHERE estado NOT IN ('CANCELADA','REPROGRAMADA') GROUP BY id_medico,fecha_hora HAVING COUNT(*)>1) x",
        duplicadosPaciente:"SELECT COUNT(*) FROM (SELECT id_paciente,fecha_hora FROM cita WHERE estado NOT IN ('CANCELADA','REPROGRAMADA') GROUP BY id_paciente,fecha_hora HAVING COUNT(*)>1) x",
        fueraHorario:"SELECT COUNT(*) FROM cita c JOIN disponibilidad d USING(id_disponibilidad) WHERE DATE(c.fecha_hora)<>d.fecha OR TIME(c.fecha_hora)<d.hora_inicio OR TIME(c.fecha_hora)>=d.hora_fin OR c.id_medico<>d.id_medico",
        motivoIncompatible:"SELECT COUNT(*) FROM cita c JOIN medico m USING(id_medico) JOIN motivo_cita mc USING(id_motivo) WHERE m.id_especialidad<>mc.id_especialidad",
        pasadoActivo:"SELECT COUNT(*) FROM cita WHERE fecha_hora<'"+fechaBase+"' AND estado IN ('SOLICITADA','CONFIRMADA','EN_SALA','EN_ATENCION')",
        correosPendientes:"SELECT (SELECT COUNT(*) FROM correo_cita WHERE estado='PENDIENTE')+(SELECT COUNT(*) FROM correo_recordatorio WHERE estado='PENDIENTE')"
    };
    const verificaciones={};
    for(const [nombre,consulta] of Object.entries(consultas)){const n=Number(sql('USE `'+esquema+'`;'+consulta+';').trim());verificaciones[nombre]=n;if(n!==0)throw Error('Validación fallida: '+nombre);}
    sql("CREATE USER '"+usuario+"'@'%' IDENTIFIED BY '"+clave+"' REQUIRE SSL; GRANT SELECT,INSERT,UPDATE,DELETE ON `"+esquema+"`.* TO '"+usuario+"'@'%';");
    const env={...process.env,DB_PASSWORD:clave,AUTH_JWT_KEY:crypto.randomBytes(32).toString('base64')};
    comando('docker',['run','-d','--name',contenedor,'--network','clinicainternacional_default','-p','127.0.0.1:8084:8080','--memory','512m','--mount','type=bind,source='+path.join(raiz,'target/clinicaInternacional.war')+',target=/usr/local/tomcat/webapps/ROOT.war,readonly','-e','APP_ENV=development','-e','DB_MODE=mysql','-e','DB_HOST=db','-e','DB_PORT=3306','-e','DB_NAME='+esquema,'-e','DB_USER='+usuario,'-e','DB_PASSWORD','-e','DB_SSL_MODE=REQUIRED','-e','AUTH_MODE=jwt','-e','AUTH_JWT_KEY','-e','AUTH_ISSUER=clinica-internacional','-e','AUTH_AUDIENCE=clinica-web','-e','APPOINTMENT_EMAIL_ENABLED=false','-e','RECORDATORIOS_EMAIL_ENABLED=false','-e','WAITLIST_EMAIL_ENABLED=false','-e','JAVA_OPTS=-Xmx256m','clinicainternacional-app:admision-corregida'],{env});app=true;
    let listo=false;
    for(let i=0;i<60;i++) {
        try{const respuesta=await fetch('http://127.0.0.1:8084/health/ready',{signal:AbortSignal.timeout(2000)});if(respuesta.ok){listo=true;break;}}catch{}
        await new Promise(r=>setTimeout(r,1000));
    }
    if(!listo){fs.writeFileSync(path.join(carpeta,'arranque.log'),comando('docker',['logs',contenedor]));throw Error('No inició la instancia de ensayo; revisar target/demo-san-borja/arranque.log');}
    const cookieJar=()=>({cookies:new Map(),header(){return Array.from(this.cookies,([k,v])=>k+'='+v).join('; ');},guardar(res){for(const cookie of res.headers.getSetCookie()){const par=cookie.split(';')[0];const i=par.indexOf('=');this.cookies.set(par.slice(0,i),par.slice(i+1));}}});
    async function ingresar(email,ruta) {
        const jar=cookieJar();
        const pagina=await fetch('http://127.0.0.1:8084/login/personal');jar.guardar(pagina);
        const html=await pagina.text();const token=html.match(/name="csrfToken" value="([^"]+)"/)?.[1];if(!token)throw Error('No se encontró CSRF');
        const login=await fetch('http://127.0.0.1:8084/login/personal',{method:'POST',redirect:'manual',headers:{Cookie:jar.header(),'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({identificador:email,password:process.env.DEMO_ADMIN_PASSWORD,csrfToken:token})});jar.guardar(login);
        if(![302,303].includes(login.status)||!login.headers.get('location')?.includes(ruta))throw Error('Inicio de sesión no verificado: '+email);
        const destinos=ruta==='/admin/dashboard'?['/admin/dashboard','/admin/medicos','/admin/dashboard-operativo']:['/sysadmin/dashboard','/sysadmin/usuarios','/sysadmin/catalogo-sedes'];
        for(const destino of destinos){const res=await fetch('http://127.0.0.1:8084'+destino,{redirect:'manual',headers:{Cookie:jar.header()}});if(res.status!==200)throw Error('Vista no disponible: '+destino+' ('+res.status+')');await res.text();}
        return true;
    }
    verificaciones.loginAdmin=await ingresar('eduardo@admin.com','/sysadmin/dashboard');
    verificaciones.loginAdmision=await ingresar('eduardo@admision.com','/admin/dashboard');
    if(origen()!==antes)throw Error('Cambió el conteo de la base original durante el ensayo');
    const resumen={...resultado.resumen,verificaciones,relacionesVerificadas:fks.length,tablasInventario:tablas,relaciones:fks.map(l=>l.split('\t')),baseOriginalConservada:true,instancia:'http://127.0.0.1:8084',contenedor,usuarioTemporal:usuario};
    fs.writeFileSync(path.join(carpeta,'resultado.json'),JSON.stringify(resumen,null,2));
    console.log(JSON.stringify({fechaBase:resumen.fechaBase,horaBase:resumen.horaBase,tablas:resumen.tablas,estados:resumen.estados,verificaciones:resumen.verificaciones,relacionesVerificadas:resumen.relacionesVerificadas,instancia:resumen.instancia},null,2));
}
main().catch(error=>{
    if(app)comando('docker',['rm','-f',contenedor]);
    if(creado)sql('DROP DATABASE IF EXISTS `'+esquema+'`;DROP USER IF EXISTS \''+usuario+'\'@\'%\';');
    console.error(error.message);process.exitCode=1;
}).finally(()=>{if(fs.existsSync(archivoHash))fs.unlinkSync(archivoHash);});
