package pe.edu.utp.clinica.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

final class TransaccionJdbc {
    private static final Logger LOGGER = Logger.getLogger(TransaccionJdbc.class.getName());
    private static final ThreadLocal<java.util.List<Runnable>> AVISOS = new ThreadLocal<>();
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
        var previos = AVISOS.get();
        var avisos = new java.util.ArrayList<Runnable>();
        AVISOS.set(avisos);
        try {
            T result = operacion.ejecutar(conn);
            conn.commit();
            emitir(avisos);
            return result;
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            if (previos == null) AVISOS.remove();
            else AVISOS.set(previos);
        }
    }

    static void alConfirmar(Runnable aviso) {
        var avisos = AVISOS.get();
        if (avisos == null) throw new IllegalStateException("Se requiere una transacción activa.");
        avisos.add(aviso);
    }

    static void avisarSede(int sede) {
        alConfirmar(() -> pe.edu.utp.clinica.eventos.EventosAgenda.INSTANCIA.publicar(sede));
    }

    private static void emitir(java.util.List<Runnable> avisos) {
        for (Runnable aviso : avisos) {
            try { aviso.run(); }
            catch (RuntimeException e) { LOGGER.warning("La operación se guardó; no se pudo emitir su aviso."); }
        }
    }
}
