package pe.edu.utp.clinica.model;

public record PerfilPaciente(int idUsuario, String nombres, String apellidos,
                             String fechaNacimiento, String telefono, String direccion,
                             String canalAviso, String sedePreferida, String alergias, String tipoCobertura) {
    @Override public String toString() { return "PerfilPaciente[privado]"; }
}
