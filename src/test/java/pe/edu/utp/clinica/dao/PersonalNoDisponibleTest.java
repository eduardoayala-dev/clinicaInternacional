package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class PersonalNoDisponibleTest {
    @Test void falloDeBaseNoSePresentaComoPadronVacioNiTotalesCero(){
        var dao=new UsuarioDAO(){@Override protected Connection abrirConexion()throws SQLException{throw new SQLException("Prueba de indisponibilidad");}};
        assertEquals(503,assertThrows(AgendaException.class,dao::obtenerTodos).status());
        assertEquals(503,assertThrows(AgendaException.class,dao::obtenerMetricasPersonal).status());
    }
}
