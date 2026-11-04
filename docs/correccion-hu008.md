# Corrección del escenario HU008

La condición anterior descartaba una evaluación médica si no tenía fecha recomendada. Esto no cumplía el escenario de asignar prioridad al terminar una consulta.

La última evaluación finalizada de la misma especialidad aporta ALTA, MEDIA o BAJA también sin fecha. Si existe fecha límite, se respeta. Una nueva evaluación reemplaza la anterior y completar el control vinculado consume la prioridad previa. La prioridad no elimina las restricciones de sede, especialidad, médico preferido, turno ni anticipación.

Entre candidatos compatibles se ordena ALTA, MEDIA y BAJA; los empates se resuelven por antigüedad e identificador. La prueba específica inscribe primero BAJA, después MEDIA y finalmente ALTA, todos sin fecha: recibe la oferta ALTA y, al rechazarla, MEDIA. La actualización no quita ofertas ya emitidas ni crea inscripciones sin preferencias del paciente.

El paciente ve la explicación de asignación de ofertas, sin presentar la clasificación clínica interna como una medida de gravedad. El personal autorizado conserva la prioridad para la gestión.

Rama `fix/prioridad-clinica-hu008`, cambios sin commit ni PR. No requiere migración ni actualización manual de prioridades guardadas: el motor consulta la evaluación al ordenar candidatos.
