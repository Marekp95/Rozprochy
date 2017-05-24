package akka.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Zygmunt extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    context().child("worker1").get().tell(s, getSender());
                    context().child("worker2").get().tell(s, getSender());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    @Override
    public void preStart() throws Exception {
        context().actorOf(Props.create(Worker.class, "books1.txt"), "worker1");
        context().actorOf(Props.create(Worker.class, "books2.txt"), "worker2");
    }
}