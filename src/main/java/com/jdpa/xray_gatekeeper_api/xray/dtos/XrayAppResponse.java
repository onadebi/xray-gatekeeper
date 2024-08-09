package com.jdpa.xray_gatekeeper_api.xray.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class XrayAppResponse {
    // Getter and Setter
    private String error;
    private String id;
    private String key;
    private String self;
}
