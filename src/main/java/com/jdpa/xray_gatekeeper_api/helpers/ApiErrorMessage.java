package com.jdpa.xray_gatekeeper_api.helpers;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiErrorMessage {

    private final UUID id = UUID.randomUUID();
    private final int status;
    private final String error;
    private final String message;
    private final LocalDateTime timestamp = LocalDateTime.now(Clock.systemUTC());
    private final String path;


    public ApiErrorMessage(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiErrorMessage() {
        this.status = 0;
        error = null;
        message = "";
        path = null;
    }
}