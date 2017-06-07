package akka.server;

import akka.NotUsed;
import akka.ResponseMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class BookReader extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    ActorMaterializer actorMaterializer = ActorMaterializer.create(context());
                    ActorRef run = Source.actorRef(1000, OverflowStrategy.dropNew())
                            .throttle(1, FiniteDuration.create(1, TimeUnit.SECONDS), 10, ThrottleMode.shaping())
                            .to(Sink.actorRef(getSender(), NotUsed.getInstance()))
                            .run(actorMaterializer);

                    try (Stream<String> lines = Files.lines(Paths.get(s.replaceFirst("read ", "")))) {
                        lines.forEachOrdered(line-> run.tell(new ResponseMessage(line), getSelf()));
                    } catch (IOException e) {
                        log.error(e.toString());
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}
