package pe.edu.utp.clinica;
import org.junit.jupiter.api.Test;
import pe.edu.utp.clinica.util.SecurityUtil;
public class HashTest {
    @Test
    public void printHash() {
        System.out.println("HASH_GENERADO: " + SecurityUtil.hashPassword("Password123!"));
    }
}
