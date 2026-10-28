package pe.edu.utp.clinica.service;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.medico.ConsultaClinica;
import static org.junit.jupiter.api.Assertions.*;

class ConsultaClinicaServiceTest {
    @Test void requiresClinicalContentWithoutInventingOptionalFields() {
        var consulta=ConsultaClinicaService.validar(new ConsultaClinica(" Motivo ",null,null,"Evaluación","Diagnóstico","Plan"));
        assertEquals("Motivo",consulta.motivo());assertEquals("",consulta.antecedentes());assertEquals("",consulta.hallazgos());
        assertEquals(400,assertThrows(AgendaException.class,()->ConsultaClinicaService.validar(new ConsultaClinica(" ","","","","",""))).status());
    }
    @Test void preservesClinicalTextButRejectsOversizedValuesAndControls() {
        String texto="Evolución: niño con tos. <control> & revisión\nSegunda línea.";
        var consulta=ConsultaClinicaService.validar(new ConsultaClinica(texto,"","","Evaluación","Diagnóstico","Plan"));
        assertEquals(texto,consulta.motivo());
        assertThrows(AgendaException.class,()->ConsultaClinicaService.validar(new ConsultaClinica("x".repeat(1001),"","","Evaluación","Diagnóstico","Plan")));
        assertThrows(AgendaException.class,()->ConsultaClinicaService.validar(new ConsultaClinica("Motivo","","","Evaluación","Diagnóstico","Plan\u0000")));
    }
}
