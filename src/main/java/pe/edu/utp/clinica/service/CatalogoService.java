package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.api.ApiException;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.api.ParametrosApi;
import pe.edu.utp.clinica.dao.MedicoDAO;
import pe.edu.utp.clinica.model.MedicoCatalogo;
import java.time.LocalDate;
import java.util.List;

public final class CatalogoService {
    private final MedicoDAO dao;

    public CatalogoService() { this(new MedicoDAO()); }
    public CatalogoService(MedicoDAO dao) { this.dao = dao; }

    public ApiResponse<List<MedicoCatalogo>> medicos(String especialidad, String sede) throws ApiException {
        int idEspecialidad = ParametrosApi.positivo(especialidad, "idEspecialidad");
        Integer idSede = ParametrosApi.positivoOpcional(sede, "idSede");
        return new ApiResponse<>("MEDICOS_OBTENIDOS", "Consulta de médicos completada.",
                List.copyOf(dao.consultarMedicos(idEspecialidad, idSede)));
    }

    public ApiResponse<List<String>> horarios(String medico, String fecha) throws ApiException {
        int idMedico = ParametrosApi.positivo(medico, "idMedico");
        LocalDate date = ParametrosApi.fechaOpcional(fecha);
        if (date == null) return new ApiResponse<>("FECHAS_OBTENIDAS", "Consulta de fechas completada.",
                List.copyOf(dao.obtenerFechasDisponibles(idMedico)));
        return new ApiResponse<>("HORARIOS_OBTENIDOS", "Consulta de horarios completada.",
                List.copyOf(dao.obtenerHorariosDisponibles(idMedico, date.toString())));
    }
}
