package com.jdpa.xray_gatekeeper_api.helpers;

import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends RuntimeException {

    public RateLimitException(final String message) {
        super(message);
    }

    public AppResponse<String> toApiErrorMessage(final String path) {
        return new AppResponse<String>("RateLimitException",this.getMessage(),HttpStatus.TOO_MANY_REQUESTS.value(), false);
    }
}
