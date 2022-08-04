import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ArcadeReader {
    public static String fileToBase64String(Path file) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        } catch(IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
