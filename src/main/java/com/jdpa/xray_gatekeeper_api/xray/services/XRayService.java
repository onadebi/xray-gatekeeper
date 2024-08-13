package com.jdpa.xray_gatekeeper_api.xray.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdpa.xray_gatekeeper_api.helpers.Validators;
import com.jdpa.xray_gatekeeper_api.xray.dtos.XrayAppFeaturesResponse;
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

@Service
public class XRayService {
    private final WebClient webClient;
    private final long FILE_SIZE = System.getenv("FILE_UPLOAD_SIZE") != null ? Integer.parseInt(System.getenv("FILE_UPLOAD_SIZE")) : 5;

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
        if(results.getSize() > Validators.valueMegabytes(FILE_SIZE) || info.getSize() > Validators.valueMegabytes(FILE_SIZE)){
            return Mono.just(AppResponse.failed(null,String.format("File size upload must not exceed %sMB",FILE_SIZE), 400));
        }
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("results", results.getResource())
                .header("Content-Disposition", "form-data; name=results; filename=" + results.getOriginalFilename());
        builder.part("info", info.getResource())
                .header("Content-Disposition", "form-data; name=info; filename=" + info.getOriginalFilename());
        // Create the MultiValueMap for the body
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        Mono<ResponseEntity<XrayAppResponse>> respEntity = webClient.post()
                .uri("/import/execution/junit/multipart")
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

    public Mono<AppResponse<XrayAppResponse>> PublishCucumberToXray(MultipartFile results, MultipartFile info, String token){
        if(token == null || token.isBlank()){
            return Mono.just(AppResponse.failed(null, "Invalid token passed", 400));
        }
        Mono<AppResponse<XrayAppResponse>> objResp;
        if(results == null || info == null){
            return Mono.just(AppResponse.failed(null, "BadRequest: results or info file is null", 400));
        }
        if(!Validators.isJsonFile(results) || !Validators.isJsonFile(info)){
            return Mono.just(AppResponse.failed(null,"Both files 'results' and 'info' file must be JSON format", 400));
        }
        if(results.getSize() > Validators.valueMegabytes(FILE_SIZE) || info.getSize() > Validators.valueMegabytes(FILE_SIZE)){
            return Mono.just(AppResponse.failed(null,String.format("File size upload must not exceed %sMB",FILE_SIZE), 400));
        }
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("results", results.getResource())
                .header("Content-Disposition", "form-data; name=results; filename=" + results.getOriginalFilename());
        builder.part("info", info.getResource())
                .header("Content-Disposition", "form-data; name=info; filename=" + info.getOriginalFilename());
        // Create the MultiValueMap for the body
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        Mono<ResponseEntity<XrayAppResponse>> respEntity = webClient.post()
                .uri("/import/execution/cucumber/multipart")
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

    public Mono<AppResponse<XrayAppFeaturesResponse>> PublishFeatureFileToXray(MultipartFile file, String projectKey, String token){
        if(token == null || token.isBlank()){
            return Mono.just(AppResponse.failed(null, "Invalid token passed", 400));
        }
        Mono<AppResponse<XrayAppFeaturesResponse>> objResp;
        if( file == null){
            return Mono.just(AppResponse.failed(null, "BadRequest: file is null", 400));
        }
        if(file.getSize() > Validators.valueMegabytes(FILE_SIZE) || file.getSize() > Validators.valueMegabytes(FILE_SIZE)){
            return Mono.just(AppResponse.failed(null,String.format("File size upload must not exceed %sMB",FILE_SIZE), 400));
        }
        if(Validators.isFeatureFile(file) ){
            System.out.println("Feature file upload.");
        }else if(Validators.isZipFile(file) ){
            System.out.println("Zipped feature files upload.");
        }else{
            return Mono.just(AppResponse.failed(null,"File is not a feature or zipped features file.", 400));
        }
        

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("file", file.getResource())
                .header("Content-Disposition", "form-data; name=file; filename=" + file.getOriginalFilename());
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        Mono<ResponseEntity<XrayAppFeaturesResponse>> respEntity = webClient.post()
                .uri(String.format("/import/feature?projectKey=%s",projectKey))
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                    httpHeaders.setBearerAuth(token); // will automatically format the "Bearer " prefix
                })
                .body(BodyInserters.fromMultipartData(multipartBody))
                .retrieve()
                .toEntity(XrayAppFeaturesResponse.class);

        objResp = respEntity.flatMap(resp->{
            int statusCode = resp.getStatusCode().value();
            XrayAppFeaturesResponse responseBody = resp.getBody();
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

}
