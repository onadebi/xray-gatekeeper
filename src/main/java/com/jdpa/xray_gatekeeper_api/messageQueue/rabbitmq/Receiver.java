package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq;

import org.springframework.stereotype.Component;

@Component
public class Receiver {

    public void receiveMessage(String message) {
        //TODO: IMPLEMENT RATE LIMITATION HERE with ACTIONs to be TAKEN on DATA
        //TODO: possibly implement function in RabbitMqSenderService/XRayService and Inject here
        System.out.println("Received <" + message + ">");
    }

    //#region Manual acknowledgement
//    @RabbitListener(queues = "yourQueueName", ackMode = "MANUAL")
//    public void receiveMessage(Message message, Channel channel) throws Exception {
//        try {
//            String messageBody = new String(message.getBody());
//            System.out.println("Received <" + messageBody + ">");
//
//            // Do processing here
//
//            // Manually acknowledge the message
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        } catch (Exception e) {
//            // If there's an issue, you can reject the message and requeue or discard
//            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // true to requeue
//        }
//    }
    //#endregion
}