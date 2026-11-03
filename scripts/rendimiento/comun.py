import json
import shutil
import subprocess
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SALIDA = RAIZ / "target" / "rendimiento"
PROYECTO = "clinica-rendimiento"
URL = "http://127.0.0.1:8083"
ENV = RAIZ / ".env.rendimiento"
COMPOSE = RAIZ / "compose.rendimiento.yml"


def docker():
    ejecutable = shutil.which("docker")
    if not ejecutable:
        raise RuntimeError("No se encontró Docker. Abre Docker Desktop y verifica su instalación.")
    return ejecutable


def ejecutar(args, entrada=None, timeout=120):
    resultado = subprocess.run(args, input=entrada, capture_output=True, cwd=RAIZ, timeout=timeout)
    if resultado.returncode:
        raise RuntimeError(resultado.stderr.decode("utf-8", errors="replace")[-1500:])
    return resultado.stdout.decode("utf-8", errors="replace").strip()


def compose(*args):
    return [docker(), "compose", "--project-name", PROYECTO, "--env-file", str(ENV), "-f", str(COMPOSE), *args]


def contenedor(servicio):
    identificador = ejecutar(compose("ps", "-q", servicio))
    if not identificador:
        raise RuntimeError("No está iniciado el servicio de pruebas: " + servicio)
    proyecto = ejecutar([docker(), "inspect", "--format", '{{index .Config.Labels "com.docker.compose.project"}}', identificador])
    if proyecto != PROYECTO:
        raise RuntimeError("El contenedor no pertenece al entorno aislado de rendimiento.")
    return identificador


def guardar_json(ruta, datos):
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(json.dumps(datos, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
