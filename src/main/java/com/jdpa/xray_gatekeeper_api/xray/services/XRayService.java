package com.jdpa.xray_gatekeeper_api.xray.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppError;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.models.XrayAuth;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;

@Service
public class XRayService {
    private final WebClient webClient;

    public XRayService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://xray.cloud.getxray.app/api/v2").build();
    }

    public Mono<AppResponse<String>> AuthenticateXRay(XrayAuth request){
        Mono<AppResponse<String>> objResp;
        try{
            Mono<ResponseEntity<String>> responseEntityMono = webClient.post()
                    .uri("/authenticate")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // Ensure correct content type
                    .bodyValue(request)
                    .retrieve()
                    .toEntity(String.class);

            objResp = responseEntityMono.flatMap(resp->{

                int statusCode = resp.getStatusCode().value();
                String responseBody = resp.getBody();

                if (statusCode == HttpStatus.OK.value()) {
                    // Clean response to remove outer double quotation
                    String cleanedResponse = responseBody != null ? responseBody.replaceAll("^\"|\"$", "") : null;
                    return Mono.just(AppResponse.success(cleanedResponse, statusCode));
                } else {
                    return Mono.just(AppResponse.failed(responseBody, "Non-200 status code: " + statusCode, statusCode));
                }
            }).onErrorResume(err-> {
                if (err instanceof WebClientResponseException webClientResponseException) {
                    int statusCode = webClientResponseException.getStatusCode().value();
                    String responseBody = webClientResponseException.getResponseBodyAsString();
                    ObjectMapper _mapper = new ObjectMapper();
                    try {
                        AppError errMessage = _mapper.readValue(responseBody, AppError.class);
                        responseBody = errMessage.getError();
                    } catch (JsonProcessingException ex) {
                        System.out.println(ex.getMessage());
                    }
                    return Mono.just(AppResponse.failed(responseBody, "Error: " + webClientResponseException.getMessage(), statusCode));
                } else {
                    return Mono.just(AppResponse.failed(null, "Error: " + err.getMessage(), 500));
                }
            }).subscribeOn(Schedulers.boundedElastic());

        }catch(Exception e){
             objResp = Mono.just(AppResponse.failed(null, "Error: " + e.getMessage(), 501));
        }
        return objResp;
    }

    public Mono<AppResponse<String>> PublishJunitToXray(MultipartFile results, MultipartFile info){
        Mono<AppResponse<String>> objResp = Mono.just(AppResponse.success("success",201));

        return objResp;
    }

    //#region Helper method to convert MultipartFile to a File
    private File convertToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        file.transferTo(convFile);
        return convFile;
    }
    //#endregion
}
