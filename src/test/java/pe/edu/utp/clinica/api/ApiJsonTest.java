package pe.edu.utp.clinica.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.medico.PacienteResumen;
import pe.edu.utp.clinica.model.medico.Pagina;
import pe.edu.utp.clinica.support.HttpExchange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApiJsonTest {
    @Test void typedClinicalDatesProduceCompleteIsoJsonInsteadOfPartialResponses() throws Exception {
        var x=new HttpExchange();
        var paciente=new PacienteResumen(1,"Niño Pérez","12345678","",LocalDate.of(2000,1,2),LocalDateTime.of(2026,10,28,10,30),1);
        ApiJson.write(x.response,200,new ApiResponse<>("PACIENTES_MEDICO_OBTENIDOS","Consulta completada.",Map.of("pacientes",new Pagina<>(List.of(paciente),1,20,false))));
        var data=new ObjectMapper().readTree(x.body.toString()).get("data").get("pacientes").get("items").get(0);
        assertEquals("2000-01-02",data.get("nacimiento").asText());
        assertEquals("2026-10-28T10:30:00",data.get("ultimaAtencion").asText());
        assertEquals("Niño Pérez",data.get("nombre").asText());assertEquals(200,x.status);
    }
}
