package akka.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ResponseSender extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ActorRef receiver;
    private boolean send = false;
    private int counter = 0;

    public ResponseSender(ActorRef receiver) {
        this.receiver = receiver;
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Integer.class, i -> {
                    counter++;
                    if (!send) {
                        if (counter == 2 || i >= 0) {
                            send = true;
                            receiver.tell(i, null);
                            getContext().getParent().tell(new SuicideRequest(), getSelf());
                        }
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}