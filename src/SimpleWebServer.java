import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleWebServer {
    private static final int PORT = 8080;
    private static final String username = "root";
    private static final String password = "toor";
    final int MAX_FILESIZE = 4096;
    final String ERROR_LOG = "/error_log.txt";
    private String authPhrase;

    private static ServerSocket dServerSocket;

    public SimpleWebServer () throws Exception {
        dServerSocket = new ServerSocket(PORT);

        /* generate and save base64 encoded phrase for hardcoded username/password to compare against */
        String credPair = username + ":" + password;
        authPhrase = Base64.getEncoder().encodeToString(
                credPair.getBytes("utf-8"));
    }

    public void run() throws Exception {
        while (true) {
            Socket s = dServerSocket.accept();
            processRequest(s);
        }
    }

    public boolean authenticate(BufferedReader br) throws Exception {
        String encodedAuth = null;
        for (String line; (line = br.readLine()) != null;) {
            if (line.isEmpty()) break;
            if (line.startsWith("Authorization: Basic ")) {
                try {
                    encodedAuth = line.split("\\s+")[2];
                }
                catch (Exception e) {
                    return false;
                }
            }
        }
        if (encodedAuth != null) {
            if (encodedAuth.equals(authPhrase)) {
                return true;
            }
        }
        return false;
    }

    public void processRequest(Socket s) throws Exception {
        BufferedReader br = new BufferedReader (
                new InputStreamReader(s.getInputStream()));

        OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());

        String request = br.readLine();
        if (authenticate(br)) {
            System.out.println("Authenticated");
            String command = null;
            String pathname = null;

            StringTokenizer st = new StringTokenizer(request, " ");

            command = st.nextToken();
            pathname = st.nextToken();

            if (command.equals("GET")) {
                serveFile(osw, pathname);
            } else {
                osw.write("HTTP/1.0 501 Not Implemented\n\n");
            }
        }
        else {
            osw.write( "HTTP/1.0 403 Forbidden\n\n");
            osw.flush();
        }
        osw.close();
    }

    public void serveFile (OutputStreamWriter osw,
                           String pathname) throws Exception {
        FileReader fr = null;
        int c = -1;

        StringBuffer sb = new StringBuffer();
         /* Remove the initial slash at the beginning
            of the pathname in the request. */
        if (pathname.charAt(0) == '/')
            pathname = pathname.substring(1);

        /* If there was no filename specified by the
        client, serve the "index.html" file. */
        if (pathname.equals(""))
            pathname = "index.html";

        /* Try to open file specified by pathname. */
        try {
            fr = new FileReader(pathname);
            c = fr.read();
        }
        catch (Exception e) {
            /* If the file is not found, return the
           appropriate HTTP response code. */
            osw.write("HTTP/1.0 404 Not Found\n\n");
            osw.flush();
            return;
        }
        if (!isEligibleSize(pathname)) {
            osw.write( "HTTP/1.0 403 Forbidden\n\n");
            osw.flush();
            logEntry(pathname, "403 Forbidden");
            return;
        }
        /* If the requested file can be successfully opened
           and read, then return an OK response code and
           send the contents of the file. */
        osw.write("HTTP/1.0 200 OK\n\n");
        osw.flush();
        while (c != -1) {
            sb.append((char) c);
            c = fr.read();
        }
        osw.write(sb.toString());
        osw.flush();
    }

    public void logEntry(String filename, String record) throws Exception {
        FileWriter fw = new FileWriter (ERROR_LOG, true);
        fw.write (getTimestamp() + " " + record + " " + filename);
        fw.close();
    }

    public boolean isEligibleSize(String path) throws Exception {
        File desiredFile = new File(path);
        return desiredFile.length() <= MAX_FILESIZE;
    }


    public String getTimestamp() {
        return (new Date()).toString();
    }
}
