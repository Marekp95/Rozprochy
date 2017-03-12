import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

public class Client {
    private String multicastIP = "224.3.5.7";
    private int multicastPort = 33011;

    private void start() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(Server.PORT));

        DatagramSocket datagramSocket = new DatagramSocket(socket.getLocalPort());

        System.out.println("Insert your name:");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();

        //TCP communication
        Scanner incomingMessages = new Scanner(socket.getInputStream());
        new Thread(() -> {
            while (incomingMessages.hasNextLine()) {
                System.out.println("TCP message: " + incomingMessages.nextLine());
            }

        }).start();

        //UDP communication
        new Thread(() -> {
            try {
                while (true) {
                    byte[] buff = new byte[Server.MSG_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
                    datagramSocket.receive(datagramPacket);
                    String message = new String(buff);
                    System.out.println("UDP message: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //multicast communication
        new Thread(() -> {
            try {
                MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
                multicastSocket.joinGroup(InetAddress.getByName(multicastIP));
                while (true) {
                    byte[] buff = new byte[Server.MSG_SIZE];
                    DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
                    multicastSocket.receive(datagramPacket);
                    String message = new String(buff);
                    System.out.println("Multicast message: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

        printWriter.write(name + '\n');
        printWriter.flush();

        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            if (message.equals("M") || message.equals("N")) {

                InetAddress IPAddress = InetAddress.getByName(message.equals("M") ? ("localhost") : (multicastIP));
                int port = message.equals("M") ? Server.PORT : multicastPort;

                byte[] bytes = (name + ": " + "\n" +
                        "              .@.                                    .\n" +
                        "              @m@,.                                 .@\n" +
                        "             .@m%nm@,.                            .@m@\n" +
                        "            .@nvv%vnmm@,.                      .@mn%n@\n" +
                        "           .@mnvvv%vvnnmm@,.                .@mmnv%vn@,\n" +
                        "           @mmnnvvv%vvvvvnnmm@,.        .@mmnnvvv%vvnm@\n" +
                        "           @mmnnvvvvv%vvvvvvnnmm@, ;;;@mmnnvvvvv%vvvnm@,\n" +
                        "           `@mmnnvvvvvv%vvvvvnnmmm;;@mmnnvvvvvv%vvvvnmm@\n" +
                        "            `@mmmnnvvvvvv%vvvnnmmm;%mmnnvvvvvv%vvvvnnmm@\n" +
                        "              `@m%v%v%v%v%v;%;%;%;%;%;%;%%%vv%vvvvnnnmm@\n" +
                        "              .,mm@@@@@mm%;;@@m@m@@m@@m@mm;;%%vvvnnnmm@;@,.\n" +
                        "           .,@mmmmmmvv%%;;@@vmvvvvvvvvvmvm@@;;%%vvnnm@;%mmm@,\n" +
                        "        .,@mmnnvvvvv%%;;@@vvvvv%%%%%%%vvvvmm@@;;%%mm@;%%nnnnm@,\n" +
                        "     .,@mnnvv%v%v%v%%;;@mmvvvv%%;*;*;%%vvvvmmm@;;%m;%%v%v%v%vmm@,.\n" +
                        " ,@mnnvv%v%v%v%v%v%v%;;@@vvvv%%;*;*;*;%%vvvvm@@;;m%%%v%v%v%v%v%vnnm@,\n" +
                        " `    `@mnnvv%v%v%v%%;;@mvvvvv%%;;*;;%%vvvmmmm@;;%m;%%v%v%v%vmm@'   '\n" +
                        "         `@mmnnvvvvv%%;;@@mvvvv%%%%%%%vvvvmm@@;;%%mm@;%%nnnnm@'\n" +
                        "            `@mmmmmmvv%%;;@@mvvvvvvvvvvmmm@@;;%%mmnmm@;%mmm@'\n" +
                        "               `mm@@@@@mm%;;@m@@m@m@m@@m@@;;%%vvvvvnmm@;@'\n" +
                        "              ,@m%v%v%v%v%v;%;%;%;%;%;%;%;%vv%vvvvvnnmm@\n" +
                        "            .@mmnnvvvvvvv%vvvvnnmm%mmnnvvvvvvv%vvvvnnmm@\n" +
                        "           .@mmnnvvvvvv%vvvvvvnnmm'`@mmnnvvvvvv%vvvnnmm@\n" +
                        "           @mmnnvvvvv%vvvvvvnnmm@':%::`@mmnnvvvv%vvvnm@'\n" +
                        "           @mmnnvvv%vvvvvnnmm@'`:::%%:::'`@mmnnvv%vvmm@\n" +
                        "           `@mnvvv%vvnnmm@'     `:;%%;:'     `@mvv%vm@'\n" +
                        "            `@mnv%vnnm@'          `;%;'         `@n%n@\n" +
                        "             `@m%mm@'              ;%;.           `@m@\n" +
                        "              @m@'                 `;%;             `@\n" +
                        "              `@'                   ;%;.             '\n" +
                        "               `                    `;%;             \n").getBytes();
                datagramSocket.send(new DatagramPacket(bytes, bytes.length, IPAddress, port));
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
