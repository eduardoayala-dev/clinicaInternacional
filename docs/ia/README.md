# IA para inasistencias y reasignación

Compromiso de propuesta de valor: HU017 debe incluir un modelo entrenado e integrado, con explicación y evaluación. HU013 incorporará aprendizaje para apoyar la selección de candidatos compatibles. Las reglas existentes de riesgo y ordenamiento no bastan para declarar completo ese compromiso.

## Modelo y simulador experimental

- Regresión logística entrenada con scikit-learn, con normalización ajustada exclusivamente sobre entrenamiento.
- Variables: inasistencias previas, atenciones previas, días entre reserva y cita, confirmación expresa del paciente. No se usan nombres, DNI, correo, sexo, diagnósticos ni texto clínico.
- Objetivo: `NO_ASISTIO`, distinto de cancelación anticipada. El simulador no predice diagnósticos ni decide prioridad clínica.
- Exportación de coeficientes y normalización a JSON; Java ejecuta la inferencia localmente. No requiere un servicio Python en Railway, una API de IA ni suscripción.
- Ruta protegida para administrador: `/sysadmin/ia/inasistencia`, enlazada desde Configuración de Priorización y Reasignación.
- El resultado muestra probabilidad, nivel, versión, origen sintético, factores, recomendación y advertencias. Las contribuciones son log-odds respecto al promedio de entrenamiento; no son causas ni puntos porcentuales.
- Los umbrales 0,25 y 0,50 son cortes demostrativos, no umbrales clínicamente validados.
- Solo simula escenarios introducidos por el administrador. No consulta pacientes, no cambia `riesgo_cancelacion`, no registra asistencia, no cancela citas y no modifica ofertas.
- Si falta el modelo o es incompatible, el simulador responde 503. El resto del sistema conserva su funcionamiento anterior. El cargador rechaza que este artefacto sintético sea presentado como validado/real.
- El cargador limita el archivo a 64 KiB, valida campos obligatorios y tipos, y rechaza claves repetidas, contenido extra y JSON excesivamente anidado. No convierte textos en números ni decimales en enteros de forma silenciosa.
- Al editar una entrada se oculta el resultado y se cancela la petición anterior. Una respuesta tardía no reemplaza el escenario actual. El cliente comprueba el contrato, la procedencia y los cuatro factores antes de mostrar la predicción; los mensajes se insertan como texto.
- Se bloquea el doble envío, se limita la espera a 15 segundos y se permite reintentar tras un fallo. Parámetros HTTP repetidos y números con formato ambiguo se rechazan. Los fallos de transporte se manejan sin propagar excepciones del servlet.

Esta entrega **no cierra HU017**: faltan extracción histórica validada, evaluación representativa, seguimiento de contacto y decisión sobre el uso operativo. Tampoco cierra la IA de HU013.

## Evaluación reproducible

Datos generados localmente, sin conexiones a la base clínica: 600 pacientes y 7.200 citas **sintéticos**. El generador define asociaciones artificiales entre variables y resultado; aprenderlas no demuestra que existan en una clínica real.

| Partición | Pacientes | Citas | Periodo sintético |
|---|---:|---:|---|
| Entrenamiento | 420 | 5.040 | 2024 |
| Validación | 90 | 1.080 | 2025 |
| Prueba | 90 | 1.080 | 2026 |

No se comparten pacientes entre particiones. Cada fila contiene únicamente resultados de citas anteriores a su fecha de reserva; se excluyen también las consultas que ocurren entre reserva y cita. El script comprueba cada instantánea contra las fechas del historial. La normalización usa entrenamiento; la selección entre tres valores de regularización usa validación; la evaluación final usa prueba. La separación temporal y por paciente está comprobada en el script. No se usan resultados futuros ni se ajusta el modelo con el conjunto de prueba.

| Métrica en prueba sintética | Modelo | Referencia constante |
|---|---:|---:|
| ROC-AUC | 0,7925 | 0,5000 |
| PR-AUC (average precision) | 0,6619 | 0,3074 |
| Brier, menor es mejor | 0,1603 | 0,2129 |
| Precision, umbral 0,5 | 0,6682 | 0 |
| Recall, umbral 0,5 | 0,4247 | 0 |

La referencia constante aprende la frecuencia de inasistencia de entrenamiento. No equivale al algoritmo actual que cuenta cancelaciones. La matriz de confusión tiene filas reales y columnas predichas, ambas en orden `[asistió, no asistió]`: `[[678,70],[191,141]]`. El recall muestra que el umbral demostrativo pierde numerosos casos incluso en estos datos. Se incluyen bins de calibración, versiones y huellas SHA-256 en `evaluacion-inasistencia-sintetica-v1.json`.

El informe también incorpora 300 remuestreos de los pacientes de prueba, conservando juntas sus citas. Los intervalos percentiles del 95 % resultantes son ROC-AUC `[0,7591; 0,8233]`, PR-AUC `[0,5841; 0,7277]` y Brier `[0,1440; 0,1749]`. Describen variabilidad dentro de la prueba sintética; no demuestran validez para pacientes reales ni corrigen el sesgo del generador. No se usan para ajustar el modelo o elegir el umbral. La calibración asigna cada muestra a un único intervalo e incluye probabilidades exactamente iguales a uno.

### Repetir el entrenamiento

Desde el repositorio, con Python 3.12 y un entorno virtual independiente:

```powershell
python -m venv target/ia-venv
target/ia-venv/Scripts/python.exe -m pip install -r scripts/ia/requirements.txt
target/ia-venv/Scripts/python.exe scripts/ia/entrenar_inasistencia.py
```

