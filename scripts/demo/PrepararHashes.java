import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import pe.edu.utp.clinica.util.SecurityUtil;

class PrepararHashes {
    public static void main(String[] args) throws Exception {
        String password = System.getenv("DEMO_ADMIN_PASSWORD");
        if (args.length != 1 || password == null || password.length() < 12) {
            throw new IllegalArgumentException("Indica el archivo de salida y DEMO_ADMIN_PASSWORD de al menos 12 caracteres.");
        }
        byte[] aleatorio = new byte[32];
        new SecureRandom().nextBytes(aleatorio);
        String admin = SecurityUtil.hashPassword(password);
        String admision = SecurityUtil.hashPassword(password);
        String simulado = SecurityUtil.hashPassword(Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio));
        if (!SecurityUtil.checkPassword(password, admin) || !SecurityUtil.checkPassword(password, admision)) {
            throw new IllegalStateException("No se pudo verificar el hash de las cuentas de gestión.");
        }
        Files.writeString(Path.of(args[0]), "{\"admin\":\"" + admin + "\",\"admision\":\"" + admision + "\",\"simulado\":\"" + simulado + "\"}");
        System.out.println("Hashes generados y verificados; contraseña en claro no guardada.");
    }
}
