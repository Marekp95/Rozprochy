package akka.server;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import scala.concurrent.duration.Duration;

import static akka.actor.SupervisorStrategy.restart;

public class DbSupervisor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    ActorRef actor = context().actorOf(Props.create(SearchResponseSender.class, getSender()));
                    context().actorOf(Props.create(DbWorker.class, "books1.txt")).tell(s, actor);
                    context().actorOf(Props.create(DbWorker.class, "books2.txt")).tell(s, actor);
                })
                .match(SuicideRequest.class, r -> {
                    getSender().tell(PoisonPill.getInstance(), getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}