package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.jdpa.xray_gatekeeper_api.configurations.RabbitMqConfig.EXCHANGE_NAME;
import static com.jdpa.xray_gatekeeper_api.configurations.RabbitMqConfig.ROUTING_KEY;

@Service
public class RabbitMqSenderService {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public RabbitMqSenderService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, message);
        System.out.println("Sent <" + message + ">");
    }

}
