'use strict';
const fs = require('node:fs');
const path = require('node:path');

function generar({ fechaBase, horaBase = '12:00:00', hashes, tablas, baseDatos }) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(fechaBase) || !/^[a-zA-Z0-9_]+$/.test(baseDatos)) throw Error('Fecha o base inválida');
    if (!['admin','admision','simulado'].every(k => /^\$2[aby]\$10\$[./A-Za-z0-9]{53}$/.test(hashes[k] || ''))) throw Error('Se requieren hashes BCrypt válidos');
    if (!Array.isArray(tablas) || tablas.length < 20 || tablas.some(t => !/^[a-z_]+$/.test(t))) throw Error('Inventario de tablas inválido');
    const datos = {};
    const agregar = (tabla, fila) => { if (!tablas.includes(tabla)) throw Error('Tabla ausente: '+tabla); (datos[tabla] ??= []).push(fila); };
    const dia = delta => new Date(Date.parse(fechaBase+'T12:00:00Z') + delta*86400000).toISOString().slice(0,10);
    if (dia(0) !== fechaBase) throw Error('Fecha inexistente');
    const fecha = (delta, hora='08:00:00') => dia(delta)+' '+hora;
    const nombres = ['Valeria','Mateo','Lucía','Santiago','Camila','Sebastián','Mariana','Nicolás','Gabriela','Joaquín','Daniela','Adrián','Renata','Alonso','Ximena','Emiliano','Paula','Diego','Fernanda','Leonardo'];
    const apellidos = ['Rivas','Mendoza','Salazar','Paredes','Vargas','Cáceres','Benavides','Peña','Delgado','Núñez','Aguilar','Sánchez','Palacios','Zegarra','Córdova','Huamán','Torres','Medina','Soto','Velásquez'];
    const especialidades = [
        ['Cardiología',['Control de hipertensión arterial','Consulta cardiológica preventiva','Revisión de resultados cardiológicos']],
        ['Pediatría',['Control de crecimiento y desarrollo','Control pediátrico preventivo','Revisión de resultados pediátricos']],
        ['Dermatología',['Evaluación de lesiones en la piel','Control de acné','Control dermatológico']],
        ['Medicina Interna',['Control de enfermedad crónica','Evaluación integral del adulto','Revisión de resultados']],
        ['Traumatología y Ortopedia',['Evaluación de dolor articular','Control de lesión musculoesquelética','Control de recuperación funcional']],
        ['Ginecología y Obstetricia',['Control ginecológico preventivo','Control prenatal','Revisión de resultados ginecológicos']],
        ['Oftalmología',['Evaluación de agudeza visual','Control oftalmológico','Revisión de resultados oftalmológicos']],
        ['Gastroenterología',['Evaluación de molestias digestivas','Control de reflujo','Revisión de resultados digestivos']],
        ['Neurología',['Evaluación de cefalea','Control neurológico','Revisión de resultados neurológicos']],
        ['Otorrinolaringología',['Evaluación de molestias de oído','Evaluación de congestión nasal','Control otorrinolaringológico']],
        ['Endocrinología',['Control de tiroides','Control de diabetes','Revisión de resultados hormonales']],
        ['Urología',['Control urológico preventivo','Evaluación de molestias urinarias','Revisión de resultados urológicos']],
        ['Medicina General',['Evaluación general','Control preventivo','Fiebre y malestar general']]
    ];
    agregar('sede',{id_sede:1,codigo:'SED-SB',nombre:'Sede San Borja',direccion:'Av. Guardia Civil 385, San Borja',telefono:'016190000',estado:'ACTIVO'});
    especialidades.forEach(([nombre,motivos],i) => {
        agregar('especialidad',{id_especialidad:i+1,nombre,descripcion:'Consulta externa de '+nombre,estado:'ACTIVO'});
        motivos.forEach((descripcion,j) => agregar('motivo_cita',{id_motivo:i*3+j+1,id_especialidad:i+1,descripcion,estado:'ACTIVO'}));
    });
    agregar('usuario',{id_usuario:1,nombres:'Eduardo',apellidos:'Ayala',email:'eduardo@admin.com',password_hash:hashes.admin,rol:'ADMINISTRADOR',activo:1,fecha_registro:fecha(-75)});
    agregar('usuario',{id_usuario:2,nombres:'Eduardo',apellidos:'Ayala',email:'eduardo@admision.com',password_hash:hashes.admision,rol:'PERSONAL_ADMINISTRATIVO',activo:1,fecha_registro:fecha(-75)});
    agregar('administrador',{id_administrador:1,id_usuario:1,estado:'ACTIVO'});
    agregar('personal_administrativo',{id_personal:1,id_usuario:2,id_sede:1,codigo_empleado:'ADM-SB-001',cargo:'Admisión'});
    const medicos = [];
    for (let i=0;i<26;i++) {
        const id=i+1, uid=100+id, especialidad=i%13+1;
        const medico={id,uid,especialidad,mascara:i%2===0?21:42,inicio:i<13?8:14};
        medicos.push(medico);
        agregar('usuario',{id_usuario:uid,nombres:nombres[(i+4)%20],apellidos:apellidos[i%20]+' '+apellidos[(i+7)%20],email:'medico.'+String(id).padStart(3,'0')+'@clinica.example.test',password_hash:hashes.simulado,rol:'MEDICO',activo:1,fecha_registro:fecha(-75)});
        agregar('medico',{id_medico:id,id_usuario:uid,cmp:String(70000+id),id_especialidad:especialidad,id_sede:1,estado:'ACTIVO'});
        agregar('propuesta_horario',{id_propuesta:id,id_medico:id,id_sede:1,dias_semana:medico.mascara,vigente_desde:dia(-60),vigente_hasta:dia(29),hora_inicio:String(medico.inicio).padStart(2,'0')+':00:00',hora_fin:String(medico.inicio+4).padStart(2,'0')+':00:00',duracion_minutos:30,estado:'PUBLICADO',creada_en:fecha(-65),revisada_en:fecha(-64),id_revisor:2});
    }
    for (let i=0;i<180;i++) {
        const id=i+1, uid=1000+id;
        agregar('usuario',{id_usuario:uid,nombres:nombres[i%20],apellidos:apellidos[Math.floor(i/20)]+' '+apellidos[(i+9)%20],email:'paciente.'+String(id).padStart(3,'0')+'@clinica.example.test',password_hash:hashes.simulado,rol:'PACIENTE',activo:1,fecha_registro:fecha(-75)});
        agregar('paciente',{id_paciente:id,id_usuario:uid,dni:String(99000000+id),fecha_nacimiento:(i<30?2012+i%8:1954+i%45)+'-'+String(i%12+1).padStart(2,'0')+'-'+String(i%27+1).padStart(2,'0'),telefono:String(900000000+id),direccion:['San Borja Norte','San Borja Sur','San Luis','Surquillo'][i%4],canal_aviso:'CORREO',sede_preferida:'Sede San Borja',alergias:i%17===0?'Penicilina':null,tipo_cobertura:i%3===0?'EPS':'Particular'});
    }
    let disponibilidad=0,cita=0;
    const citasPaciente=new Set();
    for(let offset=-60;offset<=29;offset++) {
        const semana=new Date(dia(offset)+'T12:00:00Z').getUTCDay();
        if(semana===0)continue;
        for(const medico of medicos) {
            if(!(medico.mascara & (1<<(semana-1))))continue;
            const idDisp=++disponibilidad;
            agregar('disponibilidad',{id_disponibilidad:idDisp,id_propuesta:medico.id,id_medico:medico.id,id_sede:1,fecha:dia(offset),hora_inicio:String(medico.inicio).padStart(2,'0')+':00:00',hora_fin:String(medico.inicio+4).padStart(2,'0')+':00:00',duracion_minutos:30,estado:'PUBLICADO'});
            
            for(const slot of [0,3]) {
                const hora=String(medico.inicio+Math.floor(slot/2)).padStart(2,'0')+':'+(slot%2?'30':'00')+':00';
                const finTurno=new Date(Date.parse(fecha(offset,hora).replace(' ','T')+'Z')+1800000).toISOString().slice(0,19).replace('T',' ');
                const actual=fechaBase+' '+horaBase;
                if(offset===0 && fecha(offset,hora)<=actual && finTurno>actual)continue;
                const pasada=finTurno<=actual;
                const esNino=medico.especialidad===2;
                const limite=esNino?30:150, inicio=esNino?1:31;
                let paciente=inicio+((offset+60)*13+medico.id*7+slot)%limite;
                while((medico.especialidad===6&&paciente%2===0)||citasPaciente.has(dia(offset)+' '+hora+' '+paciente))paciente=inicio+(paciente-inicio+1)%limite;
                citasPaciente.add(dia(offset)+' '+hora+' '+paciente);
                const idCita=++cita;
                const estado=pasada?(idCita%11===0?'CANCELADA':idCita%9===0?'NO_ASISTIO':'ATENDIDA'):'CONFIRMADA';
                const prioridad=['BAJA','MEDIA','ALTA'][idCita%3];
                const creada=Math.min(-1,offset-7);
                agregar('cita',{id_cita:idCita,id_paciente:paciente,id_medico:medico.id,id_disponibilidad:idDisp,id_motivo:(medico.especialidad-1)*3+slot%3+1,fecha_hora:fecha(offset,hora),estado,nivel_urgencia:prioridad,fecha_recomendada_proxima:estado==='ATENDIDA'&&offset>=-10?dia(20):null,riesgo_cancelacion:null,fecha_creacion:fecha(creada)});
                agregar('confirmacion_asistencia',{id_cita:idCita,creada_en:fecha(creada),vence_en:fecha(offset-1,hora),confirmada_en:idCita%3===0?null:fecha(Math.min(-1,offset-2)),canal:idCita%3===0?null:'WEB',id_usuario_confirmo:idCita%3===0?null:1000+paciente});
                for(const tabla of ['correo_cita','correo_recordatorio'])agregar(tabla,{id_cita:idCita,estado:'OMITIDO',intentos:0,disponible_en:fecha(Math.min(0,offset-1)),finalizada_en:fecha(0)});
                agregar('notificacion',{id_usuario:1000+paciente,id_cita:idCita,tipo:'CITA_CONFIRMADA',canal:'PLATAFORMA',mensaje:'Cita confirmada para el '+dia(offset)+' a las '+hora.slice(0,5)+'.',fecha_envio:fecha(creada),estado_envio:offset<0?'LEIDA':'ENTREGADA',leida:offset<0?1:0});
                if(offset>=0)agregar('correo_cambio_cita',{id_cita:idCita,tipo:'CONFIRMACION_VENCIDA',estado:'OMITIDO',intentos:0,disponible_en:fecha(0),finalizada_en:fecha(0)});
                if(estado==='ATENDIDA') {
                    const inicioCita=Date.parse(fecha(offset,hora).replace(' ','T')+'Z');
                    const formato = instante=>new Date(instante).toISOString().slice(0,19).replace('T',' ');
                    agregar('admision_cita',{id_cita:idCita,id_usuario_personal:2,fecha_llegada:formato(inicioCita-600000),tolerancia_minutos:10,llego_tarde:0});
                    agregar('atencion_cita',{id_cita:idCita,id_medico:medico.id,id_paciente:paciente,fecha_inicio:formato(inicioCita),fecha_fin:formato(inicioCita+1500000),id_especialidad_prioridad:medico.especialidad});
                    agregar('consulta_clinica',{id_cita:idCita,motivo:especialidades[medico.especialidad-1][1][slot%3],antecedentes:'Antecedentes revisados según la ficha del paciente.',hallazgos:'Evaluación ambulatoria de control.',evaluacion:'Seguimiento ambulatorio.',diagnostico:'Consulta de control.',plan:'Control según valoración del profesional.'});
                    agregar('historial_asistencia',{id_paciente:paciente,id_cita:idCita,asistio:1,fecha_registro:formato(inicioCita+1500000),observacion:'Atención finalizada'});
                } else if(estado==='NO_ASISTIO') {
                    agregar('inasistencia_cita',{id_cita:idCita,id_usuario_personal:2,fecha_registro:fecha(offset,'19:00:00'),motivo:'No se registró llegada dentro del horario de atención.',tolerancia_minutos:10});
                    agregar('historial_asistencia',{id_paciente:paciente,id_cita:idCita,asistio:0,fecha_registro:fecha(offset,'19:00:00'),observacion:'Inasistencia registrada'});
                } else if(estado==='CANCELADA') {
                    agregar('notificacion',{id_usuario:1000+paciente,id_cita:idCita,tipo:'CITA_CANCELADA',canal:'PLATAFORMA',mensaje:'Tu cita médica ha sido cancelada correctamente.',fecha_envio:fecha(offset-1),estado_envio:'LEIDA',leida:1});
                    agregar('auditoria_seguridad',{id_usuario:1000+paciente,evento:'CITA_CANCELADA_PACIENTE',descripcion:'Cita #'+idCita+'; motivo: Cambio de disponibilidad del paciente.',sospechoso:0,fecha_registro:fecha(offset-1)});
                }
            }
        }
    }
    for(let i=0;i<18;i++)agregar('lista_espera',{id_lista_espera:i+1,id_paciente:31+i,id_especialidad:i%13+1,id_sede:1,id_motivo:(i%13)*3+1,nivel_urgencia:'MEDIA',rango_horario:i%2?'TARDE':'MANANA',canal_notificacion:'CORREO',fecha_registro:fecha(-5+i%3),estado:'ACTIVA',posicion:i+1,anticipacion_minutos:180});
    agregar('configuracion_ofertas',{id_configuracion:1,minutos_respuesta:15,anticipacion_minima:0,version:0,confirmacion_horas:24,tolerancia_llegada:10,recordatorio_horas:24,intentos_confirmacion:2});
    agregar('auditoria_seguridad',{id_usuario:1,evento:'CARGA_DATOS_SIMULADOS',descripcion:'Clínica Internacional, Sede San Borja. Conjunto sintético de demostración; no representa actividad real.',sospechoso:0,fecha_registro:fecha(0)});
    const q = valor => valor===null?'NULL':typeof valor==='number'?String(valor):"'"+String(valor).replaceAll('\\','\\\\').replaceAll("'","''")+"'";
    const sql=['SET NAMES utf8mb4;', 'USE `'+baseDatos+'`;', 'SET FOREIGN_KEY_CHECKS=0;', 'START TRANSACTION;', ...tablas.map(t=>'DELETE FROM `'+t+'`;')];
    for(const [tabla,filas] of Object.entries(datos)) {
        const columnas=Object.keys(filas[0]);
        for(let inicio=0;inicio<filas.length;inicio+=150)sql.push('INSERT INTO `'+tabla+'` ('+columnas.map(c=>'`'+c+'`').join(',')+') VALUES\n'+filas.slice(inicio,inicio+150).map(f=>'('+columnas.map(c=>q(f[c]??null)).join(',')+')').join(',\n')+';');
    }
    sql.push('COMMIT;','SET FOREIGN_KEY_CHECKS=1;');
    return { sql:sql.join('\n'), resumen:{fechaBase,horaBase,baseDatos,cuentasGestion:2,medicos:26,pacientes:180,tablas:Object.fromEntries(Object.entries(datos).map(([t,filas])=>[t,filas.length])),estados:Object.fromEntries(['ATENDIDA','NO_ASISTIO','CANCELADA','CONFIRMADA'].map(e=>[e,datos.cita.filter(c=>c.estado===e).length]))} };
}

if(require.main===module) {
    const [configuracion,salida]=process.argv.slice(2);
    if(!configuracion||!salida)throw Error('Uso: node generar-datos.cjs configuracion.json salida.sql');
    const resultado=generar(JSON.parse(fs.readFileSync(configuracion,'utf8')));
    fs.mkdirSync(path.dirname(salida),{recursive:true});
    fs.writeFileSync(salida,resultado.sql);
    fs.writeFileSync(salida+'.resumen.json',JSON.stringify(resultado.resumen,null,2));
    console.log(JSON.stringify(resultado.resumen));
}
module.exports={generar};
