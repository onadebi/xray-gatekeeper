package com.jdpa.xray_gatekeeper_api.xray.dtos;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class XrayAppFeaturesResponse {
    String[] errors;
    XrayAppResponse[] updatedOrCreatedTests;
    XrayAppResponse[] updatedOrCreatedPreconditions;

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
