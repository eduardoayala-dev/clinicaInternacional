import copy
import unittest

import numpy as np

from entrenar_inasistencia import (
    comprobar_separacion, generar_datos, intervalos_por_paciente, metricas,
)


class EvaluacionInasistenciaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.filas = generar_datos()

    def test_generacion_y_separacion_son_reproducibles(self):
        self.assertEqual(self.filas, generar_datos())
        self.assertEqual(7200, len(self.filas))
        comprobar_separacion(self.filas)

    def test_detecta_pacientes_compartidos_y_resultados_futuros(self):
        compartida = copy.deepcopy(self.filas)
        compartida[-1]['paciente_sintetico'] = compartida[0]['paciente_sintetico']
        with self.assertRaises(ValueError):
            comprobar_separacion(compartida)
        filtracion = copy.deepcopy(self.filas)
        filtracion[0]['inasistencias_previas'] = 1
        with self.assertRaises(ValueError):
            comprobar_separacion(filtracion)
        temporal = copy.deepcopy(self.filas)
        temporal[-1]['fecha_reserva'] = '2023-01-01'
        with self.assertRaises(ValueError):
            comprobar_separacion(temporal)

    def test_calibracion_cuenta_limites_y_probabilidad_uno_una_sola_vez(self):
        y = np.array([0, 1] * 6)
        probabilidades = np.array([0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1, .3])
        resultado = metricas(y, probabilidades)
        self.assertEqual(len(y), sum(b['muestras'] for b in resultado['calibracion']))
        self.assertEqual(2, next(b for b in resultado['calibracion'] if b['desde'] == .9)['muestras'])
        self.assertEqual(2, next(b for b in resultado['calibracion'] if b['desde'] == .3)['muestras'])

    def test_rechaza_evaluaciones_incompletas_y_probabilidades_invalidas(self):
        for y, p in [([0, 0], [.1, .2]), ([0, 1], [.1]), ([0, 1], [np.nan, .2]),
                     ([0, 1], [-.1, .2]), ([0, 1], [.1, 1.1])]:
            with self.assertRaises(ValueError):
                metricas(y, p)

    def test_bootstrap_reproducible_por_paciente_sin_modificar_entradas(self):
        y = np.array([0, 1] * 4)
        p = np.array([.1, .8, .2, .6, .4, .9, .2, .7])
        pacientes = np.repeat(np.arange(4), 2)
        original = p.copy()
        resultado = intervalos_por_paciente(y, p, pacientes, repeticiones=30)
        self.assertEqual(resultado, intervalos_por_paciente(y, p, pacientes, repeticiones=30))
        self.assertEqual(30, resultado['muestras_validas'])
        np.testing.assert_array_equal(original, p)
        for intervalo in resultado['metricas'].values():
            self.assertTrue(0 <= intervalo['inferior'] <= intervalo['superior'] <= 1)
        with self.assertRaises(ValueError):
            intervalos_por_paciente(y, p, [1], repeticiones=30)


if __name__ == '__main__':
    unittest.main()
