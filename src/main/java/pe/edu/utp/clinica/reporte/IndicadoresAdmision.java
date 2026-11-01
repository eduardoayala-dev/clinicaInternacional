package pe.edu.utp.clinica.reporte;

import pe.edu.utp.clinica.dao.ReporteAdmisionDAO;
import java.util.List;
import java.util.Locale;

public final class IndicadoresAdmision {
    public record Indicador(String nombre,String valor,String unidad,String alcance) {}
    private IndicadoresAdmision() {}
    public static List<Indicador> obtener(ReporteAdmisionDAO.Resumen r) {
        String filtradas="Citas del período, estado y sede seleccionados";
        String activas="Citas activas del período, estado y sede seleccionados";
        String ofertas="Ofertas sobre cupos cuya cita original corresponde al período, estado y sede";
        return List.of(
            numero("Total de citas",r.total(),"citas",filtradas),numero("Atendidas",r.atendidas(),"citas",filtradas),
            numero("En atención",r.enAtencion(),"citas",filtradas),numero("Activas pendientes de cierre",r.pendientes(),"citas",filtradas),
            numero("Canceladas",r.canceladas(),"citas",filtradas),new Indicador("Tasa de cancelación",porcentaje(r.canceladas(),r.total()),"porcentaje",filtradas),
            new Indicador("Espera media registrada",r.esperaMedia()==null?"Sin datos":String.format(Locale.ROOT,"%.1f",r.esperaMedia()),"minutos","Desde llegada hasta inicio de atención, solo registros válidos"),
            numero("Médicos habilitados",r.medicos(),"médicos","Personal activo actualmente en la sede; independiente del período y estado de citas"),
            numero("Ofertas",r.ofertas(),"ofertas",ofertas),numero("Ofertas aceptadas",r.aceptadas(),"ofertas",ofertas),
            new Indicador("Tasa de aceptación",porcentaje(r.aceptadas(),r.ofertas()),"porcentaje",ofertas),
            numero("Riesgo bajo registrado",r.bajo(),"citas",activas),numero("Riesgo medio registrado",r.medio(),"citas",activas),
            numero("Riesgo alto registrado",r.alto(),"citas",activas),numero("Sin clasificación registrada",r.sinRiesgo(),"citas",activas));
    }
    private static Indicador numero(String nombre,long valor,String unidad,String alcance){return new Indicador(nombre,Long.toString(valor),unidad,alcance);}
    private static String porcentaje(long parte,long total){return total==0?"Sin datos":String.format(Locale.ROOT,"%.1f",parte*100.0/total);}
}
