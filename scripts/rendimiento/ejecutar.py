import argparse
import csv
import hashlib
import json
import os
import shutil
import signal
import subprocess
import sys
import threading
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from comun import RAIZ, SALIDA, URL, contenedor, docker, ejecutar, guardar_json

COLECCION = RAIZ / "docs/postman/Rendimiento-Medico-Local.postman_collection.json"


def cli_postman(ruta):
    resuelta = shutil.which(ruta) or ruta
    archivo = Path(resuelta).resolve()
    if not archivo.is_file():
        raise RuntimeError("No se encontró Postman CLI. Indica --postman con la ruta de su ejecutable.")
    if archivo.suffix.lower() == ".cmd":
        for paquete in [archivo.parent.parent / "postman-cli", archivo.parent / "node_modules/postman-cli"]:
            configuracion = paquete / "package.json"
            if configuracion.exists():
                binarios = json.loads(configuracion.read_text(encoding="utf-8"))["bin"]
                destino = (paquete / (binarios["postman"] if isinstance(binarios, dict) else binarios)).resolve()
                nodo = shutil.which("node")
                if nodo and destino.is_file() and destino.is_relative_to(paquete.resolve()):
                    return [nodo, str(destino)]
        raise RuntimeError("No se resolvió el lanzador npm de Postman. Usa el ejecutable oficial de Postman CLI.")
    return [str(archivo)]


def vigilar(terminar, identificadores, destino):
    with destino.open("w", encoding="utf-8", newline="") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(["instante", "contenedor", "cpu", "memoria", "memoria_porcentaje", "red", "disco"])
        while not terminar.is_set():
            inicio_muestra = time.monotonic()
            try:
                texto = ejecutar([docker(), "stats", "--no-stream", "--format", "{{json .}}", *identificadores], timeout=15)
                instante = datetime.now(timezone(timedelta(hours=-5))).isoformat()
                for linea in texto.splitlines():
                    datos = json.loads(linea)
                    escritor.writerow([instante, datos.get("Name"), datos.get("CPUPerc"), datos.get("MemUsage"), datos.get("MemPerc"), datos.get("NetIO"), datos.get("BlockIO")])
                archivo.flush()
            except (RuntimeError, OSError, subprocess.TimeoutExpired, json.JSONDecodeError) as error:
                escritor.writerow([datetime.now().isoformat(), "MONITOREO_NO_DISPONIBLE", type(error).__name__])
                archivo.flush()
            terminar.wait(max(0, 5 - (time.monotonic() - inicio_muestra)))


def validar_entorno():
    metadata = json.loads((SALIDA / "entorno.json").read_text(encoding="utf-8"))
    if metadata.get("baseUrl") != URL or metadata.get("proyecto") != "clinica-rendimiento":
        raise RuntimeError("Los metadatos no corresponden al laboratorio local aislado.")
    identificadores = [contenedor("app"), contenedor("db")]
    with (SALIDA / "usuarios.csv").open(encoding="utf-8", newline="") as archivo:
        filas = list(csv.DictReader(archivo))
    if not filas or any(fila.get("baseUrl") != URL or not fila.get("correo", "").startswith("carga.medico.") for fila in filas):
        raise RuntimeError("El CSV no corresponde a las cuentas sintéticas locales.")
    if (RAIZ / ".postman/resources.yaml").exists():
        raise RuntimeError("Este procedimiento local no ejecuta colecciones vinculadas a recursos cloud.")
    return metadata, identificadores


def detener(proceso):
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(proceso.pid), "/T", "/F"], capture_output=True, timeout=15)
    else:
        os.killpg(proceso.pid, signal.SIGTERM)
    try:
        proceso.wait(timeout=10)
    except subprocess.TimeoutExpired:
        if os.name == "nt":
            proceso.kill()
        else:
            os.killpg(proceso.pid, signal.SIGKILL)
        proceso.wait()


