import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 12311;
    public static final int MSG_SIZE = 4096;


    private Map<String, ServerThread> users = new HashMap<>();

    private void start() {
        new Thread(() -> {
            ExecutorService executorService = Executors.newFixedThreadPool(5);

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(Server.PORT);
                while (true) {
                    executorService.execute(new ServerThread(this, serverSocket.accept()));
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

    private void startUDP() {
        new Thread(() -> {
            try {
                DatagramSocket datagramSocket = new DatagramSocket(PORT);
                while (!datagramSocket.isClosed()) {
                    byte[] bytes = new byte[MSG_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
                    datagramSocket.receive(datagramPacket);
                    String message = new String(bytes);

                    String[] split = message.split(": ", 2);

                    if (split.length < 2) {
                        continue;
                    }

                    String name = split[0];
                    message = split[1];

                    for (String userName : users.keySet()) {
                        if (!userName.equals(name)) {
                            try {
                                InetAddress IPAddress = InetAddress.getByName("localhost");
                                byte[] newBytes = (name + ":" + message).getBytes();
                                datagramSocket.send(new DatagramPacket(newBytes, newBytes.length, IPAddress, users.get(userName).getSocket().getPort()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
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
        Server server = new Server();
        server.startUDP();
        server.start();
    }
}
