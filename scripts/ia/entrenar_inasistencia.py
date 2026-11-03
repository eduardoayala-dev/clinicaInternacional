import argparse
import csv
import hashlib
import io
import json
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import sklearn
from sklearn.dummy import DummyClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score, brier_score_loss, confusion_matrix,
    f1_score, precision_score, recall_score, roc_auc_score,
)
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

SEMILLA = 20261030
VARIABLES = ["inasistencias_previas", "atenciones_previas", "anticipacion_dias", "confirmada_por_paciente"]
RAIZ = Path(__file__).resolve().parents[2]


def generar_datos():
    rng = np.random.default_rng(SEMILLA)
    filas = []
    for paciente in range(600):
        particion = "entrenamiento" if paciente < 420 else "validacion" if paciente < 510 else "prueba"
        anio = {"entrenamiento": 2024, "validacion": 2025, "prueba": 2026}[particion]
        historial = []
        factor_no_observado = rng.normal(0, 0.6)
        for consulta in range(12):
            anticipacion = int(rng.integers(0, 46))
            fecha_cita = date(anio, 1, 20) + timedelta(days=consulta * 21)
            fecha_reserva = fecha_cita - timedelta(days=anticipacion)
            conocidos = [resultado for fecha, resultado in historial if fecha < fecha_reserva]
            faltas = sum(conocidos)
            atenciones = len(conocidos) - faltas
            confirmada = int(rng.random() < 0.7)
            x = [faltas, atenciones, anticipacion, confirmada]
            logit = -1.4 + 0.45 * faltas - 0.06 * atenciones + 0.035 * anticipacion - 1.1 * confirmada + factor_no_observado
            inasistencia = int(rng.random() < 1 / (1 + np.exp(-logit)))
            filas.append({"paciente_sintetico": paciente, "particion": particion,
                          "fecha_cita": fecha_cita.isoformat(), "fecha_reserva": fecha_reserva.isoformat(),
                          **dict(zip(VARIABLES, x)), "no_asistio": inasistencia})
            historial.append((fecha_cita, inasistencia))
    return filas


def matrices(filas, particion):
    seleccion = [f for f in filas if f["particion"] == particion]
    return np.array([[f[v] for v in VARIABLES] for f in seleccion], dtype=float), np.array([f["no_asistio"] for f in seleccion])


def comprobar_separacion(filas):
    grupos = {p: {f["paciente_sintetico"] for f in filas if f["particion"] == p}
              for p in ("entrenamiento", "validacion", "prueba")}
    if any(grupos[a] & grupos[b] for a, b in [("entrenamiento", "validacion"), ("entrenamiento", "prueba"), ("validacion", "prueba")]):
        raise ValueError("Un paciente está presente en varias particiones")
    fechas = {p: [f["fecha_cita"] for f in filas if f["particion"] == p] for p in grupos}
    reservas = {p: [f["fecha_reserva"] for f in filas if f["particion"] == p] for p in grupos}
    if not (max(fechas["entrenamiento"]) < min(reservas["validacion"]) and max(fechas["validacion"]) < min(reservas["prueba"])):
        raise ValueError("Las particiones no respetan el orden temporal")
    por_paciente = {}
    for fila in filas:
        por_paciente.setdefault(fila["paciente_sintetico"], []).append(fila)
    for consultas in por_paciente.values():
        for fila in consultas:
            anteriores = [c for c in consultas if c["fecha_cita"] < fila["fecha_reserva"]]
            faltas = sum(c["no_asistio"] for c in anteriores)
            if fila["inasistencias_previas"] != faltas or fila["atenciones_previas"] != len(anteriores)-faltas:
                raise ValueError("La instantánea contiene resultados no disponibles al reservar")


def metricas(y, probabilidades):
    y = np.asarray(y)
    probabilidades = np.asarray(probabilidades, dtype=float)
    if y.ndim != 1 or probabilidades.shape != y.shape or set(np.unique(y)) != {0, 1}:
        raise ValueError("La evaluación requiere ambas clases y una probabilidad por resultado")
    if not np.all(np.isfinite(probabilidades)) or np.any((probabilidades < 0) | (probabilidades > 1)):
        raise ValueError("Probabilidades inválidas")
    predicciones = probabilidades >= 0.5
    bins = []
    grupos = np.minimum((probabilidades * 10).astype(int), 9)
    for grupo in range(10):
        mascara = grupos == grupo
        if np.any(mascara):
            bins.append({"desde": grupo / 10, "muestras": int(mascara.sum()),
                         "probabilidad_media": float(probabilidades[mascara].mean()),
                         "frecuencia_observada": float(y[mascara].mean())})
    return {"roc_auc": float(roc_auc_score(y, probabilidades)),
            "pr_auc": float(average_precision_score(y, probabilidades)),
            "brier": float(brier_score_loss(y, probabilidades)),
            "precision_umbral_05": float(precision_score(y, predicciones, zero_division=0)),
            "recall_umbral_05": float(recall_score(y, predicciones, zero_division=0)),
            "f1_umbral_05": float(f1_score(y, predicciones, zero_division=0)),
            "matriz_confusion_tn_fp_fn_tp": confusion_matrix(y, predicciones, labels=[0, 1]).tolist(),
            "calibracion": bins}


