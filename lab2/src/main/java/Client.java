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
    private List<Address> addressList = Collections.emptyList();
    private Map<String, JChannel> channelMap = new HashMap<String, JChannel>();
    private Map<String, List<Address>> addressMap = new HashMap<String, List<Address>>();
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

                for(String channel: state.keySet()){
                    for(String name: state.get(channel)){
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
                addressList = new_view.getMembers();
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
                        for(ChatOperationProtos.ChatAction chatAction: chatState.getStateList()){
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
            System.out.println(name);
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

                addressMap.put(name, new_view.getMembers());
                System.out.println(name + " ** view: " + new_view);
            }

            @Override
            public void receive(Message msg) {
                try {
                    System.out.println(name + " " + msg.getSrc() + " " + new String(msg.getRawBuffer()));

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
        //todo
    }

    private void showMembers() {
        /*for (Address a : addressList) {
            System.out.println(a);
        }
        for (String addresses : addressMap.keySet()) {
            System.out.println(addresses);
            for (Address address : addressMap.get(addresses)) {
                System.out.println("\t" + address);
            }
        }*/
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
            String msg = scanner.nextLine();
            if (msg.equals("show")) {
                client.showMembers();
            } else if (msg.startsWith("create ")) {
                // todo
                try {
                    client.createChannel(msg.split(" ", 2)[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    client.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
