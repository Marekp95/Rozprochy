package akka.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Zygmunt extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    ActorRef actor = context().actorOf(Props.create(SearchResponseSender.class, getSender()));
                    context().child("worker1").get().tell(s, actor);
                    context().child("worker2").get().tell(s, actor);
                })
                .match(SuicideRequest.class, r -> {
                    getSender().tell(PoisonPill.getInstance(), getSelf());
                    System.out.println("aaaaaaaaaaaasssssssssssssssssss");
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    @Override
    public void preStart() throws Exception {
        context().actorOf(Props.create(DbWorker.class, "books1.txt"), "worker1");
        context().actorOf(Props.create(DbWorker.class, "books2.txt"), "worker2");
    }
}