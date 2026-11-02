package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.Connection;
import java.sql.SQLException;

@EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class ToleranciaConfiguradaMySqlTest extends ToleranciaConfiguradaTest {
    @Override protected CitaAutorizacionTest crearEsquema(){
        return new CitaAutorizacionTest(){
            @Override protected Connection abrir()throws SQLException{return MySqlPruebas.abrir();}
        };
    }
}
