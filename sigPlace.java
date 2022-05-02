import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class sigPlace {
    final static HashMap<String,String> map = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>("$SITENAME", "SigPlace")
    ));
    public static void main(String[] args) {
        Set<Path> files = GetFilesInDir("sitefiles");
        for (Path f : files) {
            System.out.println(f.getFileName());
        }
    }
    private static Set<Path> GetFilesInDir(String directory) {
        Path dir = Paths.get(directory);
        try {
            return Files.list(dir).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}