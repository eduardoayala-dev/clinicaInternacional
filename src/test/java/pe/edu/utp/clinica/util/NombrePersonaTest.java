package pe.edu.utp.clinica.util;

import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.model.Usuario;
import static org.junit.jupiter.api.Assertions.*;

class NombrePersonaTest {
    @Test void formatsEachNamePreservingAccentsAndEnye() {
        assertEquals("Carlos Eduardo", NombrePersona.formatear("CARLOS EDUARDO"));
        assertEquals("Álvaro Íñigo Muñoz", NombrePersona.formatear("ÁLVARO ÍÑIGO MUÑOZ"));
        assertEquals("José", NombrePersona.formatear("JOSE\u0301"));
    }
    @Test void respectsParticlesAndCompoundSurnames() {
        assertEquals("María del Carmen", NombrePersona.formatear("MARÍA DEL CARMEN"));
        assertEquals("De la Cruz", NombrePersona.formatear("DE LA CRUZ"));
        assertEquals("Ana-María O’Neill D'Angelo", NombrePersona.formatear("ANA-MARÍA O’NEILL D'ANGELO"));
    }
    @Test void preservesExistingDistinctiveCapitalization() {
        assertEquals("McDonald DeLeón", NombrePersona.formatear("McDonald DeLeón"));
        assertEquals("María José", NombrePersona.formatear("María José"));
    }
    @Test void handlesEmptyAndWhitespaceWithoutAddingNames() {
        assertNull(NombrePersona.formatear(null));
        assertEquals("", NombrePersona.formatear(" \t "));
        assertEquals("Juan Pérez", NombrePersona.formatear("  JUAN\u00a0\tPÉREZ  "));
    }
    @Test void formatsLoadedAccountsAndValuesUsedForNewRegistrations() {
        var loaded = new Usuario(1, "CARLOS EDUARDO", "DE LA CRUZ", "p@test", "", "PACIENTE", null, true);
        assertEquals("Carlos Eduardo De la Cruz", loaded.getNombreCompleto());
        var registered = new Usuario();
        registered.setNombres("MARÍA DEL CARMEN"); registered.setApellidos("MUÑOZ PÉREZ");
        assertEquals("María del Carmen", registered.getNombres());
        assertEquals("Muñoz Pérez", registered.getApellidos());
    }
}
