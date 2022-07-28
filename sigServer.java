import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class sigServer {
    ServerSocket socket;
    
    /**
     * Writes a span tag with the included class.
     * **/
    private static String SPAN(String className) {
        return "<span class=\""+className+"\">";
    }
    private static boolean validNumericalConstantCharacters(String storedCodeBlock,int lengthOfConstant, int j) {
        return storedCodeBlock.charAt(j)>='0'&&storedCodeBlock.charAt(j)<='9'||lengthOfConstant>0&&storedCodeBlock.charAt(j)=='.'||lengthOfConstant>0&&storedCodeBlock.charAt(j)>='A'&&lengthOfConstant>0&&storedCodeBlock.charAt(j)<='F'||lengthOfConstant>0&&storedCodeBlock.charAt(j)>='a'&&lengthOfConstant>0&&storedCodeBlock.charAt(j)<='f'||lengthOfConstant>0&&storedCodeBlock.charAt(j)=='x'||lengthOfConstant>0&&storedCodeBlock.charAt(j)=='X';
    }
    private static boolean isAlphanumeric(String storedCodeBlock,int j) {
        return storedCodeBlock.charAt(j)>='0'&&storedCodeBlock.charAt(j)<='9'||storedCodeBlock.charAt(j)>='A'&&storedCodeBlock.charAt(j)<='Z'||storedCodeBlock.charAt(j)>='a'&&storedCodeBlock.charAt(j)<='z';
    }

    sigServer() {
        try {
            socket = new ServerSocket(sigPlace.PORT);
            System.out.println("Listening on port "+sigPlace.PORT+".");
            while (true) {
                try (Socket client = socket.accept()) {
                    System.out.println("New client connection detected: "+client.toString());
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(),"ISO-8859-1"));
                    String requestLine,line;
                    ZonedDateTime modifiedDate = null;
                    String boundary=null;
                    boolean truncateUntilBoundary=false;
                    String filename=null;
                    OutputStream stream = null;
                    requestLine=in.readLine(); //Read the first line, this should be our request.
                    if (requestLine!=null) {
                        String[] splitter = requestLine.split(Pattern.quote(" "));
                        boolean ISPOST = splitter[0].equals("POST");
                        if (splitter.length==3) {
                            while (in.ready()) {
                                line=in.readLine();
                                if (ISPOST) {
                                    if (boundary!=null) {
                                        if (!truncateUntilBoundary) {
                                            System.out.println(line);
            
                                            if (boundary.length()>0) {
                                                if (line.equals(boundary)) {
                                                    truncateUntilBoundary=true;
                                                }
                                            }
                                        } else
                                        if (line.contains("Content-Disposition: ")||line.contains("Content-Type: ")) {
                                            if (line.contains("filename=")) {
                                                filename=line.substring(line.indexOf("filename=")+"filename=".length()+1);
                                                filename = filename.substring(0,filename.length()-1);
                                            } else {
                                                System.out.println(line);
                                            }
                                        } else {
                                            File myFile = new File(new File(sigPlace.OUTDIR,sigPlace.UPLOADSDIR),filename);
                                            // check if file exist, otherwise create the file before writing
                                            myFile.mkdirs();
                                            if (!myFile.exists()) {
                                                myFile.createNewFile();
                                            } else {
                                                myFile.delete();
                                                myFile.createNewFile();
                                            }
                                            stream = new FileOutputStream(myFile);
                                            char[] buffer = new char[1024];
                                            int count;
                                            while ((count = in.read(buffer))>0) {
                                                //stream.write(in.read(buffer));
                                                //stream.write(buffer.,0,count);
                                                String buf = new String(buffer);
                                                byte[] data = buf.getBytes("ISO-8859-1");
                                                stream.write(data,0,count);
                                                if (buf.contains(boundary)) {
                                                    System.out.println("");
                                                    System.out.println("<...>");
                                                    System.out.println("");
                                                    System.out.println(new String(data,StandardCharsets.UTF_8));
                                                    break;
                                                }
                                            }
                                            stream.close();

                                            filename=null;
                                            System.out.println("Saving upload to "+sigPlace.UPLOADSDIR+" directory.");
                                        }
                                    }
                                    if (line.contains("Content-Type: multipart/form-data; boundary=")) {
                                        boundary="--"+line.substring("Content-Type: multipart/form-data; boundary=".length());
                                    }
                                } else
                                if (modifiedDate==null&&line.startsWith("If-Modified-Since: ")) {
                                    String modifiedSince=line.replace("If-Modified-Since: ","");
                                    modifiedDate = ZonedDateTime.parse(modifiedSince,DateTimeFormatter.RFC_1123_DATE_TIME);
                                    //System.out.println("Found a modified date of: "+modifiedDate);
                                }
                            }
                            //This is valid.
                            if (splitter[0].equals("GET")) { //This is a GET request.
                                if (splitter[2].equals("HTTP/1.1")||splitter[2].equals("HTTP/2.0")) {
                                    String[] requestSplit = splitter[1].split(Pattern.quote("?"));
                                    String requestloc = requestSplit[0];
                                    HashMap<String,String> requestParams = new HashMap<>();
                                    if (requestSplit.length>1) {
                                        String[] params = requestSplit[1].split(Pattern.quote("&"));
                                        for (String s : params) {
                                            String key = s.substring(0,s.indexOf('='));
                                            String value = s.substring(s.indexOf('=')+1);
                                            requestParams.put(key,value);
                                        }
                                        System.out.println("  ==Params for this request are: "+requestParams);
                                    }
                                    if (requestloc.equals("/")) {
                                        //Send default directory.
                                        if (modifiedDate==null||modifiedDate.isBefore(GetLastModifiedDate(sigPlace.OUTDIR,"testfile.html"))) {
                                            System.out.println(GetLastModifiedDate(sigPlace.OUTDIR,"testfile.html")+"//"+modifiedDate);
                                            CreateRequest(client,"200","OK",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                                        } else {
                                            //System.out.println(" testfile.html is cached! No sending required.");
                                            CreateRequest(client,"304","Not Modified",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                                        }
                                    } else {
                                        String location = URLDecoder.decode(requestloc.replaceFirst("/",""),StandardCharsets.UTF_8);

                                        Path file = null;
                                        if (location.equals("COMMENTS")) {
                                            file = Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article"));
                                        } else {
                                            file = Paths.get(sigPlace.OUTDIR,location);
                                        }
                                        if (location.equals("COMMENTS")&&requestParams.containsKey("message")&&requestParams.containsKey("name")&&requestParams.containsKey("color")) {
                                            //System.out.println(requestParams);
                                            String finalMsg = requestParams.get("message").replaceAll(Pattern.quote("%0A"),"<br/>").replaceAll(Pattern.quote("%3C"),"&lt;");
                                            boolean boldBlock=false;
                                            boolean italicBlock=false;
                                            boolean underlineBlock=false;
                                            boolean codeBlock=false;
                                            boolean linkBlock=false;
                                            StringBuilder storedLink=new StringBuilder();
                                            StringBuilder codeBlockMsg = new StringBuilder();
                                            StringBuilder buildMsg = new StringBuilder();
                                            for (int i=0;i<finalMsg.length();i++) {
                                                if (i<finalMsg.length()-1&&finalMsg.charAt(i)=='~'&&finalMsg.charAt(i+1)=='~') {
                                                    if (codeBlock) {
                                                        codeBlockMsg.append("~~");
                                                        String storedCodeBlock="";
                                                        String s = URLDecoder.decode(codeBlockMsg.toString(),StandardCharsets.UTF_8.toString());
                                                        boolean keyword=false;
                                                        boolean inString=false;
                                                        boolean inComment=false;
                                                        boolean inMultiLineComment=false;
                                                        char stringChar=' ';
                                                        boolean canBeNumericalConstant=false;
                                                        int lengthOfConstant=0;
                                                        storedCodeBlock+=s.substring(0,s.indexOf("~~"));
                                                        storedCodeBlock=storedCodeBlock.replaceAll(Pattern.quote("<"),"\2");
                                                        storedCodeBlock+="</pre>";
                                                        int startPos=0;
                                                        String endText=s.substring(s.indexOf("~~")+"~~".length(),s.length());
                                                        s="";
                                                        for (int j=0;j<storedCodeBlock.length()-1;j++) {
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
                                                                if (canBeNumericalConstant&&validNumericalConstantCharacters(storedCodeBlock,lengthOfConstant, j)) {
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
                                                                if (canBeNumericalConstant&&lengthOfConstant>0&&!(validNumericalConstantCharacters(storedCodeBlock,lengthOfConstant, j))) {
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
                                                                if (j>3&&storedCodeBlock.substring(j-3,j+1).equals("true")&&!isAlphanumeric(storedCodeBlock,j-4)&&!isAlphanumeric(storedCodeBlock,j+1)) {
                                                                    s+=SPAN("number")+storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j)+"</span>";
                                                                    startPos=j+1;
                                                                } else 
                                                                if (j>4&&storedCodeBlock.substring(j-4,j+1).equals("false")&&!isAlphanumeric(storedCodeBlock,j-5)&&!isAlphanumeric(storedCodeBlock,j+1)) {
                                                                    s+=SPAN("number")+storedCodeBlock.substring(startPos,j)+storedCodeBlock.charAt(j)+"</span>";
                                                                    startPos=j+1;
                                                                } else 
                                                                if (storedCodeBlock.charAt(j)=='(') {
                                                                    s+=SPAN("function")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                                                    startPos=j+1;
                                                                } else 
                                                                if (j>0&&isAlphanumeric(storedCodeBlock,j-1) && storedCodeBlock.charAt(j)==' '&&storedCodeBlock.charAt(j-1)!=' ') {
                                                                    //Previous section was a keyword.
                                                                    keyword=true;
                                                                    s+=SPAN("keyword")+storedCodeBlock.substring(startPos,j)+"</span>"+storedCodeBlock.charAt(j);
                                                                    startPos=j+1;
                                                                } else 
                                                                if (j>0&&isAlphanumeric(storedCodeBlock,j-1) && (storedCodeBlock.charAt(j)==';'||storedCodeBlock.charAt(j)==':')) {
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
                                                                if (!isAlphanumeric(storedCodeBlock,j)){
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
                                                            if (!canBeNumericalConstant&&!isAlphanumeric(storedCodeBlock,j)) {
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
                                                        buildMsg.append(s);
                                                    } else {
                                                        codeBlockMsg=codeBlockMsg.delete(0, codeBlockMsg.length());
                                                    }
                                                    i+=1;
                                                    codeBlock=!codeBlock;
                                                    continue;
                                                } else 
                                                if (codeBlock) {
                                                    codeBlockMsg.append(finalMsg.charAt(i));continue;
                                                }
                                                if (foundSub("%5E%5E",finalMsg,i)) {
                                                    if (boldBlock) {
                                                        buildMsg.append("</b>");
                                                    } else {
                                                        buildMsg.append("<b>");
                                                    }
                                                    boldBlock=!boldBlock;
                                                    i+=5;
                                                    continue;
                                                }
                                                if (i<finalMsg.length()-1&&finalMsg.charAt(i)=='*'&&finalMsg.charAt(i+1)=='*') {
                                                    if (italicBlock) {
                                                        buildMsg.append("</i>");
                                                    } else {
                                                        buildMsg.append("<i>");
                                                    }
                                                    italicBlock=!italicBlock;
                                                    i+=1;
                                                    continue;
                                                }
                                                if (i<finalMsg.length()-1&&finalMsg.charAt(i)=='_'&&finalMsg.charAt(i +1)=='_') {
                                                    if (underlineBlock) {
                                                        buildMsg.append("</u>");
                                                    } else {
                                                        buildMsg.append("<u>");
                                                    }
                                                    underlineBlock=!underlineBlock;
                                                    i+=1;
                                                    continue;
                                                }
                                                if (foundSub("%5B%5B",finalMsg,i)||foundSub("%5D%5D",finalMsg,i)) {
                                                    if (linkBlock) {
                                                        buildMsg.append("\">").append(storedLink).append("</a>");
                                                    } else {
                                                        storedLink.delete(0,storedLink.length());
                                                        buildMsg.append("<a href=\"");
                                                    }
                                                    linkBlock=!linkBlock;
                                                    i+=5;
                                                    continue;
                                                }
                                                if (linkBlock) {
                                                    storedLink.append(finalMsg.charAt(i));
                                                }
                                                buildMsg.append(finalMsg.charAt(i));
                                            }
                                            if (Files.exists(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")))) {
                                                List<String> data = Files.readAllLines(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")));
                                                data.set(0,Integer.toString(Integer.parseInt(data.get(0))+1));
                                                data.add(buildMsg.toString()+"\n"+requestParams.get("name")+ZonedDateTime.now()+";"+requestParams.get("color"));
                                                Files.write(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")), data, StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
                                            } else {
                                                List<String> data = new ArrayList<String>();
                                                data.add("1");
                                                data.add(buildMsg.toString()+"\n"+requestParams.get("name")+ZonedDateTime.now()+";"+requestParams.get("color"));
                                                Files.write(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")), data, StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE,StandardOpenOption.CREATE_NEW);
                                            }
                                            CreateRequest(client,"200","OK",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                                        } else {
                                            if (modifiedDate==null||Files.exists(file)&&modifiedDate.isBefore(GetLastModifiedDate(file))) 
                                            {
                                                CreateRequest(client,"200","OK",file);
                                            } else {
                                                //System.out.println(" "+location+" is cached! No sending required.");
                                                CreateRequest(client,"304","Not Modified",file);
                                            }
                                        }
                                    }
                                }
                            } else 
                            if (splitter[0].equals("POST")) { //This is a POST request.
                                if (boundary!=null) {
                                    CreateRequest(client,"200","OK",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                                } else {
                                    CreateRequest(client,"400","Bad Request",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                                }
                            } else {
                                CreateRequest(client,"501","Not Implemented",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                            }
                        } else {
                            in.close();
                            CreateRequest(client,"400","Bad Request",Paths.get(sigPlace.OUTDIR,"testfile.html"));
                        }
                    }
                } catch(SocketException|NullPointerException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean foundSub(String string, String finalMsg, int i) {
        if (i+string.length()<=finalMsg.length()) {
            for (int j=0;j<string.length();j++) {
                if (string.charAt(j)!=finalMsg.charAt(i+j)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    private ZonedDateTime GetLastModifiedDate(Path p) throws IOException {
        Instant newTime = Files.getLastModifiedTime(p).toInstant();
        newTime = newTime.truncatedTo(ChronoUnit.SECONDS);
        return newTime.atZone(ZoneId.of("GMT"));
    }

    private ZonedDateTime GetLastModifiedDate(String first,String...more) throws IOException {
        return GetLastModifiedDate(Paths.get(first,more));
    }

    private void CreateRawRequest(OutputStream stream, String statusCode, String statusMsg, String contentType, byte[] content) {
        CreateRawRequest(stream, statusCode, statusMsg, contentType, content,null);
    }

    private void CreateRawRequest(OutputStream stream, String statusCode, String statusMsg, String contentType, byte[] content, FileTime lastModified) {
        try {
            stream.write(("HTTP/1.1 "+statusCode+" "+statusMsg+"\r\n").getBytes());
            stream.write(("ContentType: "+contentType+"\r\n").getBytes());
            if (lastModified!=null) {
                ZonedDateTime date = lastModified.toInstant().truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.of("GMT"));
                stream.write(("Last-Modified: "+date.format(DateTimeFormatter.RFC_1123_DATE_TIME)+"\r\n").getBytes());
            }
            stream.write("\r\n".getBytes());
            stream.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CreateRequest(Socket client, String statusCode, String statusMsg, Path file) {
        long startTime = System.currentTimeMillis();
        try {
            OutputStream clientOutput = client.getOutputStream();
            if (statusCode.equals("200")) {
                if (Files.exists(file)) {
                    if (Files.isDirectory(file)) {
                        CreateRawRequest(clientOutput,statusCode,statusMsg,"text/html",Files.readAllBytes(Paths.get(file.toAbsolutePath().toString(),sigPlace.DIRECTORYLISTING_FILENAME)),Files.getLastModifiedTime(file));
                        clientOutput.write(("<div class=\"generateTime\">Webpage generated in "+(System.currentTimeMillis()-startTime)+"ms</div>\r\n").getBytes());
                    } else {
                        CreateRawRequest(clientOutput,statusCode,statusMsg,Files.probeContentType(file),Files.readAllBytes(file),Files.getLastModifiedTime(file));
                        String contentType = Files.probeContentType(file);
                        if (contentType!=null&&contentType.equals("text/html")) {
                            clientOutput.write(("<div class=\"generateTime\">Webpage generated in "+(System.currentTimeMillis()-startTime)+"ms</div>\r\n").getBytes());
                        }
                        System.out.println(contentType);
                    }
                    System.out.println("Sent "+file+" to client "+client+".");
                } else {
                    CreateRawRequest(clientOutput,statusCode,statusMsg,"text/html","<!DOCTYPE html>\nWe're sorry, your webpage is in another castle!".getBytes());
                    System.out.println("Sent [404] "+statusMsg+" to client "+client+" for "+file+".");
                }
            } else {
                CreateRawRequest(clientOutput,statusCode,statusMsg,"text/html","<!DOCTYPE html>\nWe're sorry, your webpage exploded!".getBytes());
                System.out.println("Sent ["+statusCode+"] "+statusMsg+" to client "+client+" for "+file+".");
            }
            clientOutput.write("\r\n\r\n".getBytes());
            clientOutput.flush();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
