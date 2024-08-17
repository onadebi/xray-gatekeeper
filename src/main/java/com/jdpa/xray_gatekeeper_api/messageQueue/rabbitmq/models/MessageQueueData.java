package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class MessageQueueData {
    private String token;
    private String operation;
    private String data;

    public MessageQueueData(String token, String data, String operation) {
        this.data = data;
        this.operation = operation;
        this.token = token;
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String objResp = objectMapper.writeValueAsString(this);
            return objResp;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

}
