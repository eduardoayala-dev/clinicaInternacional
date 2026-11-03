package pe.edu.utp.clinica.catalogo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.GestionMotivosDAO;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;
import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;

public final class ImportacionMotivosExcel {
    public static final int MAX_BYTES=1_048_576;
    public static final int MAX_FILAS=200;
    private ImportacionMotivosExcel() {}

    public static List<FilaImportacionMotivo> leer(byte[] archivo) {
        if(archivo==null||archivo.length==0||archivo.length>MAX_BYTES)throw invalido("Selecciona un archivo XLSX de hasta 1 MB.");
        try{
            comprobarZip(archivo);
            try(var libro=new XSSFWorkbook(new ByteArrayInputStream(archivo))){
                if(libro.isMacroEnabled()||!libro.getExternalLinksTables().isEmpty())throw invalido("La plantilla no debe contener macros ni vínculos a otros libros.");
                var hoja=libro.getSheet("Motivos");
                if(hoja==null||libro.isSheetHidden(libro.getSheetIndex(hoja))||libro.isSheetVeryHidden(libro.getSheetIndex(hoja)))throw invalido("Usa la hoja visible Motivos de la plantilla.");
                if(hoja.getLastRowNum()>MAX_FILAS)throw invalido("La plantilla admite hasta 200 filas de datos.");
                var cabecera=hoja.getRow(0);
                if(cabecera==null||cabecera.getLastCellNum()!=2||!"Especialidad_ID".equals(texto(cabecera.getCell(0)))||!"Motivo".equals(texto(cabecera.getCell(1))))throw invalido("Las columnas deben ser Especialidad_ID y Motivo.");
                List<FilaImportacionMotivo> filas=new ArrayList<>();
                for(int i=1;i<=hoja.getLastRowNum();i++){
                    var fila=hoja.getRow(i);if(vacia(fila))continue;
                    filas.add(leerFila(fila,i+1));
                }
                if(filas.isEmpty())throw invalido("El archivo no contiene motivos para importar.");
                return List.copyOf(filas);
            }
        }catch(AgendaException e){throw e;}
        catch(IOException|RuntimeException e){throw invalido("No se pudo leer el archivo. Descarga una plantilla nueva y utiliza el formato XLSX.");}
    }
    private static FilaImportacionMotivo leerFila(Row fila,int numero){
        try{
            if(fila.getLastCellNum()>2)throw invalido("Usa únicamente las dos columnas de la plantilla.");
            var celda=fila.getCell(0);int especialidad;
            if(celda!=null&&celda.getCellType()==CellType.NUMERIC){
                double valor=celda.getNumericCellValue();if(valor<1||valor>99_999_999||valor!=Math.rint(valor))throw invalido("Especialidad_ID debe ser un identificador entero positivo.");especialidad=(int)valor;
            }else{
                String codigo=texto(celda);if(!codigo.matches("[1-9][0-9]{0,7}"))throw invalido("Especialidad_ID debe ser un identificador entero positivo.");especialidad=Integer.parseInt(codigo);
            }
            return new FilaImportacionMotivo(numero,especialidad,texto(fila.getCell(1)),"");
        }catch(AgendaException e){return new FilaImportacionMotivo(numero,0,"",e.getMessage());}
    }
    private static boolean vacia(Row fila){
        if(fila==null)return true;
        for(Cell c:fila){if(c.getCellType()!=CellType.BLANK && !(c.getCellType()==CellType.STRING&&c.getStringCellValue().isBlank()))return false;}
        return true;
    }
    private static String texto(Cell celda){
        if(celda==null||celda.getCellType()==CellType.BLANK)return "";
        if(celda.getCellType()!=CellType.STRING||celda.getHyperlink()!=null)throw invalido("Usa texto sin fórmulas ni hipervínculos.");
        return celda.getStringCellValue().strip();
    }
    private static void comprobarZip(byte[] archivo)throws IOException{
        int entradas=0,total=0;byte[] buffer=new byte[8192];
        try(var zip=new ZipInputStream(new ByteArrayInputStream(archivo))){
            java.util.zip.ZipEntry entrada;
            while((entrada=zip.getNextEntry())!=null){
                if(++entradas>100||entrada.getName().toLowerCase(Locale.ROOT).contains("vbaproject"))throw invalido("El archivo no corresponde a una plantilla XLSX admitida.");
                int leidos;while((leidos=zip.read(buffer))!=-1){total+=leidos;if(total>8_388_608)throw invalido("El contenido del libro supera el tamaño admitido.");}
            }
        }
        if(entradas==0)throw invalido("Selecciona un archivo XLSX válido.");
    }
    public static byte[] plantilla(List<GestionMotivosDAO.Especialidad> especialidades)throws IOException{
        if(especialidades.isEmpty())throw invalido("No hay especialidades activas para preparar la plantilla.");
        try(var libro=new XSSFWorkbook();var salida=new ByteArrayOutputStream()){
            var motivos=libro.createSheet("Motivos");var catalogo=libro.createSheet("Especialidades");var instrucciones=libro.createSheet("Instrucciones");
            var estilo=libro.createCellStyle();estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var fuente=libro.createFont();fuente.setBold(true);fuente.setColor(IndexedColors.WHITE.getIndex());estilo.setFont(fuente);
            cabecera(motivos,estilo,"Especialidad_ID","Motivo");cabecera(catalogo,estilo,"Especialidad_ID","Especialidad");
            for(int i=0;i<especialidades.size();i++){var e=especialidades.get(i);var r=catalogo.createRow(i+1);r.createCell(0).setCellValue(e.id());r.createCell(1).setCellValue(e.nombre());}
            var nombre=libro.createName();nombre.setNameName("IdsEspecialidades");nombre.setRefersToFormula("Especialidades!$A$2:$A$"+(especialidades.size()+1));
            var ayuda=motivos.getDataValidationHelper();var validacion=ayuda.createValidation(ayuda.createFormulaListConstraint("IdsEspecialidades"),new CellRangeAddressList(1,MAX_FILAS,0,0));validacion.setShowErrorBox(true);validacion.createErrorBox("Especialidad inválida","Selecciona un ID de la hoja Especialidades.");motivos.addValidationData(validacion);
            String[] notas={"Completa la hoja Motivos. No cambies sus encabezados.","Usa los identificadores de la hoja Especialidades.","Máximo 200 filas. El motivo debe tener entre 3 y 200 caracteres.","No uses fórmulas, macros ni vínculos externos.","La vista previa no guarda datos. Debes confirmar una importación sin errores.","Las filas válidas se pueden importar aunque otras tengan errores. Los motivos existentes no se sobrescriben."};
            for(int i=0;i<notas.length;i++)instrucciones.createRow(i).createCell(0).setCellValue(notas[i]);
            motivos.setColumnWidth(0,22*256);motivos.setColumnWidth(1,70*256);catalogo.setColumnWidth(0,22*256);catalogo.setColumnWidth(1,50*256);instrucciones.setColumnWidth(0,110*256);
            motivos.createFreezePane(0,1);catalogo.createFreezePane(0,1);libro.write(salida);return salida.toByteArray();
        }
    }
    private static void cabecera(Sheet hoja,CellStyle estilo,String...titulos){var r=hoja.createRow(0);for(int i=0;i<titulos.length;i++){var c=r.createCell(i);c.setCellValue(titulos[i]);c.setCellStyle(estilo);}}
    private static AgendaException invalido(String mensaje){return new AgendaException(422,"PLANTILLA_INVALIDA",mensaje);}
}
