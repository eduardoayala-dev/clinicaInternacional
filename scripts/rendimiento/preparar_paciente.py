"""Agrega al laboratorio local agendas futuras para el flujo del paciente.

Escenario: un paciente inicia sesión y consulta la disponibilidad semanal de
una especialidad (GET /api/horarios?vista=semana). Requiere haber ejecutado
antes scripts/rendimiento/preparar.py. Solo escribe en el MySQL aislado del
proyecto clinica-rendimiento; nunca en la base habitual ni en Railway.
"""
import argparse
import csv
import sys
from datetime import datetime, timedelta, timezone

from comun import SALIDA, URL
from preparar import esperar_db, literal, sql

BASE_DISPONIBILIDAD = 500000
BASE_CITA = 600000


def datos_sql(desde, dias, medicos, sede, ocupadas):
    sentencias = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    for indice in range(medicos):
        medico = 10000 + indice
        for dia in range(dias):
            fecha = desde + timedelta(days=dia)
            for turno, (inicio, fin) in enumerate((("08:00:00", "12:00:00"), ("14:00:00", "18:00:00"))):
                disponibilidad = BASE_DISPONIBILIDAD + (indice * dias + dia) * 2 + turno
                sentencias.append("INSERT INTO disponibilidad(id_disponibilidad,id_medico,id_sede,fecha,hora_inicio,hora_fin,duracion_minutos,estado) VALUES("
                                  f"{disponibilidad},{medico},{sede},{literal(fecha)},'{inicio}','{fin}',20,'PUBLICADO');")
                hora_base = datetime.combine(fecha, datetime.strptime(inicio, "%H:%M:%S").time())
                for posicion in range(ocupadas):
                    cita = BASE_CITA + (disponibilidad - BASE_DISPONIBILIDAD) * 10 + posicion
                    paciente = 100000 + indice * 100 + (posicion + dia) % 30
                    hora = literal((hora_base + timedelta(minutes=20 * posicion * 2)).strftime("%Y-%m-%d %H:%M:%S"))
                    sentencias.append("INSERT INTO cita(id_cita,id_paciente,id_medico,id_disponibilidad,fecha_hora,estado,nivel_urgencia) VALUES("
                                      f"{cita},{paciente},{medico},{disponibilidad},{hora},'CONFIRMADA','BAJA');")
    sentencias.append(f"INSERT INTO rendimiento_fixture(id,version,fecha,cuentas,citas) VALUES(2,'paciente',{literal(desde)},{medicos},{dias});")
    sentencias.append("COMMIT;")
    return "\n".join(sentencias)


def main():
    parser = argparse.ArgumentParser(description="Agrega agendas futuras al laboratorio local de rendimiento.")
    parser.add_argument("--dias", type=int, default=7, choices=range(1, 15), metavar="1..14")
    parser.add_argument("--ocupadas", type=int, default=3, choices=range(0, 7), metavar="0..6",
                        help="citas confirmadas por turno, para que el cálculo de cupos libres tenga ocupaciones")
    args = parser.parse_args()
    esperar_db()
    base = sql("SELECT fecha,cuentas FROM rendimiento_fixture WHERE id=1")
    if not base:
        raise RuntimeError("Primero ejecuta scripts/rendimiento/preparar.py.")
    medicos = int(base.split("\t")[1])
    existente = sql("SELECT fecha FROM rendimiento_fixture WHERE id=2")
    if existente:
        desde = datetime.strptime(existente, "%Y-%m-%d").date()
        print("Las agendas del paciente ya existían desde " + existente + "; se conservan.", flush=True)
    else:
        desde = datetime.now(timezone(timedelta(hours=-5))).date() + timedelta(days=1)
        sede = int(sql("SELECT MIN(id_sede) FROM sede WHERE estado='ACTIVO'"))
        sql(datos_sql(desde, args.dias, medicos, sede, args.ocupadas))
        print(f"Agendas creadas: {medicos} médicos x {args.dias} días x 2 turnos, desde {desde}.", flush=True)
    especialidad = int(sql("SELECT MIN(id_especialidad) FROM especialidad WHERE estado='ACTIVO'"))
    with (SALIDA / "pacientes.csv").open("w", encoding="utf-8", newline="") as archivo:
        escritor = csv.DictWriter(archivo, fieldnames=["baseUrl", "dni", "clave", "idEspecialidad", "desde"])
        escritor.writeheader()
        for indice in range(medicos):
            escritor.writerow(dict(baseUrl=URL, dni=str(98000000 + indice * 100), clave="Password123!",
                                   idEspecialidad=especialidad, desde=str(desde)))
    print("CSV listo: target/rendimiento/pacientes.csv. No se ha ejecutado carga todavía.", flush=True)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    try:
        main()
    except (RuntimeError, OSError, ValueError) as error:
        print("No se completó la preparación del paciente: " + str(error), file=sys.stderr)
        sys.exit(1)
