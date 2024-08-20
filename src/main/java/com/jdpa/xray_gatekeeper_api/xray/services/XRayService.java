package com.jdpa.xray_gatekeeper_api.xray.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdpa.xray_gatekeeper_api.helpers.FileUtils;
import com.jdpa.xray_gatekeeper_api.helpers.GenericWebClient;
import com.jdpa.xray_gatekeeper_api.helpers.Validators;
import com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq.models.MessageQueueData;
import com.jdpa.xray_gatekeeper_api.messageQueue.rabbitmq.services.RabbitMqSenderService;
import com.jdpa.xray_gatekeeper_api.xray.dtos.*;
import com.jdpa.xray_gatekeeper_api.xray.models.ActivityLog;
import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs;
import com.jdpa.xray_gatekeeper_api.xray.models.XrayAuth;
import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs.Status;
import com.jdpa.xray_gatekeeper_api.xray.repository.ActivityLogRepository;
import com.jdpa.xray_gatekeeper_api.xray.repository.XRayServiceRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class XRayService {
    private final WebClient webClient;
    private final long FILE_SIZE = System.getenv("FILE_UPLOAD_SIZE") != null ? Integer.parseInt(System.getenv("FILE_UPLOAD_SIZE")) : 5;
    private final String UPLOAD_PATH = "Uploads";
    private final XRayServiceRepository _xrayServiceRepository;
    private final RabbitMqSenderService _rabbitMqService;
    private final ActivityLogRepository _activityLogRepository;


    public XRayService(WebClient.Builder webClientBuilder, XRayServiceRepository xrayServiceRepository
    , RabbitMqSenderService rabbitMqService
    , ActivityLogRepository activityLogRepository) {
        this.webClient = webClientBuilder.baseUrl("https://xray.cloud.getxray.app/api/v2").build();
        this._xrayServiceRepository = xrayServiceRepository;
        this._rabbitMqService = rabbitMqService;
        this._activityLogRepository = activityLogRepository;
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
                    this.SaveRequestLog("--", "--", Status.COMPLETED, OperationEnum.AuthenticateXRay, "");
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
        Mono<AppResponse<XrayAppResponse>> objResp = Mono.just(AppResponse.failed(null,"",400));
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

        _rabbitMqService.sendMessage(new MessageQueueData(token,"rabbitmq test Data", Validators.getCurrentMethodName()).toString());

        // Create the MultiValueMap for the body
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

        GenericWebClient client = new GenericWebClient(webClient);
        String xRayJunitEndpoint = "/import/execution/junit/multipart";

        Mono<AppResponse<XrayAppResponse>> respEntity = client.postMultipartRequest(xRayJunitEndpoint,null,builder, XrayAppResponse.class, XrayAppResponse.class, token);

        objResp = respEntity.subscribeOn(Schedulers.boundedElastic());
        return objResp;
    }

    public Mono<AppResponse<String>> PublishCucumberToXray(MultipartFile results, MultipartFile info, String token){
        if(token == null || token.isBlank()){
            return Mono.just(AppResponse.failed(null, "Invalid token passed", 400));
        }
        Mono<AppResponse<String>> objResp;
        if(results == null || info == null){
            return Mono.just(AppResponse.failed(null, "BadRequest: results or info file is null", 400));
        }
        if(!Validators.isJsonFile(results) || !Validators.isJsonFile(info)){
            return Mono.just(AppResponse.failed(null,"Both files 'results' and 'info' file must be JSON format", 400));
        }
        if(results.getSize() > Validators.valueMegabytes(FILE_SIZE) || info.getSize() > Validators.valueMegabytes(FILE_SIZE)){
            return Mono.just(AppResponse.failed(null,String.format("File size upload must not exceed %sMB",FILE_SIZE), 400));
        }

        //#region Async replacement
        List<MultipartFile> renamedFiles = FileUtils.renameFilesWithUUID(new MultipartFile[]{results, info});
        List<MultipartFile> files = new ArrayList<>(renamedFiles);

        Map<String, List<MultipartFile>> fileObjData = new HashMap<>();
        fileObjData.put(token, files);

        FilesTransferData filesData = new FilesTransferData().AddNew(token, fileObjData, OperationEnum.PublishCucumberToXray.name(),"/import/execution/cucumber/multipart", null);

        List<String> allFileNames = new ArrayList<>();
        allFileNames.add(String.format("results:%s",renamedFiles.get(0).getOriginalFilename()));
        allFileNames.add(String.format("info:%s",renamedFiles.get(1).getOriginalFilename()));
        filesData.setFileNames(allFileNames.toArray(String[]::new));

        String filesDataJson = filesData.toJson();
        long dbId = this.SaveRequestLog("--", "--", Status.PENDING, OperationEnum.PublishCucumberToXray, filesDataJson);
        if(dbId > 0){
            filesData.setId(dbId);
            this.SaveFiles(filesData.getFilesData());
            filesDataJson = filesData.toJson();
            if(!filesDataJson.contains("ERROR")){
                _rabbitMqService.sendMessage(filesData.toJson());
                objResp = Mono.just(AppResponse.success("Successfully queued.", 201));
            }else{
                objResp = Mono.just(AppResponse.failed(null,"Failed to queue request: "+filesDataJson, 500));
            }
        }else{
            objResp = Mono.just(AppResponse.failed(null,"Failed to queue request.", 400));
        }
        //#endregion
        return objResp;
    }

    public Mono<AppResponse<String>> PublishFeatureFileToXray(MultipartFile file, String projectKey, String token){
        if(token == null || token.isBlank()){
            return Mono.just(AppResponse.failed(null, "Invalid token passed", 400));
        }
        Mono<AppResponse<String>> objResp = Mono.just(AppResponse.failed(null, "Not Modified", 304));
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

        List<MultipartFile> renamedFiles = FileUtils.renameFilesWithUUID(new MultipartFile[]{file});
        List<MultipartFile> files = new ArrayList<>(renamedFiles);
        // Create a map and associate the list of files with a key (e.g., "file")
        Map<String, List<MultipartFile>> fileObjData = new HashMap<>();
        fileObjData.put(token, files);  // Adding the list of files to the map with a key "file"
        
        FilesTransferData filesData = new FilesTransferData().AddNew(token, fileObjData, OperationEnum.PublishFeatureFileToXray.name(),String.format("/import/feature?projectKey=%s",projectKey), null);
        List<String> fileNames = new ArrayList<>();
        for(String itemName :filesData.getFileNames()){
            fileNames.add(String.format("file:%s",itemName));
        }
        filesData.setFileNames(fileNames.toArray(String[]::new));
        String filesDataJson = filesData.toJson();
        long dbId = this.SaveRequestLog("--", "--", Status.PENDING, OperationEnum.PublishFeatureFileToXray, filesDataJson);
        if(dbId > 0){
            filesData.setId(dbId);
            this.SaveFiles(filesData.getFilesData());
            filesDataJson = filesData.toJson();
            if(!filesDataJson.contains("ERROR")){
                _rabbitMqService.sendMessage(filesData.toJson());
                objResp = Mono.just(AppResponse.success("Successfully queued.", 201));
            }else{
                objResp = Mono.just(AppResponse.failed(null,"Failed to queue request: "+filesDataJson, 500));
            }
        }else{
            objResp = Mono.just(AppResponse.failed(null,"Failed to queue request.", 400));
        }
        return objResp;
    }

    public Mono<Boolean> XrayPublishImplementation(FilesTransferData data){
        boolean outcome = false;
        Mono<Boolean> outResp = Mono.just(false);
//        Mono<AppResponse<XrayAppFeaturesResponse>> objResp = Mono.just(AppResponse.failed(null, "NO CHANGES", 304));
        try {
            if (data != null) {
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                if(data.getFileNames().length > 0){
                    Path filesDirectory = Paths.get(UPLOAD_PATH);
                    for (String fileNameData : data.getFileNames()) {
                        String[] fileObject = fileNameData.split(":");
                        String fileName = fileObject[1];
                        String fileSubmissionName = fileObject[0];
                        Path filesPath = filesDirectory.resolve(fileName);
                        if (Files.exists(filesPath)) {
                            try{
                                // Create an InputStreamResource from the file path
                                InputStreamResource resource = new InputStreamResource(Files.newInputStream(filesPath));
//                                if (data.getOperation().equals(OperationEnum.PublishFeatureFileToXray.name())) {
                                    builder.part(fileSubmissionName, resource)
                                            .header("Content-Disposition", String.format("form-data; name=%s; filename=%s",fileSubmissionName ,fileName));
//                                }
//                                else if (data.getOperation().equals(OperationEnum.PublishCucumberToXray.name())) {
//                                    builder.part("results", results.getResource())
//                                            .header("Content-Disposition", "form-data; name=results; filename=" + results.getOriginalFilename());
//                                    builder.part("info", info.getResource())
//                                            .header("Content-Disposition", "form-data; name=info; filename=" + info.getOriginalFilename());
//                                }else{
//
//                                }
                            }catch (Exception e){
                                CompletableFuture.runAsync(() -> {
                                    ActivityLog activityLog = new ActivityLog().AddNew("XRAY_PUBLISH_REPORT", "Error reading file from disk: " + data.getId() + " - " + e.getMessage());
                                    _activityLogRepository.saveAndFlush(activityLog);
                                }, Executors.newCachedThreadPool());
                            }
                        }else {
                            CompletableFuture.runAsync(() -> {
                            ActivityLog activityLog = new ActivityLog().AddNew("XRAY_PUBLISH_REPORT", String.format("File upload is null for id %s: | %s", data.getId(), fileName));
                            _activityLogRepository.saveAndFlush(activityLog);
                            }, Executors.newCachedThreadPool());
                        }
                    }
                }
                //#region Obsolete
//                if (data.getFilesData() != null) {
//                    Path filesDirectory = Paths.get(UPLOAD_PATH);
//                    for (Map.Entry<String, List<MultipartFile>> entry : data.getFilesData().entrySet()) {
//                        for (MultipartFile file : entry.getValue()) {
//                            if (file != null && !file.isEmpty()) {
//                                Path filesPath = filesDirectory.resolve(file.getOriginalFilename());
//                                if (!Files.exists(filesPath)) {
//                                    try {
//                                        // Create an InputStreamResource from the file path
//                                        InputStreamResource resource = new InputStreamResource(Files.newInputStream(filesPath));
//                                        if (data.getOperation().equals(OperationEnum.PublishFeatureFileToXray.name())) {
//                                            builder.part("file", resource)
//                                                    .header("Content-Disposition", "form-data; name=file; filename=" + file.getOriginalFilename());
//                                        }
//                                    }catch (Exception e){
//                                        // Handle the exception appropriately
//                                        ActivityLog activityLog = new ActivityLog().AddNew("XRAY_PUBLISH_REPORT", "Error reading file from disk: " + data.getId() + " - " + e.getMessage());
//                                        _activityLogRepository.saveAndFlush(activityLog);
//                                    }
//                                }
//                            } else {
//                                ActivityLog activityLog = new ActivityLog().AddNew("XRAY_PUBLISH_REPORT", "File upload is null for: " + entry.getKey());
//                                _activityLogRepository.saveAndFlush(activityLog);
//                            }
//                        }
//                    }
//
//                }
                //#endregion
                GenericWebClient client = new GenericWebClient(webClient);
                String xRayFeatureEndpoint = String.format(data.getUrl());
                if(data.getOperation().equals(OperationEnum.PublishFeatureFileToXray.name())){
                    Mono<AppResponse<XrayAppFeaturesResponse>> respEntity = client.postMultipartRequest(xRayFeatureEndpoint, null, builder, XrayAppFeaturesResponse.class, XrayAppResponse.class, data.getToken());
                    Mono<AppResponse<XrayAppFeaturesResponse>> objResp = respEntity.subscribeOn(Schedulers.boundedElastic());
                    outResp = objResp.map(AppResponse->{
                        String error = AppResponse.getError();
                        XrayAppFeaturesResponse body = AppResponse.getResult();
                        if (body != null) {
                            if(body.getErrors().length > 0){
                                this.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED_WITH_ERROR,body.getErrors()[0]);
                            }else{
                                this.UpdateRequestLog(data.getId(),String.format("%s | %s",body.getUpdatedOrCreatedTests()[0].toString(),body.getUpdatedOrCreatedPreconditions()[0].toString()), Status.COMPLETED, null);
                            }
                        }
                        return AppResponse.isSuccess();
                    });
                }else if(data.getOperation().equals(OperationEnum.PublishCucumberToXray.name())){
                    Mono<AppResponse<XrayAppResponse>> respEntity =  client.postMultipartRequest(xRayFeatureEndpoint, null, builder, XrayAppResponse.class, XrayAppResponse.class, data.getToken());
                    Mono<AppResponse<XrayAppResponse>> objResp = respEntity.subscribeOn(Schedulers.boundedElastic());
                    outResp = objResp.map(AppResponse->{
                        String error = AppResponse.getError();
                        XrayAppResponse body = AppResponse.getResult();
                        if (body != null) {
                            if(body.getError() != null){
                                this.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED_WITH_ERROR,body.getError());
                            }else{
                                this.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED, null);
                            }
                        }
                        return AppResponse.isSuccess();
                    });
                }
                
            }
        }catch(Exception e){
            CompletableFuture.runAsync(() -> {
                ActivityLog activityLog = new ActivityLog().AddNew("REPORT_PUBLISH_ERROR", String.format("Failed to publish report for id: %s |Error: %s |Data: %s", data.getId(), e.getMessage(), data.toJson()));
                _activityLogRepository.saveAndFlush(activityLog);
            }, Executors.newCachedThreadPool());
        }
        return outResp;
    }


