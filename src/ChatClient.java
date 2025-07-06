import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080)) {
            System.out.println("Connected to the server!");

            // Input and Output streams
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread to read messages from server
            new Thread(() -> {
                try {
                    String text;
                    while ((text = reader.readLine()) != null) {
                        System.out.println("Message: " + text);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Send messages to server
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String text;
            while (true) {
                text = console.readLine();
                writer.println(text); // Send to server
                if ("bye".equalsIgnoreCase(text)) break;
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
