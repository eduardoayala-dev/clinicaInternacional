import argparse
import csv
import json
import re
import sys
from pathlib import Path
from comun import SALIDA, guardar_json


def eventos(carpeta):
    datos = []
    for linea in (carpeta / "salida.log").read_text(encoding="utf-8", errors="replace").splitlines():
        try:
            valor = json.loads(linea)
            if isinstance(valor, dict):
                datos.append(valor)
        except json.JSONDecodeError:
            pass
    return datos


def memoria_mib(texto):
    match = re.match(r"\s*([\d.]+)\s*(GiB|MiB|KiB|GB|MB|kB|B)", texto)
    if not match:
        return None
    factores = {"GiB": 1024, "MiB": 1, "KiB": 1 / 1024, "GB": 10**9 / 2**20,
                "MB": 10**6 / 2**20, "kB": 1000 / 2**20, "B": 1 / 2**20}
    return float(match[1]) * factores[match[2]]


def recursos(carpeta):
    resumen = {nombre: dict(cpuMax=0.0, memoriaMaxMiB=0.0, muestras=0) for nombre in ["app", "db"]}
    fallos = 0
    with (carpeta / "recursos.csv").open(encoding="utf-8", newline="") as archivo:
        for fila in csv.DictReader(archivo):
            nombre = fila.get("contenedor", "")
            servicio = next((clave for clave in resumen if nombre.endswith("-" + clave + "-1")), None)
            if servicio is None:
                fallos += 1
                continue
            try:
                cpu = float(fila["cpu"].rstrip("%"))
                memoria = memoria_mib(fila["memoria"])
                if memoria is None:
                    raise ValueError("Memoria no reconocida")
                dato = resumen[servicio]
                dato["cpuMax"] = max(dato["cpuMax"], cpu)
                dato["memoriaMaxMiB"] = round(max(dato["memoriaMaxMiB"], memoria), 2)
                dato["muestras"] += 1
            except (ValueError, TypeError, KeyError):
                fallos += 1
    return resumen, fallos


