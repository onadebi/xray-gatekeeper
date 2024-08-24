package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq;

import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.FilesTransferData;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayRateLimitService;
import org.springframework.amqp.core.Message;
import com.rabbitmq.client.Channel;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayService;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver implements ChannelAwareMessageListener {

    private final XRayService _xRayService;
    private final XRayRateLimitService _xrayRateLimitService;

    public Receiver(XRayService xRayService, XRayRateLimitService xrayRateLimitService) {
        this._xRayService = xRayService;
        this._xrayRateLimitService = xrayRateLimitService;
    }

    //#region Overrides
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        try {
            FilesTransferData data = new FilesTransferData().fromJson(messageBody);
            if (data != null) {
                AppResponse<String> rateCheck =  _xrayRateLimitService.rateLimit(data);
                if(rateCheck.isSuccess()){
                    _xRayService.XrayPublishImplementation(data).subscribe(result -> {
                        if (result.isSuccess()) {
                            System.out.println("Report Publish completed!");

                            // Acknowledge the message upon successful processing
                            try {
                                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                                System.out.println("Removed from queue <" + messageBody + ">");
                            } catch (Exception e) {
                                System.err.println("Acknowledgment error: " + e.getMessage());
                            }
                        } else {
                            System.out.println("Publish failed: "+result.getError());
                            // Optionally, you can reject or requeue the message here
                        }
                    });
                }else{
                    System.out.println(String.format("Rate limit exceeded for id: [<%s>]",data.getId()));
                }
            } else {
                //TODO: Log failed to database
                System.out.println("Unable to parse data from message <" + messageBody + ">");
                // Optionally, you can reject or requeue the message here
            }
        } catch (Exception e) {
            System.err.println("Consumer Error: <" + e.getMessage() + ">");
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