def main():
    perfiles = json.loads((RAIZ / "docs/rendimiento/perfiles.json").read_text(encoding="utf-8"))
    parser = argparse.ArgumentParser(description="Ejecuta únicamente el laboratorio local 8083.")
    parser.add_argument("--perfil", choices=["verificacion", *perfiles], default="humo")
    parser.add_argument("--postman")
    parser.add_argument("--solo-comando", action="store_true")
    args = parser.parse_args()
    herramientas = SALIDA / "herramientas.json"
    configuracion = json.loads(herramientas.read_text(encoding="utf-8")) if herramientas.exists() else {}
    postman = cli_postman(args.postman or configuracion.get("postman", "postman"))
    metadata, identificadores = validar_entorno()
    datos = SALIDA / "usuarios.csv"
    perfil = perfiles.get(args.perfil)
    if perfil:
        comando = [*postman, "performance", "run", str(COLECCION), "--runner", "local", "--data-file", str(datos),
                   "--peak-vu", str(perfil["usuarios"]), "--duration", str(perfil["minutos"]), "--load-profile", perfil["perfil"],
                   "--pass-if", "less_than(p95, 2000)", "--pass-if", "less_than(error_rate, 1)", "--output", "ndjson", "--disable-unicode"]
        if "base" in perfil:
            comando.extend(["--base-vu", str(perfil["base"]), "--load-segments", perfil["segmentos"]])
        timeout = perfil["minutos"] * 60 + 180
    else:
        comando = [*postman, "collection", "run", str(COLECCION), "--iteration-count", "1", "--iteration-data", str(datos),
                   "--no-report-events", "--bail", "--timeout-request", "60000", "--reporters", "cli,json"]
        timeout = 180
    if args.solo_comando:
        print(subprocess.list2cmdline(comando))
        return
    instante = datetime.now(timezone(timedelta(hours=-5))).strftime("%Y%m%d-%H%M%S")
    carpeta = SALIDA / (instante + "-" + args.perfil)
    carpeta.mkdir()
    if not perfil:
        comando.extend(["--reporter-json-export", str(carpeta / "funcional.json")])
    guardar_json(carpeta / "configuracion.json", dict(escenario="Acceso y consulta médica con login en cada iteración", perfil=args.perfil,
                 parametros=perfil, entorno=metadata, postmanVersion=ejecutar([*postman, "--version"]),
                 coleccionSha256=hashlib.sha256(COLECCION.read_bytes()).hexdigest(),
                 objetivosPropuestos=dict(p95Milisegundos=2000, errorRuntimePorcentaje=1), duracionMaximaSegundos=timeout))
    terminar = threading.Event()
    monitor = threading.Thread(target=vigilar, args=(terminar, identificadores, carpeta / "recursos.csv"), daemon=True)
    monitor.start()
    inicio = time.monotonic()
    print("Ejecutando " + args.perfil + ". Resultados locales en " + str(carpeta), flush=True)
    try:
        with (carpeta / "salida.log").open("wb") as salida:
            grupo = {"creationflags": subprocess.CREATE_NEW_PROCESS_GROUP} if os.name == "nt" else {"start_new_session": True}
            proceso = subprocess.Popen(comando, cwd=RAIZ, stdout=salida, stderr=subprocess.STDOUT, **grupo)
            try:
                codigo = proceso.wait(timeout=timeout)
            except (subprocess.TimeoutExpired, KeyboardInterrupt):
                detener(proceso)
                raise RuntimeError("La ejecución se detuvo; se conservan resultados parciales. No se aumentó la carga.")
    finally:
        terminar.set()
        monitor.join(timeout=20)
    guardar_json(carpeta / "ejecucion.json", dict(codigoSalida=codigo, segundos=round(time.monotonic() - inicio, 2),
                 perfil=args.perfil, capacidadMaximaDeterminado=False))
    from resumir import generar
    resumen = generar(carpeta)
    if not resumen["objetivosCumplidos"]:
        codigo = 1
    print("Código de salida: " + str(codigo) + ". Revisar aserciones y métricas antes de subir la carga.", flush=True)
    sys.exit(codigo)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        print("No se completó la prueba: " + str(error), file=sys.stderr)
        sys.exit(1)
