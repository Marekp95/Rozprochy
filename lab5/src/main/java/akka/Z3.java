package akka;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import java.util.concurrent.CompletionStage;

public class Z3 {

    public static void main(String[] argv) throws Exception {

        final ActorSystem system = ActorSystem.create("stream_system");
        final Materializer materializer = ActorMaterializer.create(system);
        //final ActorRef actor = system.actorOf(Props.create(Z3_SinkActor.class), "sink");

        final Source<Integer, NotUsed> source = Source.range(1, 10);    
        final Flow flow = Flow.of(Integer.class).map(val -> val * 2);
        
        final Sink<Integer, CompletionStage<Done>> sinkPrint = Sink.foreach(i -> System.out.println(i));        
        final Sink<Integer, CompletionStage<Done>> sinkCombined = flow.toMat(sinkPrint, Keep.right());
        
        final CompletionStage<Done> done = source.runWith(sinkCombined, materializer);
        done.thenRun(() -> system.terminate());
        
    }
}
