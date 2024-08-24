package com.jdpa.xray_gatekeeper_api.helpers;

import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    @ResponseBody
    public AppResponse<String> handleRateLimitException(RateLimitException ex) {
        AppResponse<String> errorMessage = ex.toApiErrorMessage("");
        return errorMessage;
    }
}