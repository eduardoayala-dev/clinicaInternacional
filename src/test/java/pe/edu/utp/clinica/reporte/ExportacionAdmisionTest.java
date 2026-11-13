package pe.edu.utp.clinica.reporte;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.ReporteAdmisionDAO;
import java.time.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import static org.junit.jupiter.api.Assertions.*;

class ExportacionAdmisionTest {
    private ReporteAdmisionDAO.Reporte muestra(int cantidad) {
        var fecha=LocalDate.of(2026,10,28);var citas=new ArrayList<ReporteAdmisionDAO.Cita>();
        for(int i=1;i<=cantidad;i++)citas.add(new ReporteAdmisionDAO.Cita(i,fecha.atTime(10,0),"Pérez Núñez; \"María\" "+"Apellido largo ".repeat(4),"00112233","=correo@example.test","987654321","Dra. Muñoz","Medicina general","CONFIRMADA","ALTO","Pendiente"));
        return new ReporteAdmisionDAO.Reporte("Sede San Borja",new FiltroReporte(fecha,fecha,"","citas"),new ReporteAdmisionDAO.Resumen(cantidad,0,0,cantidad,0,0,0,cantidad,0,null,2,0,0),citas,1,1,fecha.atTime(10,0));
    }
    @Test void csvConservaUtf8EscapaSeparadoresYNeutralizaFormulas() {
        String csv=new String(ExportacionAdmision.csv(muestra(1)),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\ufeff"));assertTrue(csv.contains("Pérez Núñez; \"\"María\"\""));assertTrue(csv.contains("\"00112233\""));assertTrue(csv.contains("'=correo@example.test"));
        for(String v:List.of("=1+1"," +1","@SUM(A1)","\tprueba"))assertTrue(ExportacionAdmision.celdaCsv(v).startsWith("\"'"));
    }
    @Test void excelEsUnLibroRealConDocumentosYFormulasComoTexto() throws Exception {
        try(var libro=new XSSFWorkbook(new ByteArrayInputStream(ExportacionAdmision.excel(muestra(1))))) {
            var hoja=libro.getSheet("Citas");assertEquals("00112233",hoja.getRow(1).getCell(3).getStringCellValue());assertEquals(CellType.STRING,hoja.getRow(1).getCell(4).getCellType());assertEquals("=correo@example.test",hoja.getRow(1).getCell(4).getStringCellValue());assertNotNull(libro.getSheet("Alcance"));assertNotNull(libro.getSheet("Indicadores"));
        }
    }
    @Test void pdfRealPaginadoConservaTildesYNombresLargos() throws Exception {
        byte[] pdf=ExportacionAdmision.pdf(muestra(18));
        try(var documento=Loader.loadPDF(pdf)) {
            assertTrue(documento.getNumberOfPages()>1);String texto=new PDFTextStripper().getText(documento);assertTrue(texto.contains("Pérez Núñez"));assertTrue(texto.contains("Muñoz"));assertTrue(texto.contains("Cita #18"));
            Path carpeta=Path.of("target/qa-reportes");Files.createDirectories(carpeta);Files.write(carpeta.resolve("reporte.pdf"),pdf);
            javax.imageio.ImageIO.write(new PDFRenderer(documento).renderImageWithDPI(0,110),"png",carpeta.resolve("reporte.png").toFile());
        }
    }
    @Test void indicadoresSeExportanEnLosTresFormatosSinDatosPersonales() throws Exception {
        var base=muestra(18);var filtro=new FiltroReporte(base.filtro().desde(),base.filtro().hasta(),"","indicadores");
        var reporte=new ReporteAdmisionDAO.Reporte(base.sede(),filtro,base.resumen(),List.of(),1,1,base.actualizado());
        String csv=new String(ExportacionAdmision.csv(reporte),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("Total de citas"));assertTrue(csv.contains("\"18\""));assertTrue(csv.contains("Sede San Borja"));assertFalse(csv.contains("Pérez"));assertTrue(csv.contains("Sin datos"));
        try(var libro=new XSSFWorkbook(new ByteArrayInputStream(ExportacionAdmision.excel(reporte)))) {
            assertNotNull(libro.getSheet("Indicadores"));assertNull(libro.getSheet("Citas"));assertEquals("18",libro.getSheet("Indicadores").getRow(1).getCell(1).getStringCellValue());
        }
        try(var pdf=Loader.loadPDF(ExportacionAdmision.pdf(reporte))) {
            String texto=new PDFTextStripper().getText(pdf);assertTrue(texto.contains("Total de citas: 18"));assertFalse(texto.contains("Pérez"));assertFalse(texto.contains("No hay citas"));
        }
    }
    @Test void filtrosInvalidosNoSeCorrigenSilenciosamente() {
        Clock clock=Clock.fixed(Instant.parse("2026-11-01T02:00:00Z"),ZoneId.of("America/Lima"));assertEquals(LocalDate.of(2026,10,31),FiltroReporte.leer(null,null,null,null,clock).desde());
        for(String fecha:List.of("ayer","2026-02-30"))assertThrows(pe.edu.utp.clinica.dao.AgendaException.class,()->FiltroReporte.leer(fecha,null,null,null,clock));
        assertThrows(pe.edu.utp.clinica.dao.AgendaException.class,()->FiltroReporte.leer("2026-10-01","2026-11-01",null,null,clock));assertThrows(pe.edu.utp.clinica.dao.AgendaException.class,()->FiltroReporte.leer(null,null,"' OR 1=1",null,clock));
    }
}
