package pe.edu.utp.clinica.catalogo;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.support.HttpExchange;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.catalogo.FilaImportacionMotivo;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmacionImportacionMotivosTest {
    private final Clock reloj=Clock.fixed(Instant.parse("2026-11-03T17:00:00Z"),ZoneOffset.UTC);
    private final List<FilaImportacionMotivo> filas=List.of(new FilaImportacionMotivo(2,13,"Control general",""));
    @Test void confirmacionEsDeUnSoloUsoYLigadaAlActor(){var s=new HttpExchange().session;var p=ConfirmacionImportacionMotivos.preparar(s,17,filas,reloj);assertThrows(AgendaException.class,()->ConfirmacionImportacionMotivos.consumir(s,18,p.token(),reloj));assertEquals(filas,ConfirmacionImportacionMotivos.consumir(s,17,p.token(),reloj).filas());assertThrows(AgendaException.class,()->ConfirmacionImportacionMotivos.consumir(s,17,p.token(),reloj));}
    @Test void venceEnDiezMinutosYReemplazaLaRevisionAnterior(){var s=new HttpExchange().session;var anterior=ConfirmacionImportacionMotivos.preparar(s,17,filas,reloj);var actual=ConfirmacionImportacionMotivos.preparar(s,17,filas,reloj);assertThrows(AgendaException.class,()->ConfirmacionImportacionMotivos.consumir(s,17,anterior.token(),reloj));assertThrows(AgendaException.class,()->ConfirmacionImportacionMotivos.consumir(s,17,actual.token(),Clock.offset(reloj,Duration.ofMinutes(10))));}
    @Test void descartarInvalidaLaRevisionYNoSePreparanFilasConErrores(){var s=new HttpExchange().session;var p=ConfirmacionImportacionMotivos.preparar(s,17,filas,reloj);ConfirmacionImportacionMotivos.descartar(s);assertThrows(AgendaException.class,()->ConfirmacionImportacionMotivos.consumir(s,17,p.token(),reloj));assertThrows(AgendaException.class,()->ConfirmacionImportacionMotivos.preparar(s,17,List.of(new FilaImportacionMotivo(2,0,"","Error")),reloj));}
}
