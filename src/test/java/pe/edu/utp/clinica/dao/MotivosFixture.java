package pe.edu.utp.clinica.dao;

import org.junit.jupiter.api.BeforeEach;
import java.sql.*;
import java.util.UUID;

abstract class MotivosFixture {
    private String url;
    protected Connection abrir()throws SQLException{return DriverManager.getConnection(url);}
    protected GestionMotivosDAO dao;
    protected void sql(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement()){s.execute(texto);}}
    protected int cantidad(String texto)throws SQLException{try(var c=abrir();var s=c.createStatement();var r=s.executeQuery(texto)){r.next();return r.getInt(1);}}
    @BeforeEach void preparar()throws Exception{
        url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(var c=abrir()){EsquemaPruebas.limpiar(c);}
        sql("CREATE TABLE usuario(id_usuario INT PRIMARY KEY,rol VARCHAR(40),activo BOOLEAN)");
        sql("CREATE TABLE sede(id_sede INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20))");
        sql("CREATE TABLE personal_administrativo(id_usuario INT PRIMARY KEY,id_sede INT)");
        sql("CREATE TABLE especialidad(id_especialidad INT PRIMARY KEY,nombre VARCHAR(100),estado VARCHAR(20))");
        sql("CREATE TABLE motivo_cita(id_motivo INT AUTO_INCREMENT PRIMARY KEY,id_especialidad INT,descripcion VARCHAR(200),estado VARCHAR(20))");
        sql("CREATE TABLE cita(id_cita INT PRIMARY KEY,id_motivo INT)");
        sql("CREATE TABLE auditoria_seguridad(id_auditoria INT AUTO_INCREMENT PRIMARY KEY,id_usuario INT,ip_origen VARCHAR(45) NOT NULL,evento VARCHAR(100),descripcion VARCHAR(500),sospechoso BOOLEAN)");
        sql("INSERT INTO usuario VALUES(1,'ADMINISTRADOR',TRUE),(2,'PERSONAL_ADMINISTRATIVO',TRUE),(3,'PACIENTE',TRUE),(4,'ADMINISTRADOR',FALSE),(5,'ADMINISTRADOR',TRUE)");
        sql("INSERT INTO sede VALUES(1,'Sede local','ACTIVO')");sql("INSERT INTO personal_administrativo VALUES(2,1)");
        sql("INSERT INTO especialidad VALUES(1,'Medicina General','ACTIVO'),(2,'Dermatología','ACTIVO'),(3,'Otra','INACTIVO')");
        dao=new GestionMotivosDAO(this::abrir);
    }
}
