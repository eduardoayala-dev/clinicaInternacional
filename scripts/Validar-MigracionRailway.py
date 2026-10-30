import argparse
import hashlib
import json
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--estructura', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = root / 'target' / 'validacion-migracion-railway'
    output.mkdir(parents=True, exist_ok=True)
    pattern = r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?(\w+)`?\s*\((.*?)\)\s*ENGINE\s*=\s*InnoDB[^;]*;'

    def tables(source):
        return {m.group(1): m.group(0) for m in re.finditer(pattern, source, re.S | re.I)}

    original = tables(args.estructura.read_text(encoding='utf-8-sig'))
    expected = tables((root / 'sql/schema.sql').read_text(encoding='utf-8-sig'))
    if len(original) != 18 or not set(original).issubset(expected):
        raise RuntimeError('La estructura de origen no corresponde al ensayo de 18 tablas o faltan tablas en el modelo actual.')
    migration = (root / 'sql/maintenance/Actualizar-Railway.sql').read_text(encoding='utf-8-sig')
    name = 'clinica-migracion-' + datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S')

    def docker(*commands, sql=None, check=True):
        result = subprocess.run(['docker', *commands], input=sql.encode('utf-8') if sql is not None else None,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=180)
        if check and result.returncode:
            raise RuntimeError(result.stderr.decode('utf-8', errors='replace'))
        return result

    def mysql(sql, database='railway', check=True):
        return docker('exec', '-i', name, 'mysql', '-uroot', '--default-character-set=utf8mb4',
                      '--batch', '--raw', '--skip-column-names', database, sql=sql, check=check)

    def text(result):
        return result.stdout.decode('utf-8').strip()

    created = False
    try:
        print('Creando MySQL 8.4 aislado, sin red ni puertos publicados...', flush=True)
        docker('run', '-d', '--name', name, '--network', 'none', '--memory', '768m',
               '--label', 'clinica.ensayo=migracion-railway', '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
               'mysql:8.4', '--innodb-buffer-pool-size=128M')
        created = True
        for attempt in range(180):
            logs = docker('logs', name, check=False)
            if b'MySQL init process done' in logs.stdout + logs.stderr:
                ready = docker('exec', name, 'mysqladmin', '-uroot', 'ping', '--silent', check=False)
                if ready.returncode == 0:
                    break
            if attempt % 15 == 0:
                print('Esperando inicialización de MySQL...', flush=True)
            time.sleep(2)
        else:
            raise RuntimeError('MySQL no quedó listo dentro del plazo.')
        mysql('CREATE DATABASE railway CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;'
              'CREATE DATABASE referencia CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;'
              'CREATE DATABASE restauracion CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;', database='mysql')
        mysql('SET FOREIGN_KEY_CHECKS=0;\n' + '\n'.join(original.values()) + '\nSET FOREIGN_KEY_CHECKS=1;')
        mysql('SET FOREIGN_KEY_CHECKS=0;\n' + '\n'.join(expected.values()) +
              '\nSET FOREIGN_KEY_CHECKS=1;\nCREATE INDEX idx_cita_medico_paciente_fecha ON cita(id_medico,id_paciente,fecha_hora);',
              database='referencia')
        mysql("""
SET NAMES utf8mb4;
INSERT INTO usuario(id_usuario,nombres,apellidos,email,password_hash,rol) VALUES
(1,'José 🩺','Muñoz','paciente@example.test','hash-sintetico','PACIENTE'),
(2,'Médico','Peña','medico@example.test','hash-sintetico','MEDICO'),
(3,'Admisión','Núñez','personal@example.test','hash-sintetico','PERSONAL_ADMINISTRATIVO');
INSERT INTO paciente(id_paciente,id_usuario,dni,fecha_nacimiento,telefono,direccion)
VALUES(1,1,'99000001','1990-01-01','900000001','Dirección de prueba');
INSERT INTO sede(id_sede,codigo,nombre,direccion,telefono) VALUES(1,'PRUEBA','Sede de prueba','Dirección','900000002');
INSERT INTO especialidad(id_especialidad,nombre) VALUES(1,'Especialidad de prueba');
INSERT INTO medico(id_medico,id_usuario,cmp,id_especialidad,id_sede) VALUES(1,2,'CMP-PRUEBA',1,1);
INSERT INTO personal_administrativo(id_personal,id_usuario,id_sede,codigo_empleado,cargo)
VALUES(1,3,1,'PRUEBA','Admisión');
INSERT INTO administrador(id_administrador,id_usuario) VALUES(1,3);
INSERT INTO motivo_cita(id_motivo,id_especialidad,descripcion) VALUES(1,1,'Revisión de prueba');
INSERT INTO disponibilidad(id_disponibilidad,id_medico,id_sede,fecha,hora_inicio,hora_fin)
VALUES(1,1,1,'2026-10-29','08:00','18:00');
INSERT INTO cita(id_cita,id_paciente,id_medico,id_disponibilidad,id_motivo,fecha_hora,estado) VALUES
(1,1,1,1,1,'2026-10-29 08:00','SOLICITADA'),(2,1,1,1,1,'2026-10-29 09:00','CONFIRMADA'),
(3,1,1,1,1,'2026-10-29 10:00','EN_SALA'),(4,1,1,1,1,'2026-10-29 11:00','ATENDIDA'),
(5,1,1,1,1,'2026-10-29 12:00','NO_ASISTIO'),(6,1,1,1,1,'2026-10-29 13:00','REPROGRAMADA'),
(7,1,1,1,1,'2026-10-29 14:00','CANCELADA');
INSERT INTO lista_espera(id_lista_espera,id_paciente,id_especialidad,id_sede,canal_notificacion)
VALUES(1,1,1,1,'WHATSAPP');
INSERT INTO reasignacion_inteligente(id_reasignacion,id_cita_original,id_lista_espera,id_disponibilidad,fecha_limite_respuesta)
VALUES(1,7,1,1,'2026-10-29 15:00');
INSERT INTO notificacion(id_notificacion,id_usuario,id_cita,tipo,mensaje)
VALUES(1,1,1,'PRUEBA','Notificación: revisión, niño y Peña 🩺');
INSERT INTO historial_asistencia(id_historial,id_paciente,id_cita,asistio) VALUES(1,1,4,1);
INSERT INTO priorizacion_config(id_config,nombre,criterio) VALUES(1,'Prueba','Antigüedad');
INSERT INTO indicador(id_indicador,nombre,descripcion,tipo,valor,periodo) VALUES(1,'Prueba','Prueba','PRUEBA',1,'PRUEBA');
INSERT INTO reporte(id_reporte,nombre,tipo,fecha_inicio,fecha_fin,id_usuario)
VALUES(1,'Prueba','PRUEBA','2026-10-29','2026-10-29',3);
INSERT INTO auditoria_seguridad(id_auditoria,id_usuario,ip_origen,evento) VALUES(1,1,'127.0.0.1','PRUEBA');
""")
        columns = {}
        for table in original:
            columns[table] = text(mysql(f"SELECT column_name FROM information_schema.columns WHERE table_schema='railway' AND table_name='{table}' ORDER BY ordinal_position")).splitlines()

        def snapshot(database):
            return {table: text(mysql('SELECT JSON_ARRAY(' + ','.join(f'`{col}`' for col in cols) +
                                      f') FROM `{table}` ORDER BY 1;', database=database))
                    for table, cols in columns.items()}

        before = snapshot('railway')
        backup = docker('exec', name, 'mysqldump', '-uroot', '--single-transaction', '--no-tablespaces',
                        '--set-gtid-purged=OFF', 'railway').stdout.decode('utf-8')
        mysql(backup, database='restauracion')
        assert snapshot('restauracion') == before, 'La restauración del fixture cambió datos.'
        print('Restauración del respaldo sintético comprobada. Aplicando actualización dos veces...', flush=True)
        for _ in range(2):
            mysql(migration)
            assert snapshot('railway') == before, 'Se modificaron datos de columnas originales.'

        queries = [
            "SELECT table_name,column_name,ordinal_position,column_type,is_nullable,COALESCE(column_default,'<NULL>'),extra,generation_expression,COALESCE(collation_name,'') FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position",
            "SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE table_schema=DATABASE() ORDER BY table_name,constraint_name",
            "SELECT table_name,index_name,non_unique,seq_in_index,column_name,index_type FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index",
            "SELECT table_name,constraint_name,referenced_table_name,update_rule,delete_rule FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE() ORDER BY table_name,constraint_name",
            "SELECT constraint_name,check_clause FROM information_schema.check_constraints WHERE constraint_schema=DATABASE() ORDER BY constraint_name",
            "SELECT table_name,column_name,constraint_name,ordinal_position,COALESCE(referenced_table_name,''),COALESCE(referenced_column_name,'') FROM information_schema.key_column_usage WHERE constraint_schema=DATABASE() ORDER BY table_name,constraint_name,ordinal_position",
        ]
        legacy_indexes = []
        for i, query in enumerate(queries):
            actual = text(mysql(query))
            target = text(mysql(query, database='referencia'))
            (output / f'metadata-{i}-actual.tsv').write_text(actual, encoding='utf-8')
            (output / f'metadata-{i}-referencia.tsv').write_text(target, encoding='utf-8')
            if i == 2:
                legacy_indexes = sorted(set(actual.splitlines()) - set(target.splitlines()))
                assert not set(target.splitlines()) - set(actual.splitlines()), 'Faltan índices esperados.'
                assert set(legacy_indexes) <= {'cita\tfk_cita_medico\t1\t1\tid_medico\tBTREE'}, 'Índices adicionales sin revisar.'
            else:
                assert actual == target, f'Diferencias de metadatos, consulta {i}; revisar TSV.'

        rejected = [
            "INSERT INTO admision_cita VALUES(999999,3,NOW(),10,0)",
            "INSERT INTO atencion_cita(id_cita,id_medico,id_paciente) VALUES(1,1,1)",
            "START TRANSACTION; INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio) VALUES(1,1,1,NOW()); INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio) VALUES(2,1,1,NOW());",
        ]
        for sql in rejected:
            assert mysql(sql, check=False).returncode != 0, 'Una restricción dejó pasar datos inválidos.'
        mysql("UPDATE cita SET estado='EN_ATENCION' WHERE id_cita=1; UPDATE cita SET estado='NO_ATENDIDA' WHERE id_cita=2;")
        assert text(mysql("SELECT anticipacion_minutos FROM lista_espera WHERE id_lista_espera=1")) == '180'
        report = {'fechaUTC': datetime.now(timezone.utc).isoformat(), 'mysql': text(mysql('SELECT VERSION()')),
                  'tablasAntes': len(original), 'tablasDespues': len(expected), 'aplicaciones': 2,
                  'restauracionFixture': True, 'datosOriginalesConservados': True,
                  'codificacionConservada': True, 'comparacionesMetadatos': len(queries),
                  'restriccionesNegativas': len(rejected), 'estadosNuevosAceptados': True,
                  'indicesHeredadosConservados': legacy_indexes,
                  'sha256Script': hashlib.sha256(migration.encode()).hexdigest(),
                  'backupRealRailwayRestaurado': False, 'railwayModificado': False}
        (output / 'resultado.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
        print(json.dumps(report, ensure_ascii=False), flush=True)
    finally:
        if created:
            docker('rm', '-f', '-v', name)


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')
    main()
