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
    final static String ARTICLESDIR = "articles";
    final static String UPLOADSDIR = "uploads";
    final static String COMMENTSDIR = "comments";
    final static String DIRECTORYLISTING_FILENAME = "DIRECTORY_LISTING";
    static int PORT = 8080;
    static String SECRET = "";

    static double COLOR_ROTATION = 0;

    static boolean inCodeBlock = false;
    static String storedCodeBlock = "";

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
            "%NAVBAR", Paths.get(REFDIR,"NAVBAR.html")),
        new AbstractMap.SimpleEntry<>(
            "%FOOTER", Paths.get(REFDIR,"FOOTER.html"))
    ));
    public static void main(String[] args) {

        Path secretFile = Paths.get(".clientsecret");
        List<String> data;
		try {
			data = Files.readAllLines(secretFile);
            SECRET = data.get(0);
		} catch (IOException e1) {
			System.out.println("Client secret must be included in .clientsecret!");
			e1.printStackTrace();
		}

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
            Iterator<Path> items = Files.walk(Paths.get("out")).filter((p)->!p.toAbsolutePath().toString().contains("images/")).iterator();
            ParseArticleFiles(items);

            items = Files.walk(Paths.get("out")).iterator();
            ConvertArticleReferences(items);
            items = Files.walk(Paths.get("out","articles")).iterator();
            GenerateArticleFiles(items);
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
    

    private static String JSON(HashMap<String, Object> testMap) {
        StringBuilder sb = new StringBuilder();
        String temp = testMap.toString();
        if (temp.charAt(0)=='{') {
            sb.append("{");
            int marker=1;
            boolean ending=false;
            while (marker<temp.length()) { 
                if (!ending&&temp.charAt(marker)!=' '&&temp.charAt(marker)!='{'&&temp.charAt(marker)!='}'&&temp.charAt(marker)!=',') {
                    ending=true;
                    sb.append("\"");
                } else 
                if (ending&&(temp.charAt(marker)=='='||temp.charAt(marker)==','||temp.charAt(marker)=='}')) {
                    ending=false;
                    sb.append("\"");
                }
                if (!ending&&temp.charAt(marker)=='=') {
                    sb.append(':');
                } else {
                    sb.append(temp.charAt(marker));
                }
                marker++;
            }
        } else {
            throw new UnsupportedOperationException("Not valid JSON!");
        }
        return sb.toString();
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
                        content.addAll(0,Files.readAllLines(ops.get("%NAVBAR")));
                        content.addAll(0,Files.readAllLines(ops.get("%DEFAULT")));
                        content.addAll(Files.readAllLines(ops.get("%FOOTER")));
                    }

                    System.out.println("  Parsing "+f.getFileName());
                    for (int i=0;i<content.size();i++) {
                        String s = content.get(i);
                        boolean endPreLine=false;
                        //System.out.println(s);
                        if (s.length()>0&&(isHTMLFile(f)||isArticleFile(f))) {
                            if (!inCodeBlock) {
                                if (s.trim().equals("<pre>")) {
                                    //System.out.println("Inside <pre>");
                                    inCodeBlock=true;
                                    storedCodeBlock="";
                                    s=s.substring(0,s.indexOf("<pre>"));
                                }
                            }
                            if (inCodeBlock&&s.trim().equals("</pre>")) {
                                inCodeBlock=false;
                                boolean keyword=false;
                                boolean inString=false;
                                boolean inComment=false;
                                boolean inMultiLineComment=false;
                                char stringChar=' ';
                                boolean canBeNumericalConstant=false;
                                int lengthOfConstant=0;
                                storedCodeBlock+=s.substring(0,s.indexOf("</pre>"));
                                storedCodeBlock=storedCodeBlock.replaceAll(Pattern.quote("<"),"\2");
                                storedCodeBlock+="</pre>";
                                int startPos=0;
                                String endText=s.substring(s.indexOf("</pre>")+"</pre>".length(),s.length());
                                s="";
                                for (int j=0;j<storedCodeBlock.length();j++) {
                                    if (storedCodeBlock.charAt(j)=='\n'&&inString) {
                                        inString=false;
                                        s+="</span>";
                                    } else 
                                    if (storedCodeBlock.charAt(j)=='\n'&&inComment) {
                                        inComment=false;
                                        s+=SPAN("comment")+storedCodeBlock.substring(startPos,j)+"</span>";
                                        startPos=j+1;
                                    }
                                    if (!inComment&&!inMultiLineComment&&(j>0&&storedCodeBlock.charAt(j-1)!='\\'&&(!inString&&(storedCodeBlock.charAt(j)=='"'||storedCodeBlock.charAt(j)=='\'')||inString&&(storedCodeBlock.charAt(j)==stringChar)))) {
                                        inString=!inString;
                                        if (inString) {
                                            stringChar=storedCodeBlock.charAt(j);
                                            s+=SPAN("string")+stringChar;
                                        } else {
                                            s+=stringChar;
                                            s+="</span>";
                                            startPos=j+1;
                                        }
                                    } else
                                    if (!inString) {
                                        if (canBeNumericalConstant&&validNumericalConstantCharacters(lengthOfConstant, j)) {
                                            lengthOfConstant++;
                                            //System.out.println("Length of Constant now "+lengthOfConstant);
                                        }
                                        if (j>0&&storedCodeBlock.charAt(j)=='/'&&storedCodeBlock.charAt(j+1)=='*'||inMultiLineComment) {
                                            if (!inMultiLineComment) {
                                                inMultiLineComment=true;
                                            } else {
                                                if (storedCodeBlock.charAt(j-1)=='*'&&storedCodeBlock.charAt(j)=='/') {
                                                    inMultiLineComment=false;
                                                    s+=SPAN("comment")+storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j)+"</span>";
                                                    startPos=j+1;
                                                }
                                            }
                                            //Stops further execution since we're in a comment.
                                        } else
                                        if (j>0&&storedCodeBlock.charAt(j)=='/'&&storedCodeBlock.charAt(j+1)=='/'||inComment) {
                                            if (!inComment) {
                                                inComment=true;
                                            }
                                            //Stops further execution since we're in a comment.
                                        } else
                                        if (canBeNumericalConstant&&lengthOfConstant>0&&!(validNumericalConstantCharacters(lengthOfConstant, j))) {
                                            s+=SPAN("number")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                            //System.out.println("Setting "+storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j));
                                            lengthOfConstant=0;
                                            canBeNumericalConstant=false;
                                            startPos=j+1;
                                        } else
                                        if (!canBeNumericalConstant&&storedCodeBlock.charAt(j)=='.') {
                                            //Previous section was a member.
                                            s+=SPAN("class")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                            startPos=j+1;
                                        } else 
                                        if (j>3&&storedCodeBlock.substring(j-3,j+1).equals("true")&&!isAlphanumeric(j-4)&&!isAlphanumeric(j+1)) {
                                            s+=SPAN("number")+storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j)+"</span>";
                                            startPos=j+1;
                                        } else 
                                        if (j>4&&storedCodeBlock.substring(j-4,j+1).equals("false")&&!isAlphanumeric(j-5)&&!isAlphanumeric(j+1)) {
                                            s+=SPAN("number")+storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j)+"</span>";
                                            startPos=j+1;
                                        } else 
                                        if (storedCodeBlock.charAt(j)=='(') {
                                            s+=SPAN("function")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                            startPos=j+1;
                                        } else 
                                        if (j>0&&isAlphanumeric(j-1) && storedCodeBlock.charAt(j)==' '&&storedCodeBlock.charAt(j-1)!=' ') {
                                            //Previous section was a keyword.
                                            keyword=true;
                                            s+=SPAN("keyword")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                            startPos=j+1;
                                        } else 
                                        if (j>0&&isAlphanumeric(j-1) && (storedCodeBlock.charAt(j)==';'||storedCodeBlock.charAt(j)==':')) {
                                            //Previous section was a keyword.
                                            //keyword=true;
                                            s+=SPAN("keyword")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                            startPos=j+1;
                                        } else 
                                        if (keyword&&!(storedCodeBlock.charAt(j)=='_'||storedCodeBlock.charAt(j)>='0'&&storedCodeBlock.charAt(j)<='9'||storedCodeBlock.charAt(j)>='A'&&storedCodeBlock.charAt(j)<='Z'||storedCodeBlock.charAt(j)>='a'&&storedCodeBlock.charAt(j)<='z'||storedCodeBlock.charAt(j)==' ')) {
                                            keyword=false;
                                            s+=SPAN("variable")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                            startPos=j+1;
                                        } else
                                        if (!isAlphanumeric(j)){
                                            if (startPos<j) {
                                                s+=storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j);
                                            } else {
                                                s+=storedCodeBlock.charAt(j);
                                            }
                                            startPos=j+1;
                                        }
                                    } else {
                                        s+=storedCodeBlock.charAt(j);
                                        startPos=j+1;
                                    }
                                    if (canBeNumericalConstant&&lengthOfConstant==0&&!(storedCodeBlock.charAt(j)>='0'&&storedCodeBlock.charAt(j)<='9')) {
                                        canBeNumericalConstant=false;
                                    }
                                    if (!canBeNumericalConstant&&!isAlphanumeric(j)) {
                                        canBeNumericalConstant=true;
                                        lengthOfConstant=0;
                                        //System.out.println("Found "+storedCodeBlock.charAt(j)+", can be numeric...");
                                    }
                                }
                                for (int j=0;j<s.length();j++) {
                                    if (s.charAt(j)=='\2') {
                                        s=s.substring(0,j)+"&lt;"+s.substring(j+1,s.length());
                                    }
                                }
                                s="<pre>"+s;
                                s+=endText;
                                endPreLine=true;
                                //System.out.println("Stored code block: "+storedCodeBlock);
                            } else 
                            if (inCodeBlock) {
                                storedCodeBlock+=s+"\n";
                                s=" ";
                            }
                        }
                        if (s.length()>0&&isArticleFile(f)&&!inCodeBlock) {
                            //Check for markdown pieces.
                            if (s.charAt(0)=='-') {
                                //Start of a title piece.
                                s=s.replace("-",map.get("$TITLE_CONTENT_START"));
                                s=s+map.get("$TITLE_CONTENT_END").replace("%ID%","id=\"content_"+f+"\"");
                                //Use ⤈ if there's more text to be shown than can fit.
                            } else
                            if (s.startsWith("===")) {
                                s=map.get("$CONTENT_END")+map.get("$DATE_CONTENT_START")+s.replace("===","")+map.get("$CONTENT_END")+"%CONDITIONAL_EXPAND%"+map.get("$CONTENT_END");
                            } else 
                            if (s.charAt(0)==':') {
                                //Image with caption.
                                //Format:
                                //:<url>,<left|right|center>,<width>,<caption>
                                String[] splitter = s.split(Pattern.quote(","));
                                StringBuilder captionText = new StringBuilder(splitter[3]);
                                for (int j=4;j<splitter.length;j++) {
                                    captionText.append(",").append(splitter[j]);
                                }
                                s="<div><figure style=\"text-align:center;"+((splitter[1].equals("left")||splitter[1].equals("right"))?"width:"+splitter[2]+"%;float:"+splitter[1]+";":"")+"\"><img src=\"/"+splitter[0].substring(1)+"\" style=\"margin:auto;width:100%;\"><figcaption>"+captionText.toString()+"</figcaption></figure></div>";
                            } else {
                                //It's regular content, so add paragraphs.
                                s="<p class=\"color"+(((int)(COLOR_ROTATION=(COLOR_ROTATION+0.4)%6))+1)+"\">\n"+s+"\n</p>";
                            }
                        } else {
                            if (s.length()==0&&isArticleFile(f)&&!inCodeBlock) {
                                s="<br/>"; //Setup a line break here.
                            }
                        }
                        if (!endPreLine) {
                            for (String key : map.keySet()) {
                                s=s.replaceAll(Pattern.quote(key),map.get(key));
                            }
                        }
                        if (s.trim().length()==0) {
                            content.remove(i--);
                        } else {
                            content.set(i,s);
                        }
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
    private static boolean validNumericalConstantCharacters(int lengthOfConstant, int j) {
        return storedCodeBlock.charAt(j)>='0'&&storedCodeBlock.charAt(j)<='9'||lengthOfConstant>0&&storedCodeBlock.charAt(j)=='.'||lengthOfConstant>0&&storedCodeBlock.charAt(j)>='A'&&lengthOfConstant>0&&storedCodeBlock.charAt(j)<='F'||lengthOfConstant>0&&storedCodeBlock.charAt(j)>='a'&&lengthOfConstant>0&&storedCodeBlock.charAt(j)<='f'||lengthOfConstant>0&&storedCodeBlock.charAt(j)=='x'||lengthOfConstant>0&&storedCodeBlock.charAt(j)=='X';
    }
    private static boolean isAlphanumeric(int j) {
        return storedCodeBlock.charAt(j)>='0'&&storedCodeBlock.charAt(j)<='9'||storedCodeBlock.charAt(j)>='A'&&storedCodeBlock.charAt(j)<='Z'||storedCodeBlock.charAt(j)>='a'&&storedCodeBlock.charAt(j)<='z';
    }
    /**
     * Writes a span tag with the included class.
     * **/
    private static String SPAN(String className) {
        return "<span class=\""+className+"\">";
    }
    private static void GenerateArticleFiles(Iterator<Path> items){
        System.out.println(" Generating article files...");
        while (items.hasNext()) {
            Path f = items.next();
            try {
                if (Files.isRegularFile(f)&&isArticleFile(f)) {
                    boolean inCodeBlock = false;
                    System.out.println("  Creating article for "+f.getFileName());
                    List<String> content = Files.readAllLines(f);
                    List<String> preContent = Files.readAllLines(ops.get("%DEFAULT"));
                    preContent.addAll(Files.readAllLines(ops.get("%NAVBAR")));
                    List<String> postContent = Files.readAllLines(ops.get("%FOOTER"));
                    StringBuilder sb = new StringBuilder();
                    for (String d : preContent) {
                        for (String k : sigPlace.map.keySet()) {
                            d=d.replaceAll(Pattern.quote(k),sigPlace.map.get(k));
                        }
                        sb.append(d).append("\n");
                    }
                    int lineNumb=0;
                    for (String d : content) {
                        lineNumb++;
                        if (d.trim().equals("<pre>")) {
                            System.out.println("<pre> in "+f+" Line "+lineNumb+": "+d);
                            inCodeBlock=true;
                        }
                        if (inCodeBlock&&d.trim().equals("</pre>")) {
                            inCodeBlock=false;
                        }
                        if (!inCodeBlock) {
                            for (String k : sigPlace.map.keySet()) {
                                d=d.replaceAll(Pattern.quote(k),sigPlace.map.get(k));
                            }
                            d=d.replaceFirst("div class=\"content\"","div class=\"expandedContent\"");
                            d=d.replaceFirst("%CONDITIONAL_EXPAND%","");
                        }

                        sb.append(d).append("\n");
                    }
                        
                    System.out.println("  Generating comment section for "+f+".");

                    Path ff = Paths.get(REFDIR,"COMMENT.html");
                    List<String> commentHTML = Files.readAllLines(ff);
                    for (int i=0;i<commentHTML.size();i++) {
                        if (commentHTML.get(i).contains("$ARTICLE")) {
                            commentHTML.set(i,commentHTML.get(i).replace("$ARTICLE",f.getFileName().toString()).replace(".article",""));
                        }
                        sb.append(commentHTML.get(i)).append("\n");
                    }
                    for (String d : postContent) {
                        for (String k : sigPlace.map.keySet()) {
                            d=d.replaceAll(Pattern.quote(k),sigPlace.map.get(k));
                        }
                        sb.append(d).append("\n");
                    }
                    Files.write(Paths.get(f.getParent().toString(),f.getFileName()+".html"),sb.toString().getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void ConvertArticleReferences(Iterator<Path> items) {
        while (items.hasNext()) {
            Path f = items.next();
            System.out.println(" Looking for Article References..."+f.getFileName());
            boolean articleJavascriptIncluded=false;
            if (Files.isRegularFile(f)&&isHTMLFile(f)) {
                System.out.println("  Searching "+f.getFileName());
                try {
                    List<String> content = Files.readAllLines(f);
                    for (int i=0;i<content.size();i++) {
                        String s = content.get(i);
                        if (s.length()>0&&s.contains("$ARTICLE_PREVIEW")) {
                            String article = ARTICLESDIR+"/"+s.replace("$ARTICLE_PREVIEW ","")+".article";
                            if (Files.exists(Paths.get(OUTDIR,article))) {
                                System.out.println("   Found article preview request in "+f.getFileName()+" for article "+article+".");
                                Path file = Paths.get(OUTDIR,article);
                                List<String> newData = Files.readAllLines(file);
                                if (newData.size()>0) {
                                    content.set(i,newData.get(0)
                                    .replace("<h1>","<a title=\"Click to go to the original article and to view comments!\" class=\"reallink\" href=\""+article+".html\"><h1>")
                                    .replace("</h1>","</h1></a><a title=\"Click to go to the original article and to view comments!\" class=\"reallink\" href=\""+article+".html\">🔗</a>"));
                                    for (int j=1;j<newData.size();j++) {
                                        content.add(i+j, newData.get(j));
                                    }
                                    String lastline=content.get(i+newData.size()-1);
                                    lastline=lastline.replace("%CONDITIONAL_EXPAND%","<div class=\"unexpanded\" id=\"expand_"+i+"\" onClick=\"expand(this,'"+Paths.get(OUTDIR,article.toString())+"')\"><br/><br/><br/><br/>&#x2908; Click to expand.</div>");
                                    content.set(i+newData.size()-1,lastline);//<div class=\"unexpanded\" id=\"expand_"+i+"\" onClick=\"expand("+i+")\"><br/><br/><br/><br/>&#x2908; Click to expand.</div>");
                                } else {
                                    content.set(i,"");
                                }
                                if (!articleJavascriptIncluded) {
                                    List<String> articlejs = Files.readAllLines(Paths.get(REFDIR,"article.js"));
                                    for (int j=articlejs.size()-1;j>=0;j--) {
                                        content.add(i,articlejs.get(j));
                                    }
                                    articleJavascriptIncluded=true;
                                }
                            }
                        }
                    }
                    Files.write(f,content);
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