import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

public class sigServer {
    ServerSocket socket;
    sigServer() {
        try {
            socket = new ServerSocket(8080);
            System.out.println("Listening on port 8080.");
            while (true) {
                try (Socket client = socket.accept()) {
                    System.out.println("New client connection detected: "+client.toString());
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String line;
                    line=in.readLine(); //Read the first line, this should be our request.
                    String[] splitter = line.split(Pattern.quote(" "));
                    if (splitter.length==3) {
                        //This is valid.
                        if (splitter[2].equals("HTTP/1.1")||splitter[2].equals("HTTP/2.0")) {
                            String requestloc = splitter[1];
                            if (requestloc.equals("/")) {
                                //Send default directory.
                                CreateRequest(client,"testfile.html");
                            }
                        }
                    }
                    while (!(line=in.readLine()).isBlank()) {
                        //System.out.println(line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void CreateRequest(Socket client, String string) {
        Path file = Paths.get(sigPlace.OUTDIR,string);
        try {
            OutputStream clientOutput = client.getOutputStream();
            clientOutput.write("HTTP/1.1 200 OK\r\n".getBytes());
            clientOutput.write(("ContentType: text/html\r\n").getBytes());
            clientOutput.write("\r\n".getBytes());
            clientOutput.write(Files.readAllBytes(file));
            clientOutput.write("\r\n\r\n".getBytes());
            clientOutput.flush();
            client.close();
            System.out.println("Sent "+file+" to client "+client+".");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
