package pe.edu.utp.clinica.catalogo;

import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.*;
import pe.edu.utp.clinica.dao.*;
import java.io.*;
import java.util.List;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ImportacionMotivosExcelTest {
    private byte[] libro(java.util.function.Consumer<XSSFWorkbook> cambios)throws Exception{
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(ImportacionMotivosExcel.plantilla(List.of(new GestionMotivosDAO.Especialidad(13,"Medicina General")))));var out=new ByteArrayOutputStream()){
            cambios.accept(wb);wb.write(out);return out.toByteArray();
        }
    }
    private void fila(XSSFWorkbook wb,int numero){var r=wb.getSheet("Motivos").createRow(numero);r.createCell(0).setCellValue(13);r.createCell(1).setCellValue("Control importado");}
    @Test void plantillaUsaCatalogoRealYNoIncluyeAltasDeEjemplo()throws Exception{
        byte[] bytes=ImportacionMotivosExcel.plantilla(List.of(new GestionMotivosDAO.Especialidad(81,"Dermatología")));
        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(bytes))){assertEquals(81,wb.getSheet("Especialidades").getRow(1).getCell(0).getNumericCellValue());assertEquals("Especialidad_ID",wb.getSheet("Motivos").getRow(0).getCell(0).getStringCellValue());assertEquals(0,wb.getSheet("Motivos").getLastRowNum());}
        assertEquals(422,assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(bytes)).status());
    }
    @Test void reconoceTextoYNumeroComoIdentificadoresSinInterpretarFormulas()throws Exception{
        byte[] bytes=libro(wb->{fila(wb,1);fila(wb,3);wb.getSheet("Motivos").getRow(3).getCell(0).setCellValue("13");});
        var filas=ImportacionMotivosExcel.leer(bytes);assertEquals(2,filas.size());assertEquals(2,filas.get(0).numero());assertEquals(4,filas.get(1).numero());assertTrue(filas.stream().allMatch(f->f.error().isEmpty()));
    }
    @Test void identificaErroresDeCeldaPorNumeroDeFila()throws Exception{
        byte[] bytes=libro(wb->{fila(wb,1);wb.getSheet("Motivos").getRow(1).getCell(1).setCellFormula("1+1");fila(wb,2);wb.getSheet("Motivos").getRow(2).getCell(0).setCellValue(13.5);fila(wb,3);wb.getSheet("Motivos").getRow(3).createCell(2).setCellValue("Columna extra");});
        var filas=ImportacionMotivosExcel.leer(bytes);assertEquals(3,filas.size());assertTrue(filas.stream().allMatch(f->!f.error().isEmpty()));
    }
    @Test void rechazaEncabezadosModificadosYMasDeDoscientasFilas()throws Exception{
        byte[] cabecera=libro(wb->wb.getSheet("Motivos").getRow(0).getCell(1).setCellValue("Otra columna"));
        assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(cabecera));
        byte[] extra=libro(wb->wb.getSheet("Motivos").getRow(0).createCell(2).setCellValue("Extra"));assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(extra));
        byte[] grande=libro(wb->fila(wb,201));assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(grande));
    }
    @Test void rechazaMacrosYAlejaArchivosQueNoSonExcel()throws Exception{
        byte[] macro;try(var wb=new XSSFWorkbook(XSSFWorkbookType.XLSM);var out=new ByteArrayOutputStream()){wb.createSheet("Motivos");wb.write(out);macro=out.toByteArray();}
        assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(macro));assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer("Archivo de texto".getBytes()));assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(new byte[ImportacionMotivosExcel.MAX_BYTES+1]));
    }
    @Test void limitaContenidoDescomprimidoAntesDeAbrirElLibro()throws Exception{
        byte[] archivo;try(var bytes=new ByteArrayOutputStream();var zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry("contenido.xml"));zip.write(new byte[8_388_609]);zip.closeEntry();zip.finish();archivo=bytes.toByteArray();}
        assertThrows(AgendaException.class,()->ImportacionMotivosExcel.leer(archivo));
    }
}
