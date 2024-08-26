package com.jdpa.xray_gatekeeper_api.xray.dtos;

import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestStatusDTO {
    private long count;
    private String status;

    public RequestStatusDTO(long count, String status) {
        this.count = count;
        this.status = status;
    }

    public RequestStatusDTO(long count, XRayRequestLogs.Status status) {
        this.count = count;
        this.status = status.name();
    }
}
