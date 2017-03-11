import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServerThread extends Thread {
    private Socket socket;
    private Scanner scanner;
    private PrintWriter printWriter;
    private Server server;

    public ServerThread(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        scanner = new Scanner(socket.getInputStream());
        printWriter = new PrintWriter(socket.getOutputStream(), true);
    }


    @Override
    public void run() {
        String name = scanner.nextLine();
        server.register(name, this);
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            server.sendAll(name, message);
        }
    }

    public void send(String message) {
        printWriter.write(message);
        printWriter.flush();
    }

    public Socket getSocket() {
        return socket;
    }
}
