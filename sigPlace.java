import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class sigPlace {

    final static String ROOTDIR = "sitefiles";
    final static String REFDIR = "ref";
    final static String OUTDIR = "out";

    final static HashMap<String,String> map = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>("$SITENAME", "SigPlace")
    ));
    final static HashMap<String,Path> ops = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>(
            "%DEFAULT", Paths.get(REFDIR,"DEFAULT.html")),
        new AbstractMap.SimpleEntry<>(
            "%FOOTER", Paths.get(REFDIR,"FOOTER.html"))
    ));
    public static void main(String[] args) {
        Set<Path> files = GetFilesInDir(ROOTDIR);
        for (Path f : files) {

            System.out.println(" Found "+f.getFileName());

            try {

                System.out.println("  Preparing "+f.getFileName());

                List<String> content = Files.readAllLines(f);
                content.addAll(0,Files.readAllLines(ops.get("%DEFAULT")));
                content.addAll(Files.readAllLines(ops.get("%FOOTER")));

                System.out.println("  Parsing "+f.getFileName());
                for (int i=0;i<content.size();i++) {
                    String s = content.get(i);
                    for (String key : map.keySet()) {
                        s=s.replaceAll(Pattern.quote(key),map.get(key));
                    }
                    content.set(i,s);
                }

                Path newf = Paths.get(OUTDIR,f.getFileName().toString());

                System.out.println("  Writing to "+newf);

                Files.write(newf, content, Charset.defaultCharset(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
                
                System.out.println(" "+newf.getFileName() + " conversion complete!");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Site has been built into the "+OUTDIR+" directory.");
        System.out.println("\nStarting web server...");
        new sigServer();
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