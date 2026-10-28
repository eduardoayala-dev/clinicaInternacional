package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.medico.ConsultaClinica;
import static org.junit.jupiter.api.Assertions.*;

class ConsultaClinicaServiceTest {
    @Test void requiresClinicalContentWithoutInventingOptionalFields() {
        var consulta=ConsultaClinicaService.validar(new ConsultaClinica(" Motivo ",null,null,"Evaluación","Diagnóstico","Plan"));
        assertEquals("Motivo",consulta.motivo());assertEquals("",consulta.antecedentes());assertEquals("",consulta.hallazgos());
        var incompleta = new ConsultaClinica(" ","","","","","");
        assertEquals(400,assertThrows(AgendaException.class,()->ConsultaClinicaService.validar(incompleta)).status());
    }
    @Test void preservesClinicalTextButRejectsOversizedValuesAndControls() {
        String texto="Evolución: niño con tos. <control> & revisión\nSegunda línea.";
        var consulta=ConsultaClinicaService.validar(new ConsultaClinica(texto,"","","Evaluación","Diagnóstico","Plan"));
        assertEquals(texto,consulta.motivo());
        var extensa = new ConsultaClinica("x".repeat(1001),"","","Evaluación","Diagnóstico","Plan");
        assertThrows(AgendaException.class,()->ConsultaClinicaService.validar(extensa));
        var control = new ConsultaClinica("Motivo","","","Evaluación","Diagnóstico","Plan\u0000");
        assertThrows(AgendaException.class,()->ConsultaClinicaService.validar(control));
    }
}
