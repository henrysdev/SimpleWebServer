import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class Main {
    public static void main(String[] args) throws Exception {
        SimpleWebServer sws = new SimpleWebServer();
        sws.run();
    }
}
