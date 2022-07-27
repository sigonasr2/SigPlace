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
                                            if (Files.exists(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")))) {
                                                List<String> data = Files.readAllLines(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")));
                                                data.set(0,Integer.toString(Integer.parseInt(data.get(0))+1));
                                                data.add(finalMsg+"\n"+requestParams.get("name")+ZonedDateTime.now()+";"+requestParams.get("color"));
                                                Files.write(Paths.get(sigPlace.COMMENTSDIR,requestParams.get("article")), data, StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);
                                            } else {
                                                List<String> data = new ArrayList<String>();
                                                data.add("1");
                                                data.add(finalMsg+"\n"+requestParams.get("name")+ZonedDateTime.now()+";"+requestParams.get("color"));
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
