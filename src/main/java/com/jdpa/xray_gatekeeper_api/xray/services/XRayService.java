package com.jdpa.xray_gatekeeper_api.xray.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdpa.xray_gatekeeper_api.helpers.Validators;
import com.jdpa.xray_gatekeeper_api.xray.dtos.XrayAppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.models.XrayAuth;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
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
                        XrayAppResponse errMessage = _mapper.readValue(responseBody, XrayAppResponse.class);
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

    public Mono<AppResponse<XrayAppResponse>> PublishJunitToXray(MultipartFile results, MultipartFile info, String token){
        if(token == null || token.isBlank()){
            return Mono.just(AppResponse.failed(null, "Invalid token passed", 400));
        }
        Mono<AppResponse<XrayAppResponse>> objResp;
        if(results == null || info == null){
            return Mono.just(AppResponse.failed(null, "BadRequest: results or info file is null", 400));
        }
        if(!Validators.isXmlFile(results) || !Validators.isJsonFile(info)){
            return Mono.just(AppResponse.failed(null,"Results file must be an XML file and Info file JSON format", 400));
        }
        if(results.getSize() > Validators.valueMegabytes(5) || info.getSize() > Validators.valueMegabytes(5)){
            return Mono.just(AppResponse.failed(null,"File size upload must not exceed 5MB", 400));
        }
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("results", results.getResource())
                .header("Content-Disposition", "form-data; name=file1; filename=" + results.getOriginalFilename());
        builder.part("info", info.getResource())
                .header("Content-Disposition", "form-data; name=file2; filename=" + info.getOriginalFilename());
        // Create the MultiValueMap for the body
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
        // Send the files to the external service using WebClient
        Mono<ResponseEntity<XrayAppResponse>> respEntity = webClient.post()
                .uri("/api/v2/import/execution/junit/multipart")
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                    httpHeaders.setBearerAuth(token); // will automatically format the "Bearer " prefix
                })
                .body(BodyInserters.fromMultipartData(multipartBody))
                .retrieve()
                .toEntity(XrayAppResponse.class);
        objResp = respEntity.flatMap(resp->{
                    int statusCode = resp.getStatusCode().value();
                    XrayAppResponse responseBody = resp.getBody();
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
                        XrayAppResponse errMessage = _mapper.readValue(responseBody, XrayAppResponse.class);
                        responseBody = errMessage.getError();
                    } catch (JsonProcessingException ex) {
                        System.out.println(ex.getMessage());
                    }
                    return Mono.just(AppResponse.failed(null, "Error: " + responseBody, statCode));
                } else {
                    return Mono.just(AppResponse.failed(null, "Error: " + err.getMessage(), 500));
                }
            }).subscribeOn(Schedulers.boundedElastic());
        return objResp;
    }

    //#region Helper method to convert MultipartFile to a File
//    private File convertToFile(MultipartFile file) throws IOException {
//        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
//        file.transferTo(convFile);
//        return convFile;
//    }
    //#endregion
}
