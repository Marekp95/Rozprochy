import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

public class Client {

    private void start() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(Server.PORT));

        DatagramSocket datagramSocket = new DatagramSocket(socket.getLocalPort());

   /*     openUDPListener(datagramSocket);
        openMultiCast();*/

        System.out.println("Enter nick:");
        Scanner scanner = new Scanner(System.in);
        String nick = scanner.nextLine();

        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

        printWriter.write(nick + '\n');
        printWriter.flush();

        Scanner finalScanner = new Scanner(socket.getInputStream());
        new Thread(() -> {
            while(true) {
                while (finalScanner.hasNextLine()) {
                    System.out.println("TCP " + finalScanner.nextLine());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();

        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            if (message.equals("M") || message.equals("N")) {
                InetAddress IPAddress = InetAddress.getByName("localhost");

                byte[] bytes = (nick + ":" + message + ":                    \n" +
                        "                 _.-;;-._\n" +
                        "          '-..-'|   ||   |\n" +
                        "          '-..-'|_.-;;-._|\n" +
                        "          '-..-'|   ||   |\n" +
                        "    jgs   '-..-'|_.-''-._|").getBytes();
                datagramSocket.send(new DatagramPacket(bytes, bytes.length, IPAddress, Server.PORT));
            } else {
                printWriter.write(message + '\n');
                printWriter.flush();
            }
        }

        socket.close();
    }

    public static void main(String[] args) {
        try {
            (new Client()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