def generar(carpeta):
    carpeta = Path(carpeta).resolve()
    if not carpeta.is_relative_to(SALIDA.resolve()):
        raise ValueError("El informe debe proceder de target/rendimiento.")
    config = json.loads((carpeta / "configuracion.json").read_text(encoding="utf-8"))
    ejecucion = json.loads((carpeta / "ejecucion.json").read_text(encoding="utf-8"))
    limites = config["objetivosPropuestos"]
    if config["perfil"] == "verificacion":
        run = json.loads((carpeta / "funcional.json").read_text(encoding="utf-8"))["run"]
        resumen = run["summary"]
        medicion = dict(peticiones=resumen["executedRequests"]["executed"], iteraciones=resumen["iterations"]["executed"],
                        asercionesFallidas=resumen["tests"]["failed"], erroresRuntime=resumen["executedRequests"]["errors"],
                        p95Ms=None, p99Ms=None, rps=None, duracionPostmanSegundos=None, picoUsuariosObservado=None)
        valido = medicion["peticiones"] == 8 and medicion["asercionesFallidas"] == 0 and medicion["erroresRuntime"] == 0 and not run.get("runError")
    else:
        datos = eventos(carpeta)
        finales = [dato for dato in datos if dato.get("event") == "summary"]
        if not finales:
            raise ValueError("La corrida no produjo un resumen final; no se puede declarar exitosa.")
        ultimo = finales[-1]
        metricas = ultimo["metrics"]
        tiempo = metricas["responseTime"]
        progreso = [dato for dato in datos if dato.get("event") == "progress"]
        pico = max((dato["currentVUs"] for dato in progreso), default=0)
        medicion = dict(peticiones=ultimo["totalRequests"], iteraciones=None,
                        asercionesFallidas=metricas["testFailures"]["requestsWithFailedAssertions"],
                        erroresRuntime=metricas["errors"]["percentage"],
                        p95Ms=tiempo["percentile95"], p99Ms=tiempo["percentile99"], rps=metricas["requestThroughput"]["rate"],
                        duracionPostmanSegundos=ultimo["durationSeconds"], picoUsuariosObservado=pico)
        perfil = config["parametros"]
        completo = pico >= perfil["usuarios"] and ultimo["durationSeconds"] >= perfil["minutos"] * 60 * .9
        valido = completo and medicion["peticiones"] > 0 and medicion["asercionesFallidas"] == 0 and medicion["p95Ms"] < limites["p95Milisegundos"] and medicion["erroresRuntime"] < limites["errorRuntimePorcentaje"]
        with (carpeta / "series.csv").open("w", encoding="utf-8", newline="") as archivo:
            escritor = csv.writer(archivo)
            escritor.writerow(["segundos", "usuarios_virtuales", "rps", "respuesta_media_ms", "errores_runtime_porcentaje", "peticiones_acumuladas"])
            for dato in progreso:
                escritor.writerow([dato["elapsedSec"], dato["currentVUs"], dato["rps"], dato["avgResponseTime"], dato["errorPct"], dato["totalRequests"]])
    uso, fallos = recursos(carpeta)
    valido = valido and ejecucion["codigoSalida"] == 0 and all(dato["muestras"] > 0 for dato in uso.values())
    resultado = dict(perfil=config["perfil"], objetivosCumplidos=valido, capacidadMaximaDeterminada=False,
                     medicion=medicion, recursosMuestreados=uso, fallosMonitoreo=fallos,
                     duracionTotalGeneradorSegundos=ejecucion["segundos"], entorno=config["entorno"])
    guardar_json(carpeta / "resumen.json", resultado)
    etiqueta_fallos = "Aserciones fallidas" if config["perfil"] == "verificacion" else "Solicitudes con aserciones fallidas"
    texto = ["# Resultado local: " + config["perfil"], "", "Escenario: acceso y consulta médica con login real en cada iteración.", "",
             "| Métrica | Resultado |", "| --- | --- |", "| Peticiones | " + str(medicion["peticiones"]) + " |",
             "| " + etiqueta_fallos + " | " + str(medicion["asercionesFallidas"]) + " |"]
    if config["perfil"] != "verificacion":
        texto.extend(["| Usuarios virtuales máximos observados | " + str(medicion["picoUsuariosObservado"]) + " |",
                      "| Duración configurada | " + str(config["parametros"]["minutos"] * 60) + " s |",
                      "| Duración informada por Postman | " + str(medicion["duracionPostmanSegundos"]) + " s |",
                      "| p95 global | " + str(medicion["p95Ms"]) + " ms |", "| p99 global | " + str(medicion["p99Ms"]) + " ms |",
                      "| Peticiones por segundo | " + str(medicion["rps"]) + " |", "| Errores runtime | " + str(medicion["erroresRuntime"]) + " % |"])
    for nombre, dato in uso.items():
        texto.extend(["| CPU máxima muestreada " + nombre + " | " + str(dato["cpuMax"]) + " % |",
                      "| Memoria máxima muestreada " + nombre + " | " + str(dato["memoriaMaxMiB"]) + " MiB |"])
    texto.extend(["", "Resultado: **" + ("cumple los objetivos propuestos de esta corrida" if valido else "no cumple o requiere revisar errores/instrumentación") + "**.", "",
                  "Esto no determina la capacidad máxima de la clínica ni de Railway. Las métricas son del escenario y entorno indicados; incluyen el coste del login. La duración de Postman puede incluir el cierre de peticiones pendientes, además del minuto configurado.", "",
                  "CPU y memoria son muestras de contenedores con intervalo objetivo de cinco segundos, sujeto a la demora de lectura; no son máximos continuos ni medidas del heap Java. El generador comparte máquina con Docker. El CLI utilizado proporciona métricas agregadas y una serie de respuesta media, no p95 por petición. Revisar el desglose y las gráficas en Postman de escritorio para la entrega al profesor.", "",
                  "Datos: " + str(config["entorno"]["cuentas"]) + " médicos sintéticos y " + str(config["entorno"]["citasSinteticas"]) + " consultas. Commit de la app: `" + config["entorno"]["commit"] + "`. Más detalles en configuracion.json."])
    (carpeta / "informe.md").write_text("\n".join(texto) + "\n", encoding="utf-8")
    return resultado


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="Genera un informe agregado sin cookies ni contraseñas.")
    parser.add_argument("carpeta", type=Path)
    args = parser.parse_args()
    try:
        resultado = generar(args.carpeta)
        print("Informe creado. Objetivos cumplidos: " + str(resultado["objetivosCumplidos"]))
        sys.exit(0 if resultado["objetivosCumplidos"] else 1)
    except (OSError, ValueError, KeyError) as error:
        print("No se generó el informe: " + str(error), file=sys.stderr)
        sys.exit(1)
