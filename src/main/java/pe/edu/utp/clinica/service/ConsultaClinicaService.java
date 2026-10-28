package pe.edu.utp.clinica.service;

import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.model.medico.ConsultaClinica;

public final class ConsultaClinicaService {
    private ConsultaClinicaService() {}

    public static ConsultaClinica validar(ConsultaClinica consulta) {
        if (consulta == null) throw invalida("Completa el registro clínico.");
        return new ConsultaClinica(
                texto(consulta.motivo(), "motivo de consulta", 1000, true),
                texto(consulta.antecedentes(), "antecedentes", 2000, false),
                texto(consulta.hallazgos(), "hallazgos", 2000, false),
                texto(consulta.evaluacion(), "evaluación", 2000, true),
                texto(consulta.diagnostico(), "diagnóstico", 1000, true),
                texto(consulta.plan(), "plan", 2000, true));
    }

    private static String texto(String valor, String campo, int maximo, boolean obligatorio) {
        String texto = valor == null ? "" : valor.strip();
        if ((obligatorio && texto.isEmpty()) || texto.length() > maximo
                || texto.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
            throw invalida("Revisa " + campo + ": " + (obligatorio ? "es obligatorio y admite" : "admite") + " hasta " + maximo + " caracteres.");
        }
        return texto;
    }

    private static AgendaException invalida(String mensaje) {
        return new AgendaException(400, "CONSULTA_CLINICA_INVALIDA", mensaje);
    }
}
