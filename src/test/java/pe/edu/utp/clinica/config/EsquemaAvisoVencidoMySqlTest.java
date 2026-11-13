package pe.edu.utp.clinica.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import pe.edu.utp.clinica.dao.*;

import java.nio.file.*;

@EnabledIfEnvironmentVariable(named = "AGENDA_MYSQL_URL", matches = "jdbc:mysql:.*")
class EsquemaAvisoVencidoMySqlTest {
    @Test
    void readinessRechazaLaMigracionIncompletaYConservaLaRestriccionDeTipos() throws Exception {
        try (var c = MySqlPruebas.abrir()) {
            EsquemaPruebas.limpiar(c);
            try (var s = c.createStatement()) {
                s.execute("CREATE TABLE cita(id_cita INT PRIMARY KEY)");
                s.execute(
                        Files.readString(Path.of("sql/migrations/V018__correo_cambios_cita.sql")));
                assertFalse(DisponibilidadSistema.esquemaAvisoVencido(c));
                String[] pasos =
                        Files.readString(
                                        Path.of(
                                                "sql/migrations/V020__aviso_confirmacion_vencida.sql"))
                                .split(";");
                s.execute(pasos[0]);
                assertFalse(DisponibilidadSistema.esquemaAvisoVencido(c));
                s.execute(pasos[1]);
                assertTrue(DisponibilidadSistema.esquemaAvisoVencido(c));
                s.execute("INSERT INTO cita VALUES(1)");
                s.execute(
                        "INSERT INTO correo_cambio_cita(id_cita,tipo,disponible_en)"
                            + " VALUES(1,'CONFIRMACION_VENCIDA','2026-11-02 10:00:00')");
                assertThrows(
                        java.sql.SQLException.class,
                        () ->
                                s.execute(
                                        "INSERT INTO correo_cambio_cita(id_cita,tipo,disponible_en)"
                                            + " VALUES(1,'OTRO','2026-11-02 10:00:00')"));
            }
        }
    }
}
