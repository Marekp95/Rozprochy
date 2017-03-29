import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos;

public class Receiver extends ReceiverAdapter {
    private String channelName;
    private Client client;

    public Receiver(String channelName, Client client) {
        this.channelName = channelName;
        this.client = client;
    }

    @Override
    public void viewAccepted(View new_view) {
        client.clearState(channelName);
        for (Address a : new_view.getMembers()) {
            ChatOperationProtos.ChatAction chatAction = ChatOperationProtos.ChatAction.newBuilder().setAction(ChatOperationProtos.ChatAction.ActionType.JOIN).setNickname(a.toString()).setChannel(channelName).build();
            client.refreshState(chatAction);
        }

        System.out.println(channelName + " ** view: " + new_view);
    }

    @Override
    public void receive(Message msg) {
        try {
            ChatOperationProtos.ChatMessage chatMessage = ChatOperationProtos.ChatMessage.parseFrom(msg.getRawBuffer());
            System.out.println(channelName + ">> " + msg.getSrc() + ": " + chatMessage.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
