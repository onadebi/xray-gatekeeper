package com.jdpa.xray_gatekeeper_api.xray.dtos;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class XrayAppFeaturesResponse {
    String[] errors;
    XrayAppResponse[] updatedOrCreatedTests;
    XrayAppResponse[] updatedOrCreatedPreconditions;
}
