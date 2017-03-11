import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    public static final int PORT = 12311;
    public static final int PORT_M = 33011;
    public static final String IP_M = "224.3.5.7";
    public static final int MSG_SIZE = 4096;


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

    private void startUDP() {
        new Thread(() -> {
            try {
                DatagramSocket datagramSocket = new DatagramSocket(PORT);
                while (!datagramSocket.isClosed()) {
                    byte[] bytes = new byte[MSG_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
                    datagramSocket.receive(datagramPacket);
                    String message = new String(bytes);

                    String[] split = message.split(";", 3);

                    if (split.length < 3) {
                        continue;
                    }

                    String name = split[0];
                    String messageType = split[1];
                    message = split[2];

                    if (messageType.equals("M") || messageType.equals("N")) {
                        for (String userName: users.keySet()) {
                            if(!userName.equals(name)){
                                try {
                                    int port;
                                    String IP;
                                    switch (messageType) {
                                        case "M":
                                            IP = "localhost";
                                            port = users.get(userName).getSocket().getPort();
                                            break;
                                        case "N":
                                            IP = IP_M;
                                            port = PORT_M;
                                            break;
                                        default:
                                            continue;
                                    }
                                    InetAddress IPAddress = InetAddress.getByName(IP);
                                    byte[] newBytes = (name + ":" + message).getBytes();
                                    datagramSocket.send(new DatagramPacket(newBytes, newBytes.length, IPAddress, port));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
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
        Server server= new Server();
        server.startUDP();
        server.start();
    }
}
