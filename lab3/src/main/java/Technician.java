import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Technician {

    public static void main(String[] argv) throws Exception {
        // info
        System.out.println("Technician");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String[] specializations = br.readLine().split(" ");


        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(Doctor.QUEUE_NAME, false, false, false, null);

        for (String specialization : specializations) {
            // queue
            String QUEUE_NAME = specialization;
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            // consumer (handle msg)
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    String s[] = message.split(" ", 2);
                    System.out.println("Received: " + s[1]);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                    channel.basicPublish("", Doctor.QUEUE_NAME + s[0], null, ("done: " + s[1]).getBytes());
                    channel.basicPublish("", Administrator.ADMIN_QUEUE_NAME, null, ("done: " + s[1]).getBytes());

                }
            };

            channel.basicConsume(QUEUE_NAME, false, consumer);//
            channel.basicQos(1);
        }

        {
            channel.queueDeclare(Administrator.CLIENT_QUEUE_NAME, false, false, false, null);
            // consumer (handle msg)
            Consumer infoConsumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Received info: " + message);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };

            // start listening
            channel.basicConsume(Administrator.CLIENT_QUEUE_NAME, false, infoConsumer);
            channel.basicQos(1);
        }
    }

}
