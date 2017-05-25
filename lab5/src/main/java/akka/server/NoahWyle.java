package akka.server;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import scala.concurrent.duration.Duration;

import static akka.actor.SupervisorStrategy.restart;

public class NoahWyle extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    if (s.startsWith("order ")) {
                        getContext().actorSelection("orderReceiver").tell(s, getSender());
                    } else if (s.startsWith("read ")) {
                        getContext().actorSelection("reader").tell(s, getSender());
                    } else if (s.startsWith("find ")) {
                        getContext().actorSelection("dbManager").tell(s, getSender());
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    @Override
    public void preStart() throws Exception {
        context().actorOf(Props.create(OrderActor.class), "orderReceiver");
        context().actorOf(Props.create(TomaszKnapik.class), "reader");
        context().actorOf(Props.create(Zygmunt.class), "dbManager");
    }

    private static SupervisorStrategy strategy
            = new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder
            .matchAny(o -> restart())
            .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
