package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class ReporteAdmisionMySqlTest extends ReporteAdmisionTest {
    @Override protected CitaAutorizacionTest crearEsquema(){return new CitaAutorizacionTest(){@Override protected Connection abrir()throws SQLException{return MySqlPruebas.abrir();}};}
}