def intervalos_por_paciente(y, probabilidades, pacientes, repeticiones=300):
    y = np.asarray(y)
    probabilidades = np.asarray(probabilidades, dtype=float)
    pacientes = np.asarray(pacientes)
    metricas(y, probabilidades)
    if pacientes.shape != y.shape or repeticiones < 20:
        raise ValueError("Agrupación o cantidad de remuestreos inválida")
    unicos = np.unique(pacientes)
    if len(unicos) < 2:
        raise ValueError("Se requieren varios pacientes de prueba")
    indices = {p: np.flatnonzero(pacientes == p) for p in unicos}
    rng = np.random.default_rng(SEMILLA + 1)
    resultados = []
    for _ in range(repeticiones):
        seleccion = np.concatenate([indices[p] for p in rng.choice(unicos, size=len(unicos), replace=True)])
        muestra_y, muestra_p = y[seleccion], probabilidades[seleccion]
        if len(np.unique(muestra_y)) == 2:
            resultados.append([roc_auc_score(muestra_y, muestra_p), average_precision_score(muestra_y, muestra_p), brier_score_loss(muestra_y, muestra_p)])
    if len(resultados) < 20:
        raise ValueError("Remuestreos válidos insuficientes para informar intervalos")
    extremos = np.quantile(resultados, [0.025, 0.975], axis=0)
    return {"metodo": "bootstrap por paciente sobre prueba sintética", "confianza": 0.95,
            "repeticiones": repeticiones, "muestras_validas": len(resultados),
            "metricas": {nombre: {"inferior": float(extremos[0, i]), "superior": float(extremos[1, i])}
                         for i, nombre in enumerate(["roc_auc", "pr_auc", "brier"])}}


def escribir_json(ruta, datos):
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(json.dumps(datos, ensure_ascii=False, indent=2, allow_nan=False) + "\n", encoding="utf-8")


def entrenar(salida):
    filas = generar_datos()
    comprobar_separacion(filas)
    buffer = io.StringIO(newline="")
    writer = csv.DictWriter(buffer, fieldnames=list(filas[0]), lineterminator="\n")
    writer.writeheader()
    writer.writerows(filas)
    csv_texto = buffer.getvalue()
    salida.mkdir(parents=True, exist_ok=True)
    (salida / "datos-sinteticos.csv").write_text(csv_texto, encoding="utf-8")
    x_train, y_train = matrices(filas, "entrenamiento")
    x_val, y_val = matrices(filas, "validacion")
    x_test, y_test = matrices(filas, "prueba")
    candidatos = []
    for c in (0.1, 1.0, 10.0):
        modelo = make_pipeline(StandardScaler(), LogisticRegression(C=c, max_iter=2000, random_state=SEMILLA))
        modelo.fit(x_train, y_train)
        candidatos.append((brier_score_loss(y_val, modelo.predict_proba(x_val)[:, 1]), c, modelo))
    perdida_val, c, modelo = min(candidatos, key=lambda candidato: candidato[0])
    escala = modelo.named_steps["standardscaler"]
    regresion = modelo.named_steps["logisticregression"]
    if list(regresion.classes_) != [0, 1]:
        raise ValueError("Orden de clases inesperado")
    artefacto = {"esquema": 1, "version": "inasistencia-sintetico-v1", "objetivo": "NO_ASISTIO",
                 "origen": "SINTETICO", "experimental": True,
                 "intercepto": float(regresion.intercept_[0]), "umbralMedio": 0.25, "umbralAlto": 0.5,
                 "variables": [{"nombre": nombre, "media": float(escala.mean_[i]),
                                "escala": float(escala.scale_[i]), "coeficiente": float(regresion.coef_[0, i])}
                               for i, nombre in enumerate(VARIABLES)]}
    escribir_json(salida / "modelo.json", artefacto)
    baseline = DummyClassifier(strategy="prior").fit(x_train, y_train)
    predicciones_prueba = modelo.predict_proba(x_test)[:, 1]
    pacientes_prueba = [f["paciente_sintetico"] for f in filas if f["particion"] == "prueba"]
    informe = {"version": artefacto["version"], "datos": "Exclusivamente sintéticos; no demuestra eficacia clínica real",
               "semilla": SEMILLA, "sklearn": sklearn.__version__, "numpy": np.__version__,
               "sha256_datos": hashlib.sha256(csv_texto.encode()).hexdigest(),
               "sha256_entrenador": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
               "particiones": {p: {"citas": len(matrices(filas, p)[1]),
                                   "pacientes": len({f['paciente_sintetico'] for f in filas if f['particion'] == p})}
                               for p in ("entrenamiento", "validacion", "prueba")},
               "pacientes_disjuntos": True, "orden_temporal_respetado": True, "instantanea_en_reserva_validada": True,
               "seleccion_por_validacion": {"C": c, "brier": float(perdida_val)},
               "prueba": metricas(y_test, predicciones_prueba),
               "incertidumbre_prueba": intervalos_por_paciente(y_test, predicciones_prueba, pacientes_prueba),
               "referencia_constante": metricas(y_test, baseline.predict_proba(x_test)[:, 1])}
    escribir_json(salida / "evaluacion.json", informe)
    ejemplos = [[0, 0, 0, 1], [0, 8, 7, 1], [3, 2, 30, 0], [7, 4, 45, 0], [3, 2, 30, 1]]
    escribir_json(salida / "paridad-java.json", [{"entrada": x, "probabilidad": float(modelo.predict_proba([x])[0, 1])} for x in ejemplos])
    print(json.dumps({"modelo": artefacto["version"], "origen": "SINTETICO", "citas": len(filas),
                      "roc_auc_prueba": informe["prueba"]["roc_auc"], "brier_prueba": informe["prueba"]["brier"]}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Entrena un modelo demostrativo con datos sintéticos; no conecta con la base clínica.")
    parser.add_argument("--salida", type=Path, default=RAIZ / "target/ia")
    args = parser.parse_args()
    entrenar(args.salida)
