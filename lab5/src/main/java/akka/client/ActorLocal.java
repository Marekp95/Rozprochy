package akka.client;

import akka.ResponseMessage;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ActorLocal extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                // TODO
                .match(String.class, s -> {
                    getContext().actorSelection("akka.tcp://library@127.0.0.1:3552/user/librarian").tell(s, getSelf());
                })
                .match(ResponseMessage.class, r -> {
                    System.out.println(r.getMessage());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
