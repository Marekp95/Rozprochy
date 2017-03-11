import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    public static final int PORT = 12311;
    private Map<String, ServerThread> users = new HashMap<>();

    private void start() {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(Server.PORT);
                while (true) {
                    new ServerThread(this, serverSocket.accept()).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void register(String name, ServerThread serverThread) {
        System.out.println("registered " + name);
        users.put(name, serverThread);
    }

    public void sendAll(String name, String message) {
        for (String userName : users.keySet()) {
            if (!userName.equals(name)) {
                users.get(userName).send(name + ": " + message + "\n");
            }
        }
    }

    public static void main(String[] args) {
        (new Server()).start();
    }
}
