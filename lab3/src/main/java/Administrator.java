import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Administrator {
    public static String CLIENT_QUEUE_NAME = "clientinfo";
    public static String ADMIN_QUEUE_NAME = "admininfo";

    public static void main(String[] args) throws Exception {
        // info
        System.out.println("Administrator");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();


        // queue
        channel.queueDeclare(ADMIN_QUEUE_NAME, false, false, false, null);


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
        channel.basicConsume(ADMIN_QUEUE_NAME, false, consumer);
        channel.basicQos(1);

        while (true) {
            String message = br.readLine();
            if (message.equals("exit")) {
                break;
            }
            channel.basicPublish("", CLIENT_QUEUE_NAME, null, message.getBytes());
        }

        // close
        channel.close();
        connection.close();
    }

}
