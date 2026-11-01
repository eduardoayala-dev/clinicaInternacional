package pe.edu.utp.clinica.reporte;

import pe.edu.utp.clinica.dao.ReporteAdmisionDAO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

public final class ExportacionAdmision {
    private static final List<String> CABECERAS=List.of("Cita","Fecha y hora","Paciente","Documento","Correo","Teléfono","Médico","Especialidad","Estado","Riesgo registrado","Confirmación de asistencia");
    private ExportacionAdmision() {}
    private static List<String> valores(ReporteAdmisionDAO.Cita c){return List.of(Integer.toString(c.id()),c.fecha().toString().replace('T',' '),texto(c.paciente()),texto(c.documento()),texto(c.correo()),texto(c.telefono()),texto(c.medico()),texto(c.especialidad()),texto(c.estado()),texto(c.riesgo()),texto(c.confirmacion()));}
    private static String texto(String s){return s==null?"":s;}
    public static String celdaCsv(String valor) {
        String s=texto(valor);String inicio=s.stripLeading();
        if((!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0))>=0) || s.startsWith("\t") || s.startsWith("\r") || s.startsWith("\n"))s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static byte[] csv(ReporteAdmisionDAO.Reporte reporte) {
        if("indicadores".equals(reporte.filtro().tipo()))return csvIndicadores(reporte);
        StringBuilder s=new StringBuilder("\ufeff");s.append(String.join(";",CABECERAS)).append("\r\n");
        for(var cita:reporte.citas())s.append(String.join(";",valores(cita).stream().map(ExportacionAdmision::celdaCsv).toList())).append("\r\n");
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }
    public static byte[] excel(ReporteAdmisionDAO.Reporte reporte)throws IOException {
        try(var libro=new XSSFWorkbook();var bytes=new ByteArrayOutputStream()) {
            var estilo=libro.createCellStyle();var fuente=libro.createFont();fuente.setBold(true);fuente.setColor(IndexedColors.WHITE.getIndex());estilo.setFont(fuente);estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if(!"indicadores".equals(reporte.filtro().tipo()))hojaCitas(libro,reporte,estilo);
            var indicadores=libro.createSheet("Indicadores");indicadores.createFreezePane(0,1);
            List<String> titulos=List.of("Indicador","Valor","Unidad","Alcance");
            var cabecera=indicadores.createRow(0);for(int i=0;i<titulos.size();i++){var celda=cabecera.createCell(i);celda.setCellValue(titulos.get(i));celda.setCellStyle(estilo);indicadores.setColumnWidth(i,(i==3?100:32)*256);}
            int fila=1;for(var indicador:IndicadoresAdmision.obtener(reporte.resumen())){var row=indicadores.createRow(fila++);var datos=List.of(indicador.nombre(),indicador.valor(),indicador.unidad(),indicador.alcance());for(int i=0;i<datos.size();i++)row.createCell(i,CellType.STRING).setCellValue(datos.get(i));}
            var meta=libro.createSheet("Alcance");List<String> lineas=List.of("Sede: "+reporte.sede(),"Desde: "+reporte.filtro().desde(),"Hasta: "+reporte.filtro().hasta(),"Estado: "+(reporte.filtro().estado().isBlank()?"Todos":reporte.filtro().estado()),"Tipo: "+reporte.filtro().tipo(),"Actualizado: "+reporte.actualizado(),"Citas analizadas: "+reporte.resumen().total(),"Filas de citas exportadas: "+reporte.citas().size());
            for(int i=0;i<lineas.size();i++)meta.createRow(i).createCell(0).setCellValue(lineas.get(i));meta.setColumnWidth(0,70*256);
            libro.write(bytes);return bytes.toByteArray();
        }
    }
    public static byte[] pdf(ReporteAdmisionDAO.Reporte reporte)throws IOException {
        try(var documento=new PDDocument();var bytes=new ByteArrayOutputStream()) {
            PDFont fuente=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<List<String>> paginas=new ArrayList<>();List<String> pagina=new ArrayList<>();paginas.add(pagina);
            pagina.add("INDICADORES DEL REPORTE");
            for(var indicador:IndicadoresAdmision.obtener(reporte.resumen()))envolver(indicador.nombre()+": "+indicador.valor()+("Sin datos".equals(indicador.valor())?"":" "+indicador.unidad()),pagina,fuente);
            pagina.add("");
            for(var cita:reporte.citas()) {
                List<String> bloque=new ArrayList<>();
                envolver("Cita #"+cita.id()+" | "+cita.fecha().toString().replace('T',' ')+" | "+cita.estado(),bloque,fuente);
                envolver("Paciente: "+texto(cita.paciente())+" | Documento: "+texto(cita.documento()),bloque,fuente);
                envolver("Médico: "+texto(cita.medico())+" | Especialidad: "+texto(cita.especialidad()),bloque,fuente);
                envolver("Correo: "+texto(cita.correo())+" | Teléfono: "+texto(cita.telefono()),bloque,fuente);
                envolver("Riesgo registrado: "+texto(cita.riesgo())+" | Asistencia: "+cita.confirmacion(),bloque,fuente);bloque.add("");
                if(!pagina.isEmpty() && pagina.size()+bloque.size()>29){pagina=new ArrayList<>();paginas.add(pagina);}
                pagina.addAll(bloque);
            }
            if(reporte.citas().isEmpty() && !"indicadores".equals(reporte.filtro().tipo()))pagina.add("No hay citas para los filtros seleccionados.");
            for(int numero=0;numero<paginas.size();numero++) {
                var hoja=new PDPage(new PDRectangle(PDRectangle.A4.getHeight(),PDRectangle.A4.getWidth()));documento.addPage(hoja);
                try(var contenido=new PDPageContentStream(documento,hoja)) {
                    cabeceraPdf(contenido,hoja,reporte,fuente);
                    contenido.beginText();contenido.setNonStrokingColor(25/255f,37/255f,55/255f);contenido.setFont(fuente,10);contenido.setLeading(14);contenido.newLineAtOffset(36,hoja.getMediaBox().getHeight()-125);
                    for(String linea:paginas.get(numero)){contenido.showText(linea);contenido.newLine();}contenido.endText();
                    contenido.beginText();contenido.setFont(fuente,8);contenido.newLineAtOffset(36,20);contenido.showText("Uso interno | Página "+(numero+1)+" de "+paginas.size());contenido.endText();
                }
            }
            documento.save(bytes);return bytes.toByteArray();
        }
    }
    private static byte[] csvIndicadores(ReporteAdmisionDAO.Reporte reporte) {
        StringBuilder csv=new StringBuilder("\ufeffINDICADOR;VALOR;UNIDAD;ALCANCE;SEDE;DESDE;HASTA;ESTADO;ACTUALIZADO\r\n");
        for(var indicador:IndicadoresAdmision.obtener(reporte.resumen())) {
            var fila=List.of(indicador.nombre(),indicador.valor(),indicador.unidad(),indicador.alcance(),reporte.sede(),reporte.filtro().desde().toString(),reporte.filtro().hasta().toString(),reporte.filtro().estado().isBlank()?"Todos":reporte.filtro().estado(),reporte.actualizado().toString());
            csv.append(String.join(";",fila.stream().map(ExportacionAdmision::celdaCsv).toList())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static void hojaCitas(XSSFWorkbook libro,ReporteAdmisionDAO.Reporte reporte,CellStyle estilo) {
        var hoja=libro.createSheet("Citas");hoja.createFreezePane(0,1);
        var cabecera=hoja.createRow(0);for(int i=0;i<CABECERAS.size();i++){var celda=cabecera.createCell(i);celda.setCellValue(CABECERAS.get(i));celda.setCellStyle(estilo);hoja.setColumnWidth(i,(i==2||i==4||i==6?34:24)*256);}
        int fila=1;for(var cita:reporte.citas()){var row=hoja.createRow(fila++);var datos=valores(cita);for(int i=0;i<datos.size();i++)row.createCell(i,CellType.STRING).setCellValue(datos.get(i));}
        hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,fila-1),0,CABECERAS.size()-1));
    }
    private static void cabeceraPdf(PDPageContentStream contenido,PDPage hoja,ReporteAdmisionDAO.Reporte reporte,PDFont fuente)throws IOException {
        float alto=hoja.getMediaBox().getHeight();
        contenido.beginText();contenido.setNonStrokingColor(13/255f,43/255f,78/255f);contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),14);contenido.newLineAtOffset(36,alto-36);contenido.showText("CLÍNICA INTERNACIONAL - REPORTE DE ADMISIÓN");contenido.endText();
        List<String> datos=new ArrayList<>();envolver("Sede: "+reporte.sede()+" | Período: "+reporte.filtro().desde()+" al "+reporte.filtro().hasta(),datos,fuente);
        envolver("Estado: "+(reporte.filtro().estado().isBlank()?"Todos":reporte.filtro().estado())+" | Tipo: "+reporte.filtro().tipo()+" | Citas analizadas: "+reporte.resumen().total(),datos,fuente);
        envolver("Actualizado: "+reporte.actualizado().toString().replace('T',' ')+" (America/Lima)",datos,fuente);
        contenido.beginText();contenido.setFont(fuente,10);contenido.setLeading(14);contenido.newLineAtOffset(36,alto-56);for(String linea:datos){contenido.showText(linea);contenido.newLine();}contenido.endText();
        contenido.setStrokingColor(0.8f,0.86f,0.92f);contenido.moveTo(36,alto-105);contenido.lineTo(hoja.getMediaBox().getWidth()-36,alto-105);contenido.stroke();
    }
    private static void envolver(String valor,List<String> salida,PDFont fuente)throws IOException {
        StringBuilder linea=new StringBuilder();float ancho=0;
        for(int cp:valor.codePoints().toArray()) {
            String caracter=Character.isISOControl(cp)?" ":new String(Character.toChars(cp));
            try{fuente.encode(caracter);}catch(IllegalArgumentException e){caracter="?";}
            float avance=fuente.getStringWidth(caracter)/1000*10;
            if(ancho+avance>765){salida.add(linea.toString());linea.setLength(0);ancho=0;}
            linea.append(caracter);ancho+=avance;
        }
        salida.add(linea.toString());
    }
}
