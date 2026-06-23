import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Test@1234";
        String encodedPassword = "$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2";
        System.out.println("Matches: " + encoder.matches(rawPassword, encodedPassword));
        System.out.println("New Hash: " + encoder.encode(rawPassword));
    }
}
