package akka.server;

import akka.ResponseMessage;
import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class OrderActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final File file;

    public OrderActor(){
        file = new File("orders.txt");
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    System.out.println("order: " + s);
                    synchronized (ServerApp.class) {
                        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, true), true);
                        printWriter.println(s.replace("order ", ""));
                        printWriter.close();
                    }

                    sender().tell(new ResponseMessage("done"), getSelf());//sender null?
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    @Override
    public void preStart() throws Exception {

    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder.
                matchAny(o -> SupervisorStrategy.restart()).
                build());
    }
}
