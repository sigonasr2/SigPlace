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
    static int PORT = 8080;

    final static HashMap<String,String> map = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>("$SITENAME", "SigPlace"),
        new AbstractMap.SimpleEntry<>("$SITE_BACKCOL", "#111"),
        new AbstractMap.SimpleEntry<>("$TITLE_CONTENT_START", "<div class=\"contentWrapper\"><h1>"),
        new AbstractMap.SimpleEntry<>("$TITLE_CONTENT_END", "</h1><div class=\"content\">"),
        new AbstractMap.SimpleEntry<>("$CONTENT_END", "</div>"),
        new AbstractMap.SimpleEntry<>("$DATE_CONTENT_START", "<div class=\"datebar\"></div><div class=\"date\">")
    ));
    final static HashMap<String,Path> ops = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>(
            "%DEFAULT", Paths.get(REFDIR,"DEFAULT.html")),
        new AbstractMap.SimpleEntry<>(
            "%FOOTER", Paths.get(REFDIR,"FOOTER.html"))
    ));
    public static void main(String[] args) {

        if (args.length>0&&args.length%2==0) {
            int i=0;
            while (i<args.length) {
                String arg1=args[i];
                String arg2=args[i+1];
                i+=2;
                if (arg1.equals("-p")) {
                    PORT=Integer.parseInt(arg2);
                    System.out.println("Port set to "+PORT+".");
                } else {
                    System.err.println("Invalid argument \""+arg1+"\".");
                    return;
                }
            }
        }

        Set<Path> files = GetFilesInDir(ROOTDIR);
        for (Path f : files) {

            System.out.println(" Found "+f.getFileName());

            try {

                System.out.println("  Preparing "+f.getFileName());

                List<String> content = Files.readAllLines(f);
                if (isHTMLFile(f)) {
                    content.addAll(0,Files.readAllLines(ops.get("%DEFAULT")));
                    content.addAll(Files.readAllLines(ops.get("%FOOTER")));
                }

                System.out.println("  Parsing "+f.getFileName());
                for (int i=0;i<content.size();i++) {
                    String s = content.get(i);
                    if (s.length()>0&&isHTMLFile(f)) {
                        //Check for markdown pieces.
                        if (s.charAt(0)=='-') {
                            //Start of a title piece.
                            s=s.replace("-",map.get("$TITLE_CONTENT_START"));
                            s=s+map.get("$TITLE_CONTENT_END");
                        } else
                        if (s.contains("===")) {
                            s=map.get("$CONTENT_END")+map.get("$DATE_CONTENT_START")+s.replace("===","")+map.get("$CONTENT_END")+map.get("$CONTENT_END");
                        }
                    }
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

        ExportCodeFile();

        System.out.println("\nStarting web server...");
        new sigServer();
    }
    private static boolean isHTMLFile(Path f) {
        return f.getFileName().toString().contains(".html");
    }
    private static void ExportCodeFile() {
        try {
            Path file = Paths.get("sigServer.java");
            List<String> data = Files.readAllLines(file);
            int i=0;
            while (!data.get(i++).contains("sigServer()")&&i<data.size());
            if (i<data.size()) {
                Files.write(Paths.get(OUTDIR,"codeBackground"),data.subList(i, Math.min(i+40,data.size())),Charset.defaultCharset(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            e.printStackTrace();
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