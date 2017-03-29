import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

import java.io.InputStream;
import java.io.OutputStream;

public class ManagementReceiver extends ReceiverAdapter {
    private Client client;

    public ManagementReceiver(Client client) {
        this.client = client;
    }

    @Override
    public void viewAccepted(View new_view) {
        client.clearState(Client.channelManagementName);
        for (Address a : new_view.getMembers()) {
            System.out.println(a);
            ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setAction(ChatOperationProtos.ChatAction.ActionType.JOIN).setNickname(a.toString()).setChannel(Client.channelManagementName).build();
            client.refreshState(chatAction);
        }

        System.out.println("** view: " + new_view);
    }

    @Override
    public void receive(Message msg) {
        try {
            ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.parseFrom(msg.getRawBuffer());
            client.refreshState(chatAction);

        } catch (InvalidProtocolBufferException e) {
            try {
                ChatOperationProtos.ChatState chatState = ChatOperationProtos.ChatState.parseFrom(msg.getRawBuffer());
                for (ChatOperationProtos.ChatAction chatAction : chatState.getStateList()) {
                    client.refreshState(chatAction);
                }
            } catch (InvalidProtocolBufferException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        ChatOperationProtos.ChatState.Builder chatStateBuilder = ChatOperationProtos.ChatState.newBuilder();

        for (String channel : client.getState().keySet()) {
            for (String name : client.getState().get(channel)) {
                ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setAction(ChatOperationProtos.ChatAction.ActionType.JOIN).setNickname(name).setChannel(channel).build();
                chatStateBuilder.addState(chatAction);
            }
        }
        chatStateBuilder.build().writeTo(output);
    }

    @Override
    public void setState(InputStream input) throws Exception {
        ChatOperationProtos.ChatState chatState = ChatOperationProtos.ChatState.parseFrom(input);
        for (ChatOperationProtos.ChatAction chatAction : chatState.getStateList()) {
            client.refreshState(chatAction);
        }
    }
}
