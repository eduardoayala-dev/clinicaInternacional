package pe.edu.utp.clinica.persistence;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.persistence.entity.CorreoCitaEntidad;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaJpaTest {
    @Test void unaFallaRevierteLaEscrituraYConservaLaConexionDelLlamador() throws Exception {
        try (var conexion = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID())) {
            try (var sentencia = conexion.createStatement()) {
                sentencia.execute("CREATE TABLE correo_cita(id_cita INT PRIMARY KEY,estado VARCHAR(20),intentos INT,disponible_en TIMESTAMP)");
            }
            var esperada = new IllegalStateException("Operación interrumpida");
            var recibida = assertThrows(IllegalStateException.class, () -> PersistenciaJpa.transaccion(conexion, () -> {
                PersistenciaJpa.escribir(conexion, sesion -> {
                    sesion.persist(new CorreoCitaEntidad(42, LocalDateTime.of(2026, 11, 13, 9, 0)));
                    sesion.flush();
                    return null;
                });
                try (var sentencia = conexion.createStatement(); var filas = sentencia.executeQuery("SELECT COUNT(*) FROM correo_cita")) {
                    assertTrue(filas.next()); assertEquals(1, filas.getInt(1));
                }
                throw esperada;
            }));
            assertSame(esperada, recibida);
            assertFalse(conexion.isClosed());
            try (var sentencia = conexion.createStatement(); var filas = sentencia.executeQuery("SELECT COUNT(*) FROM correo_cita")) {
                assertTrue(filas.next()); assertEquals(0, filas.getInt(1));
            }
        }
    }

    @Test void unaEscrituraSinUnidadDeTrabajoNoSeEjecuta() throws Exception {
        try (var conexion = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID())) {
            conexion.setAutoCommit(false);
            assertThrows(SQLException.class, () -> PersistenciaJpa.escribir(conexion, sesion -> fail("No debe ejecutar la escritura")));
            assertFalse(conexion.isClosed());
            assertFalse(conexion.getAutoCommit());
        }
    }
}
