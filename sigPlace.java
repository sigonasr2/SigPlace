import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class sigPlace {

    final static String ROOTDIR = "sitefiles";
    final static String OUTDIR = "out";

    final static HashMap<String,String> map = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>("$SITENAME", "SigPlace")
    ));
    final static HashMap<String,String> ops = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>(
            "%DEFAULT", 
                "<!DOCTYPE html>"+
                "<html>"+
                "<head>"+
                "<link rel=\"stylesheet\" href=\"sig.css\">"+
                "</head>")
    ));
    public static void main(String[] args) {
        Set<Path> files = GetFilesInDir(ROOTDIR);
        for (Path f : files) {

            System.out.println("Found "+f.getFileName());

            try {

                System.out.println("Preparing "+f.getFileName());

                List<String> content = Files.readAllLines(f);
                content.add(0,ops.get("%DEFAULT"));
                Path newf = Paths.get(OUTDIR,f.getFileName().toString());

                System.out.println("Writing to "+newf);

                Files.write(newf, content, Charset.defaultCharset(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)
                
                System.out.println(newf.getFileName() + " conversion complete!");

            } catch (IOException e) {
                e.printStackTrace();
            }
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