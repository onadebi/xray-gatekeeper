package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq;

import org.springframework.stereotype.Component;

@Component
public class Receiver {

    public void receiveMessage(String message) {
        //TODO: IMPLEMENT RATE LIMITATION HERE with ACTIONs to be TAKEN on DATA
        //TODO: possibly implement function in RabbitMqSenderService/XRayService and Inject here
        System.out.println("Received <" + message + ">");
    }
}