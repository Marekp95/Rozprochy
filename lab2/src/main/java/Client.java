import com.google.protobuf.*;
import org.jgroups.*;
import org.jgroups.Message;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

import java.net.InetAddress;
import java.util.*;

public class Client extends ReceiverAdapter {
    private final String name;
    private JChannel channelState;
    private Map<String, JChannel> channelMap = new HashMap<String, JChannel>();
    private String channelName = "";
    private Map<String, Set<String>> state = new HashMap<String, Set<String>>();

    public Client(String name) {
        this.name = name;
    }

    private void start() throws Exception {
        channelState = new JChannel(false);

        final ProtocolStack stack = new ProtocolStack();
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

        channelState.setReceiver(new ReceiverAdapter() {
            @Override
            public void viewAccepted(View new_view) {
                for (Address a : new_view.getMembers()) {
                    System.out.println(a);
                }
                ChatOperationProtos.ChatState.Builder chatStateBuilder = ChatOperationProtos.ChatState.newBuilder();

                for (String channel : state.keySet()) {
                    for (String name : state.get(channel)) {
                        ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setAction(ChatOperationProtos.ChatAction.ActionType.JOIN).setNickname(name).setChannel(channel).build();
                        chatStateBuilder.addState(chatAction);
                    }
                }
                Message message = new Message(null, null, chatStateBuilder.build().toByteArray());
                try {
                    channelState.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("** view: " + new_view);
            }

            @Override
            public void receive(Message msg) {
                try {
                    ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.parseFrom(msg.getRawBuffer());
                    refreshState(chatAction);

                } catch (InvalidProtocolBufferException e) {
                    try {
                        ChatOperationProtos.ChatState chatState = ChatOperationProtos.ChatState.parseFrom(msg.getRawBuffer());
                        for (ChatOperationProtos.ChatAction chatAction : chatState.getStateList()) {
                            refreshState(chatAction);
                        }
                    } catch (InvalidProtocolBufferException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        channelState.connect("ChatManagement321123");

    }

    private void refreshState(ChatOperationProtos.ChatAction chatAction) {
        switch (chatAction.getAction()) {
            case JOIN:
                if (!state.containsKey(chatAction.getChannel())) {
                    state.put(chatAction.getChannel(), new HashSet<String>());
                }
                state.get(chatAction.getChannel()).add(chatAction.getNickname());

                break;
            case LEAVE:
                if (state.containsKey(chatAction.getChannel())) {
                    state.get(chatAction.getChannel()).remove(chatAction.getNickname());
                }
                break;
            default:
                break;
        }
    }

    private void send(String msg) throws Exception {
        ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage.newBuilder().setMessage(msg).build();
        Message message = new Message(null, null, chatMessage.toByteArray());
        if (channelMap.containsKey(channelName)) {
            channelMap.get(channelName).send(message);
        }
    }

    private void createChannel(final String name) throws Exception {
        if (!name.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            System.out.println("Wrong channel name: " + name);
            return;
        }

        if (channelMap.containsKey(name)) {
            changeChannelName(name);
            return;
        }

        JChannel channel = new JChannel(false);

        ProtocolStack stack = new ProtocolStack();
        stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName(name)))
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

        channel.setProtocolStack(stack);
        stack.init();

        channel.setReceiver(new ReceiverAdapter() {
            @Override
            public void viewAccepted(View new_view) {
                System.out.println(name + " ** view: " + new_view);
            }

            @Override
            public void receive(Message msg) {
                try {
                    ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage.parseFrom(msg.getRawBuffer());
                    System.out.println(name + ">> " + msg.getSrc() + ": " + chatMessage.getMessage());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        channel.connect(name);
        channelName = name;
        channelMap.put(name, channel);
        ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setAction(ChatOperationProtos.ChatAction.ActionType.JOIN).setNickname(this.name).setChannel(channelName).build();
        Message message = new Message(null, null, chatAction.toByteArray());
        channelState.send(message);
    }

    private void changeChannelName(String name) {
        if (channelMap.containsKey(name)) {
            channelName = name;
        }
    }

    private void disconnectChannel(String name) {
        if (channelMap.containsKey(name)) {
            if (name.equals(channelName)) {
                channelName = "";
            }

            JChannel jChannel = channelMap.get(name);
            if (!jChannel.isClosed()) {
                jChannel.close();
            }

            channelMap.remove(name);

            ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setAction(ChatOperationProtos.ChatAction.ActionType.LEAVE).setNickname(this.name).setChannel(name).build();
            Message message = new Message(null, null, chatAction.toByteArray());
            try {
                channelState.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showMembers() {
        for (String channel : state.keySet()) {
            System.out.println(channel);
            for (String name : state.get(channel)) {
                System.out.println("\t" + name);
            }
        }
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
        while (true) {
            System.out.print(client.channelName + ">> ");
            String msg = scanner.nextLine();
            if (msg.equals("/h")) {
                System.out.println("Commands: \n" +
                        "/h - help\n" +
                        "/s - show users\n" +
                        "/o IP - open channel\n" +
                        "/c IP - close channel\n" +
                        "/exit");
            } else if (msg.equals("/s")) {
                client.showMembers();
            } else if (msg.startsWith("/c ")) {
                String[] strings = msg.split(" ", 2);
                if (strings.length > 1) {
                    client.disconnectChannel(strings[1]);
                }
            } else if (msg.equals("/exit")) {
                break;
            } else if (msg.startsWith("/o ")) {
                try {
                    String[] strings = msg.split(" ", 2);
                    if (strings.length > 1) {
                        client.createChannel(strings[1]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (msg.length() > 0) {
                try {
                    client.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (JChannel jChannel : client.channelMap.values()) {
            if (!jChannel.isClosed()) {
                jChannel.close();
            }
        }
        if (!client.channelState.isClosed()) {
            client.channelState.close();
        }
    }
}