Se generan `target/ia/datos-sinteticos.csv`, `modelo.json`, `evaluacion.json` y `paridad-java.json`. No se sobrescribe el modelo empaquetado automáticamente. Después de revisar resultados, una promoción explícita copia el modelo a `src/main/resources/ia/riesgo-inasistencia-demo.json`, los casos de paridad a `src/test/resources/ia/paridad-inasistencia.json` y el informe a esta carpeta. Maven comprueba la paridad Python/Java con tolerancia de `1e-12`. Las dependencias de entrenamiento no entran en el WAR.

Para verificar la separación temporal, las instantáneas, la calibración y el remuestreo con el mismo entorno:

```powershell
target/ia-venv/Scripts/python.exe -m unittest discover -s scripts/ia -p "test_*.py" -v
node --test tests/ia-inasistencia-api.test.cjs
```

## Integración con el seguimiento de citas

La nueva rama incorpora una evaluación de citas futuras por admisión a partir de antecedentes documentados, con autorización por sede. Separa la probabilidad aprendida de la regla de plazo vencido y conserva el estado de la cita. El entrenamiento sigue siendo sintético. Ver [flujo y límites de la integración](seguimiento-citas.md). Su prueba manual está pendiente; no se declara cerrada HU017 ni la IA de HU013.

## Preparación de datos reales y cierre pendiente

1. Guardar fechas de reserva y confirmación expresa, origen y resultado final con auditoría. El estado actual `CONFIRMADA`, asignado al reservar, **no demuestra** confirmación expresa del paciente.
2. Construir instantáneas con información disponible en el momento de predecir. Separar `ATENDIDA` y `NO_ASISTIO`; excluir canceladas, reprogramadas, pendientes de cierre y casos sin resultado inequívoco.
3. Confirmar que una inasistencia fue registrada por personal autorizado. No inventarla porque pasó la fecha.
4. Definir ventana de predicción, población y política de datos faltantes. Evitar que entradas fuera del rango de entrenamiento parezcan validadas.
5. Evaluar por periodos y pacientes, comparando contra referencia simple, con calibración, recall, precision, métricas por segmentos e incertidumbre. No fijar una cifra mínima de citas arbitraria como garantía de calidad.
6. Probar inicialmente sin efectos sobre citas/ofertas; registrar versión, entrada y explicación con mínima información personal y acceso restringido. Aprobar su uso con evidencia antes de influir en operaciones.
7. Incorporar seguimiento de contacto de admisión. El riesgo no cancela ni reasigna citas; la IA solo recomienda.

## IA de reasignación: siguiente modelo, no completado

Requiere eventos históricos de ofertas (`ACEPTADA`, `RECHAZADA`, `EXPIRADA`) y las condiciones conocidas al ofrecer el cupo. Se evaluará un modelo de aceptación de ofertas; no se deduce esa probabilidad como `1 - riesgo de inasistencia`, porque son resultados distintos.

Primero se filtra por sede/especialidad, disponibilidad, preferencias y anticipación. La prioridad clínica es una restricción; la IA no puede sustituirla. Deben definirse protección contra postergación indefinida, criterios de desempate y parámetros auditados de HU018. Una oferta solo se convierte en reserva cuando el paciente acepta, con transacción que evite reservas duplicadas. Ante fallo del modelo se mantiene el ordenamiento determinista existente.

## Fuentes técnicas

- [Regresión logística, probabilidades y coeficientes](https://scikit-learn.org/1.7/modules/generated/sklearn.linear_model.LogisticRegression.html).
- [Separación de datos y prevención de filtraciones](https://scikit-learn.org/stable/common_pitfalls.html).
- [Evaluación de calibración](https://scikit-learn.org/stable/modules/calibration.html).

## Evidencia de pruebas de esta entrega

- Suite general sobre la base actualizada: 759 casos descubiertos, 509 ejecutados, 250 omitidos por configuración, sin fallos/errores (`target/ia-revision-integral.log`).
- Se repitieron los 17 casos específicos después de los ajustes: paridad Python/Java, reconstrucción del logit, origen experimental, límites y extremos, ausencia de historial, artefactos inválidos, parámetros repetidos, permisos, contratos HTTP y fallos de transporte (`target/ia-validacion-final.log`).
- Cinco pruebas Python comprueban generación reproducible, detección de pacientes compartidos, resultados futuros, calibración e intervalos por paciente. El reentrenamiento conserva exactamente el modelo y los cinco casos de paridad empaquetados.
- Regresión JavaScript: 52 pruebas aprobadas, incluidas cuatro del contrato de IA (`target/ia-js-regresion.log`). La revisión detectó una salida del historial que no seguía la convención de escape del proyecto; se uniformó sin cambiar su texto o estado.
- Interacción en Edge con respuestas controladas: evita doble envío, aborta al editar, descarta respuestas tardías, mantiene bloqueada la petición actual y rechaza contratos inválidos. Las descripciones se representan como texto sin crear elementos HTML (`target/probar-ia-respuestas-tardias.cjs`).
- El entrenamiento final se repitió con la misma semilla y entorno: coeficientes y normalización idénticos. El informe registra la huella del entrenador.
- Navegador local: resultado del API coincide con Python; comprobación de CSRF ausente, rol no autorizado, entradas inválidas, recuperación del botón tras fallo de red y ausencia de desbordamiento a 1440/768/390/360 px. Las capturas y el auxiliar están en `target/ia-simulador-*.png` y `target/probar-ia-vistas.cjs`.
- Sin cambios de esquema por IA, sin nuevas citas/propuestas creadas y sin correos enviados por estas comprobaciones. Solo queda la aceptación manual del usuario antes de commits/PR.
