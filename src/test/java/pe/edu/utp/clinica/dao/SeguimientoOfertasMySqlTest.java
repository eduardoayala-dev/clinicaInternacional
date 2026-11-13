package pe.edu.utp.clinica.dao;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="AGENDA_MYSQL_URL",matches="jdbc:mysql:.*")
class SeguimientoOfertasMySqlTest extends SeguimientoOfertasDAOTest {
    @Override protected java.sql.Connection abrir()throws java.sql.SQLException{return MySqlPruebas.abrir();}
}
