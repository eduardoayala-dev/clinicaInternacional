"""Despliega en el laboratorio local 8083 la versión ANTES o DESPUÉS de la mejora.

antes   = be0f568 (calendario semanal con ~3 consultas por médico y día)
despues = ef52581 (perf(paciente): agrupar consultas del calendario semanal, 5 consultas)

Entre ambos commits solo cambia MedicoDAO.java (y su prueba), así que la
diferencia medida en la petición "04 Consultar disponibilidad semanal" se
atribuye a esa mejora. Usa la misma base de datos, los mismos datos y los
mismos límites de CPU/memoria. No toca la base habitual ni Railway.
"""
import json
import sys
import time
import urllib.request
from datetime import datetime

from comun import RAIZ, SALIDA, URL, compose, docker, ejecutar

VERSIONES = {
    "antes": "be0f5681b6443dadab2515d258e39ef8733be6a1",
    "despues": "ef5258119bdf99f95b9d6f1534472b54ce3c883b",
}


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in VERSIONES:
        raise RuntimeError("Uso: python scripts/rendimiento/cambiar_version.py antes|despues")
    nombre = sys.argv[1]
    sha = ejecutar(["git", "rev-parse", VERSIONES[nombre]])
    carpeta = SALIDA / ("version-" + nombre)
    if not carpeta.exists():
        print("Preparando copia del código " + sha[:7] + " (" + nombre + ")...", flush=True)
        ejecutar(["git", "worktree", "add", "--detach", str(carpeta), sha], timeout=300)
    print("Construyendo la imagen (la primera vez tarda varios minutos)...", flush=True)
    ejecutar([docker(), "build", "-t", "clinica-rendimiento:local", str(carpeta)], timeout=1800)
    ejecutar(compose("up", "-d", "--no-build", "--force-recreate", "app"), timeout=300)
    for _ in range(90):
        try:
            with urllib.request.urlopen(URL + "/login/paciente", timeout=5) as respuesta:
                if respuesta.status == 200:
                    break
        except (OSError, TimeoutError):
            pass
        time.sleep(2)
    else:
        raise RuntimeError("La aplicación no respondió. Revisa: docker compose --project-name clinica-rendimiento logs app")
    # Calentamiento: compila JSP y abre el pool antes de medir; no forma parte de la medición.
    for ruta in ("/login/paciente", "/login/personal"):
        urllib.request.urlopen(URL + ruta, timeout=30).read()
    (SALIDA / "version-activa.json").write_text(json.dumps(
        {"version": nombre, "commit": sha, "desplegado": datetime.now().isoformat(timespec="seconds")},
        ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Listo: versión {nombre.upper()} ({sha[:7]}) en {URL}. Ya puedes correr Postman.", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    try:
        main()
    except (RuntimeError, OSError) as error:
        print("No se cambió la versión: " + str(error), file=sys.stderr)
        sys.exit(1)
