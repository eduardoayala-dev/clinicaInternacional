import argparse
import csv
import hashlib
import json
import secrets
import subprocess
import sys
import time
import urllib.request
from datetime import datetime, timedelta, timezone
from comun import RAIZ, SALIDA, ENV, URL, compose, contenedor, docker, ejecutar, guardar_json


def sql(texto):
    contenedor("db")
    return ejecutar(compose("exec", "-T", "db", "sh", "-c",
                           'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root --default-character-set=utf8mb4 --database=clinica_db --batch --skip-column-names'), texto.encode("utf-8"))


def literal(valor):
    return "'" + str(valor).replace("'", "''") + "'"


def esperar_db():
    for _ in range(180):
        try:
            identificador = contenedor("db")
            if ejecutar([docker(), "inspect", "--format", "{{.State.Health.Status}}", identificador]) == "healthy":
                return
        except (RuntimeError, subprocess.TimeoutExpired):
            pass
        time.sleep(2)
    raise RuntimeError("MySQL de rendimiento no terminó de iniciar. Revisa target/rendimiento/preparacion.log.")


def datos_sql(fecha, cuentas, citas, sede, especialidad, clave_hash):
    sentencias = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    for indice in range(cuentas):
        medico = 10000 + indice
        correo = f"carga.medico.{indice + 1:03d}@example.test"
        sentencias.append("INSERT INTO usuario(id_usuario,nombres,apellidos,email,password_hash,rol,activo) VALUES(" +
                          ",".join([str(medico), literal("Médico de carga"), literal(str(indice + 1)), literal(correo), literal(clave_hash), "'MEDICO'", "1"]) + ");")
        sentencias.append(f"INSERT INTO medico(id_medico,id_usuario,cmp,id_especialidad,id_sede,estado) VALUES({medico},{medico},'PERF{indice + 1:05d}',{especialidad},{sede},'ACTIVO');")
        sentencias.append(f"INSERT INTO disponibilidad(id_disponibilidad,id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES({medico},{medico},{sede},{literal(fecha)},'00:00:00','12:00:00',15,'PUBLICADO');")
        for posicion in range(citas):
            paciente = 100000 + indice * 100 + posicion
            usuario = 200000 + indice * 100 + posicion
            cita = 300000 + indice * 100 + posicion
            inicio = datetime.combine(fecha, datetime.min.time()) + timedelta(minutes=posicion * 15)
            fin = inicio + timedelta(minutes=10)
            hora = literal(inicio.strftime("%Y-%m-%d %H:%M:%S"))
            cierre = literal(fin.strftime("%Y-%m-%d %H:%M:%S"))
            recomendacion = literal(fecha + timedelta(days=8)) if posicion == 0 else "NULL"
            sentencias.append(f"INSERT INTO usuario(id_usuario,nombres,apellidos,email,password_hash,rol,activo) VALUES({usuario},'Paciente sintético','Carga {indice + 1}-{posicion + 1}','carga.paciente.{indice}.{posicion}@example.test',{literal(clave_hash)},'PACIENTE',1);")
            sentencias.append(f"INSERT INTO paciente(id_paciente,id_usuario,dni,fecha_nacimiento,telefono,direccion) VALUES({paciente},{usuario},'{98000000 + indice * 100 + posicion}','2000-01-01','900000001','Datos sintéticos de rendimiento');")
            sentencias.append(f"INSERT INTO cita(id_cita,id_paciente,id_medico,id_disponibilidad,fecha_hora,estado,nivel_urgencia,fecha_recomendada_proxima) VALUES({cita},{paciente},{medico},{medico},{hora},'ATENDIDA','MEDIA',{recomendacion});")
            sentencias.append(f"INSERT INTO atencion_cita(id_cita,id_medico,id_paciente,fecha_inicio,fecha_fin) VALUES({cita},{medico},{paciente},{hora},{cierre});")
            sentencias.append(f"INSERT INTO historial_asistencia(id_paciente,id_cita,asistio,observacion,fecha_registro) VALUES({paciente},{cita},TRUE,'Indicaciones sintéticas de rendimiento',{cierre});")
            sentencias.append(f"INSERT INTO consulta_clinica(id_cita,motivo,antecedentes,hallazgos,evaluacion,diagnostico,plan) VALUES({cita},'Consulta sintética','','','Evaluación de prueba','Impresión de prueba','Plan de prueba');")
    sentencias.append(f"INSERT INTO rendimiento_fixture(id,version,fecha,cuentas,citas) VALUES(1,'v1',{literal(fecha)},{cuentas},{citas});")
    sentencias.append("COMMIT;")
    return "\n".join(sentencias)


