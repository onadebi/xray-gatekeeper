package com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MessageData {
    private String token;
    private String operation;
    private String data;

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