//#region HELPERS
    @Async
    protected long SaveRequestLog(String filePath, String fileNames, XRayRequestLogs.Status status, OperationEnum operation, String data){
        XRayRequestLogs newObj = new XRayRequestLogs().AddNew(filePath, fileNames, status, operation.name(),data);
        _xrayServiceRepository.saveAndFlush(newObj);
        System.out.printf("Operation: %s | ID: %s%n", operation, newObj.getId());
        return  newObj.getId();
    }

    @Async
    protected void UpdateRequestLog(long id, String response, Status status, String error){
        try {
            Optional<XRayRequestLogs> optionalLog = _xrayServiceRepository.findById(id);
            if (optionalLog.isPresent()) {
                XRayRequestLogs log = optionalLog.get();
                log.setResponse(response);
                log.setStatus(status);
                if(error != null){
                    log.setError(error);
                }
                _xrayServiceRepository.save(log);
            }
        }catch(Exception ex){
            //TODO: Log exception activity
            System.out.println("Update request log failed. Error: " + ex.getMessage());
        }
    }

    @Async
    protected void SaveFiles(Map<String, List<MultipartFile>> files){
        // Define the upload directory
        Path uploadPath = Paths.get(UPLOAD_PATH);
        try {
            // Create the "Uploads" directory if it does not exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            // Loop through the map and save each file in the "Uploads" folder
            for (Map.Entry<String, List<MultipartFile>> entry : files.entrySet()) {
                for (MultipartFile file : entry.getValue()) {
                    if (file != null && !file.isEmpty()) {
                        // Resolve the path for each file
                        Path filePath = uploadPath.resolve(Objects.requireNonNull(file.getOriginalFilename()));

                        // Save the file to the directory
                        Files.write(filePath, file.getBytes());
                    }else{
                        ActivityLog activityLog = new ActivityLog().AddNew("FILE_UPLOAD_EMPTY","File upload is null for: "+entry.getKey());
                        _activityLogRepository.saveAndFlush(activityLog);
                    }
                }
            }

        } catch (IOException e) {
            ActivityLog activityLog = new ActivityLog().AddNew("FILE_UPLOAD_ERROR","File upload encountered an error: "+e.getMessage());
            _activityLogRepository.saveAndFlush(activityLog);
        }
    }
//#endregion


}
