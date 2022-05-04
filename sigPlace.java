import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class sigPlace {

    final static String ROOTDIR = "sitefiles";
    final static String REFDIR = "ref";
    final static String OUTDIR = "out";
    final static String DIRECTORYLISTING_FILENAME = "DIRECTORY_LISTING";
    static int PORT = 8080;

    final static HashMap<String,String> map = new HashMap<>(Map.ofEntries(
        new AbstractMap.SimpleEntry<>("$SITENAME", "SigPlace"),
        new AbstractMap.SimpleEntry<>("$SITE_BACKCOL", "#111"),
        new AbstractMap.SimpleEntry<>("$TITLE_CONTENT_START", "<div class=\"contentWrapper\"><h1>"),
        new AbstractMap.SimpleEntry<>("$TITLE_CONTENT_END", "</h1><div class=\"content\" %ID%>"),
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

        System.out.println("Copying files over to output directory...");
        try {
            copyDirectory("sitefiles","out");
            Iterator<Path> items = Files.walk(Paths.get("out")).iterator();
            ParseArticleFiles(items);

            items = Files.walk(Paths.get("out")).iterator();
            ConvertArticleReferences(items);
        }catch (IOException e) {
            e.printStackTrace();
            System.err.println("Copying files over failed!");
            return;
        }

        System.out.println("Building directory listings...");
        try {
            buildDirectoryListings();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to build directory listings!");
            return;
        }

        System.out.println("Site has been built into the "+OUTDIR+" directory.");

        ExportCodeFile();

        System.out.println("\nStarting web server...");
        new sigServer();
    }
    private static void ParseArticleFiles(Iterator<Path> items) {
        while (items.hasNext()) {
            Path f = items.next();
            System.out.println(" Found "+f.getFileName());
            if (Files.isRegularFile(f)) {
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
                        if (s.length()>0&&isArticleFile(f)) {
                            //Check for markdown pieces.
                            if (s.charAt(0)=='-') {
                                //Start of a title piece.
                                s=s.replace("-",map.get("$TITLE_CONTENT_START"));
                                s=s+map.get("$TITLE_CONTENT_END").replace("%ID%","id=\"content_"+i+"\"");
                                //Use ⤈ if there's more text to be shown than can fit.
                            } else
                            if (s.contains("===")) {
                                s=map.get("$CONTENT_END")+map.get("$DATE_CONTENT_START")+s.replace("===","")+map.get("$CONTENT_END")+"<div class=\"unexpanded\" id=\"expand_"+i+"\" onClick=\"expand("+i+")\"><br/><br/><br/><br/>&#x2908; Click to expand.</div>"+map.get("$CONTENT_END");
                            }
                        }
                        for (String key : map.keySet()) {
                            s=s.replaceAll(Pattern.quote(key),map.get(key));
                        }
                        content.set(i,s);
                    }

                    System.out.println("  Writing to "+f.toAbsolutePath());

                    Files.write(f, content, Charset.defaultCharset(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
                    
                    System.out.println(" "+f.getFileName() + " conversion complete!");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static void ConvertArticleReferences(Iterator<Path> items) {
        while (items.hasNext()) {
            Path f = items.next();
            System.out.println(" Looking for Article References..."+f.getFileName());
            if (Files.isRegularFile(f)&&isHTMLFile(f)) {
                System.out.println("  Searching "+f.getFileName());
                try {
                    List<String> content = Files.readAllLines(f);
                    for (int i=0;i<content.size();i++) {
                        String s = content.get(i);
                        if (s.length()>0&&s.contains("$ARTICLE_PREVIEW")) {
                            String article = s.replace("$ARTICLE_PREVIEW ","");
                            System.out.println("   Found article preview request in "+f.getFileName()+" for article "+article+".");
                            Path file = Paths.get(OUTDIR,article);
                            content.remove(i--);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) 
    throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation))
        .forEach(source -> {
            Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                .substring(sourceDirectoryLocation.length()));
            try {
                if (Files.isDirectory(destination)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES,StandardCopyOption.REPLACE_EXISTING});
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    static void buildDirectoryListings() 
    throws IOException {
        String startingPath=Paths.get(sigPlace.OUTDIR).toAbsolutePath().toString();
        HashMap<String,List<Path>> map = new HashMap<>();
        Iterator<Path> it = Files.walk(Paths.get(sigPlace.OUTDIR)).iterator();
        map.put("/",new ArrayList<Path>());
        while (it.hasNext()) {
            Path f = it.next();
            if (!f.getFileName().toString().equals(OUTDIR)) {
                String myKey = f.toAbsolutePath().toString().replace(startingPath,"").replace(f.getFileName().toString(),"");
                //System.out.println(myKey+","+f);
                map.putIfAbsent(myKey,new ArrayList<Path>());
                map.get(myKey).add(f);
            }
        }
        System.out.println("Directory structure determined:");
        System.out.println("    "+map);
        for (String key : map.keySet()) {
            System.out.println("Creating directory listing for "+key+"...");
            StringBuilder sb = new StringBuilder("");
            List<String> data = Files.readAllLines(ops.get("%DEFAULT"));
            List<String> data2 = Files.readAllLines(ops.get("%FOOTER"));
            for (String d : data) {
                for (String k : sigPlace.map.keySet()) {
                    d=d.replaceAll(Pattern.quote(k),sigPlace.map.get(k));
                }
                sb.append(d).append("\n");
            }
            sb.append("<h2>Directory Listing for "+key+"</h2>");
            sb.append("<div class=\"folderlisting\"><a href=\"")
            .append(key)
            .append("..\" class=\"icon\">&#x1F4C1;</a><a href=\"")
            .append(key)
            .append("..\">.. </a><a href=\"")
            .append(key)
            .append("..\" class=\"nounderline\">(Previous Directory)</a></div>");
            for (Path f : map.get(key)) {
                sb.append("<div class=\"").append((Files.isDirectory(f)?"folderlisting":"filelisting")).append("\">")
                .append("<a href=\""+(f.toAbsolutePath().toString().replace(Paths.get(OUTDIR).toAbsolutePath().toString(),""))+"\" class=\"icon\">")
                .append((Files.isDirectory(f)?"&#x1F4C1;":"&#x1F5CE;"))
                .append("</a>")
                .append("<a href=\""+(f.toAbsolutePath().toString().replace(Paths.get(OUTDIR).toAbsolutePath().toString(),""))+"\">")
                .append(f.getFileName())
                .append("</a>\t")
                .append(Files.getLastModifiedTime(f))
                .append("\t")
                .append(Files.getOwner(f))
                .append("\t")
                .append(Files.size(f))
                .append("</div>\n");
            }
            for (String d : data2) {
                for (String k : sigPlace.map.keySet()) {
                    d=d.replaceAll(Pattern.quote(k),sigPlace.map.get(k));
                }
                sb.append(d).append("\n");
            }
            Path newf = Files.write(Paths.get(OUTDIR,key,DIRECTORYLISTING_FILENAME),sb.toString().getBytes());
            System.out.println("  Added info for ("+map.size()+") files to "+newf.toAbsolutePath());
        }
    }
    private static boolean isArticleFile(Path f) {
        return f.getFileName().toString().contains(".article");
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
}