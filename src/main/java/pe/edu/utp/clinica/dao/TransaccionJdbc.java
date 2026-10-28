package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

final class TransaccionJdbc {
    private static final Logger LOGGER = Logger.getLogger(TransaccionJdbc.class.getName());
    private TransaccionJdbc() {}
    @FunctionalInterface
    interface Operacion<T> { T ejecutar(Connection conn) throws SQLException; }

    static <T> T ejecutar(AgendaCitaDAO.ConnectionSource source, Operacion<T> operacion) {
        try (Connection conn = source.open()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            return completar(conn, operacion);
        } catch (SQLException e) {
            LOGGER.warning(() -> "Operación de agenda no disponible: " + e.getSQLState());
            throw new AgendaException(503, "AGENDA_NO_DISPONIBLE", "No se pudo completar la operación. Inténtalo más tarde.");
        }
    }

    private static <T> T completar(Connection conn, Operacion<T> operacion) throws SQLException {
        try {
            T result = operacion.ejecutar(conn);
            conn.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        }
    }
}
