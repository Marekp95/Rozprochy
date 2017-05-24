package akka.server;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class DbWorker extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String fileName;

    public DbWorker(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
                        String bookName = s.replace("find ", "");
                        Optional<String> optional = lines.filter(line -> {
                            String[] words = line.split("::");

                            return (words[0].equals(bookName));
                        }).findFirst();
                        if (optional.isPresent()) {
                            getSender().tell(Integer.valueOf(optional.get().split("::")[1]), getSelf());
                        } else {
                            getSender().tell(-1, getSelf());
                        }
                    } catch (IOException e) {
                        log.error(e.toString());
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }
}