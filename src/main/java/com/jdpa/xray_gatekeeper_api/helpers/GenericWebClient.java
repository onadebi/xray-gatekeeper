package com.jdpa.xray_gatekeeper_api.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.XrayAppResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

public class GenericWebClient {
    private final WebClient webClient;

    public GenericWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public <T,E> Mono<AppResponse<T>> postMultipartRequest(String uri, Map<String, String> headers, MultipartBodyBuilder multipartBody, Class<T> responseType, Class<E> errorResponseType, String token) {
        Map<String, String> finalHeaders = headers == null ? new HashMap<>(): headers;
        Mono<AppResponse<T>> objResp =  webClient.post()
                .uri(uri)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                    httpHeaders.setBearerAuth(token); // Automatically adds "Bearer " prefix
                    finalHeaders.forEach(httpHeaders::set); // Set additional custom headers if any
                })
                .body(BodyInserters.fromMultipartData(multipartBody.build()))
                .retrieve()
                .toEntity(responseType)
                .flatMap(resp->{
                    int statusCode = resp.getStatusCode().value();
                    T responseBody = resp.getBody();
                    if (statusCode == HttpStatus.OK.value()) {
                        return Mono.just(AppResponse.success(responseBody, statusCode));
                    } else {
                        return Mono.just(AppResponse.failed(responseBody, "Non-200 status code: " + statusCode, statusCode));
                    }
                }).onErrorResume(err-> {
                    if (err instanceof WebClientResponseException webClientResponseException) {
                        int statCode = webClientResponseException.getStatusCode().value();
                        String responseBody = webClientResponseException.getResponseBodyAsString();
                        ObjectMapper _mapper = new ObjectMapper();
                        try {
                            E errMessage = _mapper.readValue(responseBody, errorResponseType);
                            if (errMessage instanceof XrayAppResponse) {
                                responseBody = ((XrayAppResponse)errMessage).getError();
                            }
                        } catch (JsonProcessingException ex) {
                            System.out.println(ex.getMessage());
                        }
                        return Mono.just(AppResponse.failed(null, "Error: " + responseBody, statCode));
                    } else {
                        return Mono.just(AppResponse.failed(null, "Error: " + err.getMessage(), 500));
                    }
                });
        return objResp;
    }
}
