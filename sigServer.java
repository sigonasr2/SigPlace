import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String requestLine,line;
                    ZonedDateTime modifiedDate = null;
                    /*String boundary="";
                    boolean truncateUntilBoundary=false;*/
                    requestLine=in.readLine(); //Read the first line, this should be our request.
                    if (requestLine!=null) {
                        while (in.ready()) {
                            line=in.readLine();
                            /*
                            if (!truncateUntilBoundary) {
                                System.out.println(line);

                                if (boundary.length()>0) {
                                    if (line.equals(boundary)) {
                                        truncateUntilBoundary=true;
                                    }
                                }
                            } else 
                            if (line.contains(boundary)) {
                                System.out.println("");
                                System.out.println("<...>");
                                System.out.println("");
                                System.out.println(line);
                                truncateUntilBoundary=false;
                            } else
                            if (line.contains("Content-Disposition: ")||line.contains("Content-Type: ")) {
                                System.out.println(line);
                            }

                            if (line.contains("Content-Type: multipart/form-data; boundary=")) {
                                boundary="--"+line.substring("Content-Type: multipart/form-data; boundary=".length());
                            } else*/
                            if (modifiedDate==null&&line.startsWith("If-Modified-Since: ")) {
                                String modifiedSince=line.replace("If-Modified-Since: ","");
                                modifiedDate = ZonedDateTime.parse(modifiedSince,DateTimeFormatter.RFC_1123_DATE_TIME);
                                //System.out.println("Found a modified date of: "+modifiedDate);
                            }
                        }
                        String[] splitter = requestLine.split(Pattern.quote(" "));
                        if (splitter.length==3) {
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
                                            CreateRequest(client,"200","OK","testfile.html");
                                        } else {
                                            //System.out.println(" testfile.html is cached! No sending required.");
                                            CreateRequest(client,"304","Not Modified","testfile.html");
                                        }
                                    } else {
                                        String location = URLDecoder.decode(requestloc.replaceFirst("/",""),StandardCharsets.UTF_8);
                                        if (modifiedDate==null||modifiedDate.isBefore(GetLastModifiedDate(sigPlace.OUTDIR,location))) 
                                        {
                                            CreateRequest(client,"200","OK",location);
                                        } else {
                                            //System.out.println(" "+location+" is cached! No sending required.");
                                            CreateRequest(client,"304","Not Modified",location);
                                        }
                                    }
                                }
                            } else 
                            if (splitter[0].equals("POST")) { //This is a POST request.
                                CreateRequest(client,"501","Not Implemented","testfile.html");
                            } else {
                                CreateRequest(client,"501","Not Implemented","testfile.html");
                            }
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

    private ZonedDateTime GetLastModifiedDate(String first,String...more) throws IOException {
        Instant newTime = Files.getLastModifiedTime(Paths.get(first,more)).toInstant();
        newTime = newTime.truncatedTo(ChronoUnit.SECONDS);
        return newTime.atZone(ZoneId.of("GMT"));
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

    private void CreateRequest(Socket client, String statusCode, String statusMsg, String string) {
        long startTime = System.currentTimeMillis();
        Path file = Paths.get(sigPlace.OUTDIR,string);
        try {
            OutputStream clientOutput = client.getOutputStream();
            if (statusCode.equals("200")) {
                if (Files.exists(file)) {
                    if (Files.isDirectory(file)) {
                        CreateRawRequest(clientOutput,statusCode,statusMsg,"text/html",Files.readAllBytes(Paths.get(sigPlace.OUTDIR,string,sigPlace.DIRECTORYLISTING_FILENAME)),Files.getLastModifiedTime(file));
                        clientOutput.write(("<div class=\"generateTime\">Webpage generated in "+(System.currentTimeMillis()-startTime)+"ms</div>\r\n").getBytes());
                    } else {
                        CreateRawRequest(clientOutput,statusCode,statusMsg,Files.probeContentType(file),Files.readAllBytes(file),Files.getLastModifiedTime(file));
                        String contentType = Files.probeContentType(file);
                        if (contentType!=null&&contentType.equals("text/html")) {
                            clientOutput.write(("<div class=\"generateTime\">Webpage generated in "+(System.currentTimeMillis()-startTime)+"ms</div>\r\n").getBytes());
                        }
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
