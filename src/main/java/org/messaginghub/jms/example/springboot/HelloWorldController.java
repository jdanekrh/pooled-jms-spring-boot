package org.messaginghub.jms.example.springboot;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsQueue;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.jms.*;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@EnableAutoConfiguration
public class HelloWorldController {
    @Autowired
    ConnectionFactory connectionFactory;

    private Queue queue = new JmsQueue("MyLiQueueRe");
    /// makes message content more interesting
    AtomicInteger counter = new AtomicInteger(0);

    @RequestMapping("/")
    @ResponseBody
    String home() {
        return "You can <a href=\"/send\">send</a> and then <a href=\"/receive\">receive</a> a message here.";
    }

    @RequestMapping("/send")
    @ResponseBody
    String send() throws JMSException {
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(queue);
        ) {
            connection.start();
            final TextMessage message = session.createTextMessage();
            message.setText("Message no. " + counter.getAndIncrement());
            producer.send(message);
            return "Sent: " + message.getText() + ". Now proceed by <a href=\"/receive\">receiving</a> it.";
        }
    }

    @RequestMapping("/receive")
    @ResponseBody
    String receive() throws JMSException {
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(queue);
        ) {
            connection.start();
            final Message message = consumer.receive(2000);
            if (message == null) {
                return "Did not receive any message. You can <a href=\"/send\">send</a> some.";
            }
            return "Received message: " + ((TextMessage) message).getText();
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(HelloWorldController.class, args);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        System.out.println("created pool");
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory();
        connectionFactory.setRemoteURI("failover:(amqp://localhost:5672)");
        connectionFactory.setUsername("admin");
        connectionFactory.setPassword("admin");

        JmsPoolConnectionFactory pool = new org.messaginghub.pooled.jms.JmsPoolConnectionFactory();
        pool.setConnectionFactory(connectionFactory);
        pool.setUseAnonymousProducers(false);
        return pool;
    }
}