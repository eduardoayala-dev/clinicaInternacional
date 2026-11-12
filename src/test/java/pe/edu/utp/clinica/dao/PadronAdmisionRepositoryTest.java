package pe.edu.utp.clinica.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import pe.edu.utp.clinica.repository.PadronAdmisionRepository;

import java.time.*;

class PadronAdmisionRepositoryTest {
    private CitaAutorizacionTest esquema;

    @BeforeEach
    void preparar() throws Exception {
        esquema = new CitaAutorizacionTest();
        esquema.setUp();
        try (var c = esquema.abrir();
                var s = c.createStatement()) {
            s.execute("ALTER TABLE paciente ADD COLUMN sede_preferida VARCHAR(100)");
            s.execute(
                    "UPDATE paciente SET sede_preferida='Central',telefono='900000001' WHERE"
                        + " id_paciente=1");
            s.execute("INSERT INTO sede(id_sede,nombre) VALUES(2,'Otra sede')");
            s.execute(
                    "INSERT INTO disponibilidad"
                        + " VALUES(50,1,1,'2026-10-28','08:00:00','12:00:00',30,'PUBLICADO')");
            s.execute("UPDATE cita SET id_disponibilidad=50 WHERE id_cita=7");
        }
    }

    @Test
    void padronFiltraSedeYCuentaSoloReservasVigentes() throws Exception {
        try (var c = esquema.abrir()) {
            var repo = new PadronAdmisionRepository(c);
            assertEquals(1, repo.pacientes("", null, null, 1).size());
            assertTrue(repo.pacientes("", null, null, 2).isEmpty());
            assertEquals(
                    1, repo.pacientes("12345678", null, null, 1).get(0).entero("citas_activas"));
            try (var s = c.createStatement()) {
                s.execute("UPDATE cita SET estado='REPROGRAMADA' WHERE id_cita=7");
            }
            assertEquals(
                    0, repo.pacientes("Paciente", null, null, 1).get(0).entero("citas_activas"));
        }
    }

    @Test
    void filtroNoInterpretaComodinesComoDatosDelUsuario() throws Exception {
        try (var c = esquema.abrir()) {
            var repo = new PadronAdmisionRepository(c);
            assertTrue(repo.pacientes("%", null, null, 1).isEmpty());
            assertTrue(repo.pacientes("_", null, null, 1).isEmpty());
            assertEquals(1, repo.paciente("12345678"));
            assertEquals(-1, repo.paciente("' OR 1=1 --"));
        }
    }

    @Test
    void salaRespetaSedeHistoricaYLaFechaSinDuplicarPacientes() throws Exception {
        try (var c = esquema.abrir();
                var s = c.createStatement()) {
            s.execute("UPDATE medico SET id_sede=2 WHERE id_medico=1");
            var repo = new PadronAdmisionRepository(c);
            var dia = LocalDate.of(2026, 10, 28);
            var filas = repo.sala(1, dia);
            assertEquals(1, filas.size());
            assertEquals("Central", filas.get(0).texto("sede_nombre"));
            assertTrue(repo.sala(2, dia).isEmpty());
            assertTrue(repo.sala(1, dia.plusDays(1)).isEmpty());
        }
    }

    @Test
    void horariosYConteosSeRestringenALaSede() throws Exception {
        try (var c = esquema.abrir()) {
            var repo = new PadronAdmisionRepository(c);
            var filas = repo.disponibilidades(1);
            assertEquals(1, filas.size());
            assertEquals(50, filas.get(0).entero("id_disponibilidad"));
            assertEquals(1, filas.get(0).entero("citas_agendadas"));
            assertTrue(repo.disponibilidades(2).isEmpty());
        }
    }
}
