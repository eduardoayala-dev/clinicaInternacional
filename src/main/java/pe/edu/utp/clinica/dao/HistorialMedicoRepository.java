package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.model.medico.HistorialPaciente;
import pe.edu.utp.clinica.model.medico.CitaControl;
import pe.edu.utp.clinica.model.medico.PacienteResumen;
import pe.edu.utp.clinica.model.medico.Pagina;
import pe.edu.utp.clinica.model.medico.SeguimientoResumen;
import java.util.List;

public interface HistorialMedicoRepository {
    Pagina<PacienteResumen> pacientes(int medico, String consulta, int pagina);
    HistorialPaciente historial(int medico, int paciente, int pagina);
    Pagina<SeguimientoResumen> seguimientos(int medico, String consulta, String estado, int pagina);
    SeguimientoResumen seguimiento(int medico, int origen);
    List<CitaControl> controles(int medico, int origen);
    void vincular(int medico, int origen, int control);
}
