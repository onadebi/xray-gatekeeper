package com.jdpa.xray_gatekeeper_api.configurations;

import com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq.Receiver;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = AppConfig.EXCHANGE_NAME;// "XRayOpExchange";
    public static final String QUEUE_NAME = AppConfig.QUEUE_NAME; // "XRayOpQueue";
    public static final String ROUTING_KEY = AppConfig.ROUTING_KEY; // "jdpa.xrayop.routing.key";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                                    Receiver receiver) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(QUEUE_NAME);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL); //SET Acknowledge mode to be manual
        container.setMessageListener(receiver);
        return container;
    }

//    @Bean
//    public MessageListenerAdapter listenerAdapter(Receiver receiver) {
//        return new MessageListenerAdapter(receiver, "receiveMessage");
//    }
}