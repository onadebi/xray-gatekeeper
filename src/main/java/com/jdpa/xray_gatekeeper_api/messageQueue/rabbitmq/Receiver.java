package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq;

import com.jdpa.xray_gatekeeper_api.xray.dtos.FilesTransferData;
import org.springframework.amqp.core.Message;
import com.rabbitmq.client.Channel;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayService;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver implements ChannelAwareMessageListener {

    private final XRayService _xRayService;
    public Receiver(XRayService xRayService) {
        this._xRayService = xRayService;
    }

    //#region Overrides
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        //TODO: IMPLEMENT RATE LIMITATION HERE with ACTIONs to be TAKEN on DATA
        try {
            FilesTransferData data = new FilesTransferData().fromJson(messageBody);
            if (data != null) {
                _xRayService.XrayPublishImplementation(data).subscribe(result -> {
                    if (result.isSuccess()) {
                        System.out.println("Report Publish completed!");

                        // Acknowledge the message upon successful processing
                        try {
                            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                            System.out.println("Removed from queue <" + messageBody + ">");
                        } catch (Exception e) {
                            System.out.println("Acknowledgment error: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Publish failed: "+result.getError());
                        // Optionally, you can reject or requeue the message here
                    }
                });
            } else {
                //TODO: Log failed to database
                System.out.println("Unable to parse data from message <" + messageBody + ">");
                // Optionally, you can reject or requeue the message here
            }
        } catch (Exception e) {
            System.out.println("Consumer Error: <" + e.getMessage() + ">");
        }
    }
    //#endregion

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