def main():
    parser = argparse.ArgumentParser(description="Prepara exclusivamente el laboratorio local de rendimiento.")
    parser.add_argument("--cuentas", type=int, default=50, choices=range(1, 101), metavar="1..100")
    parser.add_argument("--citas", type=int, default=30, choices=range(1, 41), metavar="1..40")
    args = parser.parse_args()
    SALIDA.mkdir(parents=True, exist_ok=True)
    if not ENV.exists():
        ENV.write_text("PERF_DB_PASSWORD=" + secrets.token_hex(24) + "\nPERF_ROOT_PASSWORD=" + secrets.token_hex(24) + "\n", encoding="utf-8")
    print("Iniciando MySQL aislado; no se usa el volumen de la clínica habitual.", flush=True)
    with (SALIDA / "preparacion.log").open("wb") as log:
        subprocess.run(compose("up", "-d", "db"), cwd=RAIZ, stdout=log, stderr=subprocess.STDOUT, check=True)
        esperar_db()
        sql("CREATE TABLE IF NOT EXISTS rendimiento_fixture(id INT PRIMARY KEY,version VARCHAR(20) NOT NULL,fecha DATE NOT NULL,cuentas INT NOT NULL,citas INT NOT NULL)")
        existente = sql("SELECT fecha,cuentas,citas FROM rendimiento_fixture WHERE id=1")
        if existente:
            fecha_texto, cuentas, citas = existente.split("\t")
            if (int(cuentas), int(citas)) != (args.cuentas, args.citas):
                raise RuntimeError("El laboratorio ya tiene otro tamaño de datos. Se conserva sin cambios; usa sus mismos parámetros.")
            fecha = datetime.strptime(fecha_texto, "%Y-%m-%d").date()
        else:
            import re
            esquema = (RAIZ / "sql/schema.sql").read_text(encoding="utf-8")
            hash_prueba = re.search(r"\$2a\$10\$uY5I1bYkV5SnupobyT1meePKeOzUYs8qlsNZeZ0UWGRxgPGkNVVQa", esquema)
            if not hash_prueba:
                raise RuntimeError("No se encontró el hash sintético del esquema de demostración.")
            fecha = datetime.now(timezone(timedelta(hours=-5))).date() - timedelta(days=1)
            sede = int(sql("SELECT MIN(id_sede) FROM sede WHERE estado='ACTIVO'"))
            especialidad = int(sql("SELECT MIN(id_especialidad) FROM especialidad WHERE estado='ACTIVO'"))
            contenido = datos_sql(fecha, args.cuentas, args.citas, sede, especialidad, hash_prueba.group())
            (SALIDA / "fixture.sql").write_text(contenido, encoding="utf-8")
            sql(contenido)
        with (SALIDA / "usuarios.csv").open("w", encoding="utf-8", newline="") as archivo:
            escritor = csv.DictWriter(archivo, fieldnames=["baseUrl", "correo", "clave", "fecha", "idMedico", "idPaciente"])
            escritor.writeheader()
            for indice in range(args.cuentas):
                escritor.writerow(dict(baseUrl=URL, correo=f"carga.medico.{indice + 1:03d}@example.test", clave="Password123!", fecha=str(fecha), idMedico=10000 + indice, idPaciente=100000 + indice * 100))
        print("Datos sintéticos preparados. Iniciando aplicación en " + URL, flush=True)
        subprocess.run(compose("up", "-d", "--build", "app"), cwd=RAIZ, stdout=log, stderr=subprocess.STDOUT, check=True)
    listo = False
    for _ in range(90):
        try:
            with urllib.request.urlopen(URL + "/login/personal", timeout=5) as respuesta:
                if respuesta.status == 200 and 'name="csrf-token"' in respuesta.read().decode("utf-8"):
                    listo = True
                    break
        except (OSError, TimeoutError):
            pass
        time.sleep(2)
    if not listo:
        raise RuntimeError("La aplicación aún no respondió al calentamiento. Los contenedores se conservan para revisar los logs.")
    identificador = contenedor("app")
    imagen = ejecutar([docker(), "inspect", "--format", "{{.Image}}", identificador])
    sha = ejecutar(["git", "rev-parse", "HEAD"])
    guardar_json(SALIDA / "entorno.json", dict(baseUrl=URL, proyecto="clinica-rendimiento", commit=sha, imagen=imagen,
                 esquemaSha256=hashlib.sha256((RAIZ / "sql/schema.sql").read_bytes()).hexdigest(), fechaDatos=str(fecha),
                 cuentas=args.cuentas, citasPorMedico=args.citas, citasSinteticas=args.cuentas * args.citas,
                 recursos=dict(appCpu=1, appMemoriaMiB=512, javaHeapMiB=256, dbCpu=1, dbMemoriaMiB=768, poolJdbc=5)))
    print("Laboratorio listo. CSV: target/rendimiento/usuarios.csv. No se ha ejecutado carga todavía.", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    try:
        main()
    except (RuntimeError, subprocess.SubprocessError, OSError, ValueError) as error:
        print("No se completó la preparación: " + str(error), file=sys.stderr)
        sys.exit(1)
