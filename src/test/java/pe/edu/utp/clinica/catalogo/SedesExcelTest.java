package pe.edu.utp.clinica.catalogo;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.CatalogoSedesDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import java.util.List;
import java.io.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.junit.jupiter.api.Assertions.*;
class SedesExcelTest {
    private CatalogoSedesDAO.Sede sede(){return new CatalogoSedesDAO.Sede(1,"SB-01","Sede San Borja","Avenida de prueba 123","016102222","ACTIVO");}
    @Test void exportaTextoConCodigoYLeeLasCincoColumnas()throws Exception{
        var bytes=SedesExcel.escribir(List.of(sede()));var filas=SedesExcel.leer(bytes);assertEquals(1,filas.size());assertEquals("SB-01",filas.get(0).sede().codigo());assertEquals("016102222",filas.get(0).sede().telefono());
    }
    @Test void formulaEnDatosSeRechazaPorFila()throws Exception{
        byte[] bytes;try(var libro=new XSSFWorkbook(new ByteArrayInputStream(SedesExcel.escribir(List.of(sede()))));var salida=new ByteArrayOutputStream()){libro.getSheet("Sedes").getRow(1).getCell(1).setCellFormula("1+1");libro.write(salida);bytes=salida.toByteArray();}
        assertFalse(SedesExcel.leer(bytes).get(0).resultado().isEmpty());
    }
    @Test void rechazaArchivoExcesivoYPlantillaSinFilas()throws Exception{
        byte[] excesivo=new byte[1_048_577];assertThrows(AgendaException.class,()->SedesExcel.leer(excesivo));byte[] vacio=SedesExcel.escribir(List.of());assertThrows(AgendaException.class,()->SedesExcel.leer(vacio));
    }
}
