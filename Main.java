import org.mindrot.jbcrypt.BCrypt;
public class Main {
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("12345678", BCrypt.gensalt(10)));
    }
}
