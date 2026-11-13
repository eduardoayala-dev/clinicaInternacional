package pe.edu.utp.clinica.catalogo;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import pe.edu.utp.clinica.dao.CatalogoSedesDAO;
import pe.edu.utp.clinica.dao.AgendaException;
import java.io.*;
import java.util.*;

public final class SedesExcel {
    private static final String[] CABECERA={"Codigo","Nombre","Direccion","Telefono","Estado"};
    private SedesExcel(){}
    public static byte[] escribir(List<CatalogoSedesDAO.Sede> sedes)throws IOException{
        try(var libro=new XSSFWorkbook();var salida=new ByteArrayOutputStream()){
            var hoja=libro.createSheet("Sedes");var fila=hoja.createRow(0);
            for(int i=0;i<CABECERA.length;i++){fila.createCell(i).setCellValue(CABECERA[i]);hoja.setColumnWidth(i,(i==2?60:28)*256);}
            int n=1;for(var sede:sedes){var r=hoja.createRow(n++);String[] valores={sede.codigo(),sede.nombre(),sede.direccion(),sede.telefono(),sede.estado()};for(int i=0;i<valores.length;i++)r.createCell(i).setCellValue(valores[i]);}
            hoja.createFreezePane(0,1);libro.write(salida);return salida.toByteArray();
        }
    }
    public static List<CatalogoSedesDAO.Fila> leer(byte[] bytes){
        if(bytes==null||bytes.length==0||bytes.length>1_048_576)throw invalido();
        try{
            comprobarZip(bytes);
            try(var libro=new XSSFWorkbook(new ByteArrayInputStream(bytes))){
                if(libro.isMacroEnabled()||!libro.getExternalLinksTables().isEmpty())throw invalido();
                var hoja=libro.getSheet("Sedes");if(hoja==null||hoja.getLastRowNum()>200||hoja.getRow(0)==null||hoja.getRow(0).getLastCellNum()!=5)throw invalido();
                for(int i=0;i<5;i++)if(!CABECERA[i].equals(texto(hoja.getRow(0).getCell(i))))throw invalido();
                var filas=new ArrayList<CatalogoSedesDAO.Fila>();
                for(int n=1;n<=hoja.getLastRowNum();n++){
                    var r=hoja.getRow(n);if(r==null)continue;
                    boolean vacia=true;for(var celda:r)if(celda.getCellType()!=CellType.BLANK){vacia=false;break;}if(vacia)continue;
                    try{if(r.getLastCellNum()>5)throw invalido();var s=new CatalogoSedesDAO.Sede(0,texto(r.getCell(0)),texto(r.getCell(1)),texto(r.getCell(2)),texto(r.getCell(3)),texto(r.getCell(4)));filas.add(new CatalogoSedesDAO.Fila(n+1,CatalogoSedesDAO.validar(s),""));}
                    catch(AgendaException e){filas.add(new CatalogoSedesDAO.Fila(n+1,null,"Revisa los datos y usa texto sin fórmulas ni hipervínculos."));}
                }
                if(filas.isEmpty())throw invalido();return List.copyOf(filas);
            }
        }catch(IOException|RuntimeException e){if(e instanceof AgendaException a)throw a;throw invalido();}
    }
    private static String texto(Cell c){if(c==null||c.getCellType()==CellType.BLANK)return "";if(c.getCellType()!=CellType.STRING||c.getHyperlink()!=null)throw invalido();return c.getStringCellValue().strip();}
    private static void comprobarZip(byte[] bytes)throws IOException{
        int entradas=0,total=0;byte[] buffer=new byte[8192];
        try(var zip=new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))){java.util.zip.ZipEntry e;while((e=zip.getNextEntry())!=null){if(++entradas>100||e.getName().toLowerCase(Locale.ROOT).contains("vbaproject"))throw invalido();int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>8_388_608)throw invalido();}}}
        if(entradas==0)throw invalido();
    }
    private static AgendaException invalido(){return new AgendaException(422,"EXCEL_INVALIDO","Usa la plantilla XLSX de Sedes, con hasta 200 filas y 1 MB; conserva sus cinco columnas y los valores como texto.");}
}
