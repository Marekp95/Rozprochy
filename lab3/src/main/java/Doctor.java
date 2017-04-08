import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Doctor {
    public static String[] DISEASES = {"ankle", "knee", "elbow"};
    public static String QUEUE_NAME = "doctor";

    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("Doctor");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();


        for (String disease : DISEASES) {
            // queue
            String QUEUE_NAME = disease;
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        }


        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // consumer (handle msg)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        // start listening
        channel.basicConsume(QUEUE_NAME, false, consumer);
        channel.basicQos(1);


        while (true) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String message = br.readLine();
            if (message.equals("exit")) {
                break;
            }
            String s[] = message.split(" ", 2);
            if (Arrays.asList(DISEASES).contains(s[0])) {
                channel.basicPublish("", s[0], null, message.getBytes());
                System.out.println("Sent: " + message);
            }

        }

        // close
        channel.close();
        connection.close();
    }
}
