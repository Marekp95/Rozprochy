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
                .match(String.class, s -> {
                    getContext().actorSelection("akka.tcp://library@127.0.0.1:3552/user/librarian").tell(s, getSelf());
                })
                .match(ResponseMessage.class, r -> {
                    System.out.println(r.getMessage());
                })
                .match(Integer.class, i -> {
                    if (i >= 0) {
                        System.out.println("Price: " + i);
                    } else {
                        System.out.println("404 - Book not found");
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
