package pe.edu.utp.clinica.ia;

public record EntradaInasistencia(int inasistenciasPrevias, int atencionesPrevias,
                                 int anticipacionDias, boolean confirmadaPorPaciente) {
    public EntradaInasistencia {
        if (inasistenciasPrevias < 0 || inasistenciasPrevias > 100
                || atencionesPrevias < 0 || atencionesPrevias > 100
                || anticipacionDias < 0 || anticipacionDias > 180) {
            throw new IllegalArgumentException("Usa historiales entre 0 y 100 y una anticipación entre 0 y 180 días.");
        }
    }

    double[] valores() {
        return new double[]{inasistenciasPrevias, atencionesPrevias, anticipacionDias, confirmadaPorPaciente ? 1 : 0};
    }
}
