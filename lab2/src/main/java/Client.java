import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

import java.net.InetAddress;
import java.util.Scanner;

public class Client extends ReceiverAdapter {
    private final String name;
    private JChannel channelState;

    public Client(String name) {
        this.name = name;
    }

    private void start() throws Exception {
        channelState = new JChannel(false);

        ProtocolStack stack = new ProtocolStack();
        stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName("230.0.0.36")))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE_TRANSFER())
                .addProtocol(new FLUSH());

        channelState.setProtocolStack(stack);
        stack.init();

        channelState.setReceiver(new ReceiverAdapter(){
            @Override
            public void viewAccepted(View new_view) {
                System.out.println("** view: " + new_view);
            }

            @Override
            public void receive(Message msg) {
                try {
                    //ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage.parseFrom(msg.getRawBuffer());
                    System.out.println(msg.getSrc() + " " + new String(msg.getRawBuffer()));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        channelState.connect("ChatManagement321123");

        // ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setChannel("ChatManagement321123").setNickname(name).setAction(ChatOperationProtos.ChatAction.ActionType.JOIN).build();

        //channelState.send(new Message(null, null, chatAction.toByteArray()));
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    @Override
    public void receive(Message msg) {
        try {
            //ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage.parseFrom(msg.getRawBuffer());
            System.out.println(msg.getSrc() + " " + new String(msg.getRawBuffer()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(String msg) throws Exception {
        ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage.newBuilder().setMessage(msg).build();
        Message message = new Message(null, null, chatMessage.toByteArray());
        channelState.send(message);
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.nextLine();

        Client client = new Client(name);
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while(true){
            String msg = scanner.nextLine();
            try {
                client.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
