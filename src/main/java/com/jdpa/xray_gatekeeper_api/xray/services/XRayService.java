package com.jdpa.xray_gatekeeper_api.xray.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdpa.xray_gatekeeper_api.helpers.FileUtils;
import com.jdpa.xray_gatekeeper_api.helpers.GenericWebClient;
import com.jdpa.xray_gatekeeper_api.helpers.Validators;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
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
    private final XRayRequestLogsDBService _xrayRequestLogsDBService;
    private final RabbitMqSenderService _rabbitMqService;
    private final ActivityLogRepository _activityLogRepository;


    public XRayService(WebClient.Builder webClientBuilder, XRayServiceRepository xrayServiceRepository, XRayRequestLogsDBService xrayRequestLogsDBService
            , RabbitMqSenderService rabbitMqService
    , ActivityLogRepository activityLogRepository) {
        this.webClient = webClientBuilder.baseUrl("https://xray.cloud.getxray.app/api/v2").build();
        this._xrayRequestLogsDBService = xrayRequestLogsDBService;
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
                    //TODO: May not need to be logged.
                    _xrayRequestLogsDBService.SaveRequestLog("--", "--", Status.COMPLETED, OperationEnum.AuthenticateXRay, "");
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

    public Mono<AppResponse<String>> PublishJunitToXray(MultipartFile results, MultipartFile info, String token){
        if(token == null || token.isBlank()){
            return Mono.just(AppResponse.failed(null, "Invalid token passed", 400));
        }
        Mono<AppResponse<String>> objResp = Mono.just(AppResponse.failed(null,"",400));
        if(results == null || info == null){
            return Mono.just(AppResponse.failed(null, "BadRequest: results or info file is null", 400));
        }
        if(!Validators.isXmlFile(results) || !Validators.isJsonFile(info)){
            return Mono.just(AppResponse.failed(null,"Results file must be an XML file and Info file JSON format", 400));
        }
        if(results.getSize() > Validators.valueMegabytes(FILE_SIZE) || info.getSize() > Validators.valueMegabytes(FILE_SIZE)){
            return Mono.just(AppResponse.failed(null,String.format("File size upload must not exceed %sMB",FILE_SIZE), 400));
        }

        //#region Async replacement
        List<MultipartFile> renamedFiles = FileUtils.renameFilesWithUUID(new MultipartFile[]{results, info});
        List<MultipartFile> files = new ArrayList<>(renamedFiles);

        Map<String, List<MultipartFile>> fileObjData = new HashMap<>();
        fileObjData.put(token, files);

        FilesTransferData filesData = new FilesTransferData().AddNew(token, fileObjData, OperationEnum.PublishJunitToXray.name(),"/import/execution/junit/multipart", null);

        List<String> allFileNames = new ArrayList<>();
        allFileNames.add(String.format("results:%s",renamedFiles.get(0).getOriginalFilename()));
        allFileNames.add(String.format("info:%s",renamedFiles.get(1).getOriginalFilename()));
        filesData.setFileNames(allFileNames.toArray(String[]::new));

        String filesDataJson = filesData.toJson();
        long dbId = _xrayRequestLogsDBService.SaveRequestLog("--", "--", Status.PENDING, OperationEnum.PublishJunitToXray, filesDataJson);
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
        long dbId = _xrayRequestLogsDBService.SaveRequestLog("--", "--", Status.PENDING, OperationEnum.PublishCucumberToXray, filesDataJson);
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
        long dbId = _xrayRequestLogsDBService.SaveRequestLog("--", "--", Status.PENDING, OperationEnum.PublishFeatureFileToXray, filesDataJson);
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

    public Mono<AppResponse<Boolean>> XrayPublishImplementation(FilesTransferData data){
        Mono<AppResponse<Boolean>> httpResp = Mono.just(AppResponse.failed(false, "NO CHANGES", 304));
        try {
            if (data != null) {
                //#region Check that request is not cancelled before processing of request
                AppResponse<XRayRequestLogs> dataCheck = _xrayRequestLogsDBService.GetRequestLogBy(data.getId());
                if (dataCheck.getResult() != null && dataCheck.getResult().getId() > 0) {
                    if(dataCheck.getResult().getStatus() == Status.CANCELED){
                        return Mono.just(AppResponse.success(true,  304));
                    }
                }
                //#endregion
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
                                    builder.part(fileSubmissionName, resource)
                                            .header("Content-Disposition", String.format("form-data; name=%s; filename=%s",fileSubmissionName ,fileName));
                            }catch (Exception e){
                                CompletableFuture.runAsync(() -> {
                                    ActivityLog activityLog = new ActivityLog().AddNew("XRAY_PUBLISH_REPORT", "Error reading file from disk: " + data.getId() + " - " + e.getMessage());
                                    _activityLogRepository.saveAndFlush(activityLog);
                                }, Executors.newCachedThreadPool());
                            }
                        }else {
                            CompletableFuture.runAsync(() -> {
                            ActivityLog activityLog = new ActivityLog().AddNew("FILE_NOT_FOUND", String.format("File upload not found for id %s: |Filename: %s |Operation: %s", data.getId(), fileName, data.getOperation()));
                            _activityLogRepository.saveAndFlush(activityLog);
                            }, Executors.newCachedThreadPool());
                        }
                    }
                }

                GenericWebClient client = new GenericWebClient(webClient);
                String xRayFeatureEndpoint = String.format(data.getUrl());
                if(data.getOperation().equals(OperationEnum.PublishFeatureFileToXray.name())){
                    Mono<AppResponse<XrayAppFeaturesResponse>> respEntity = client.postMultipartRequest(xRayFeatureEndpoint, null, builder, XrayAppFeaturesResponse.class, XrayAppResponse.class, data.getToken());
                    Mono<AppResponse<XrayAppFeaturesResponse>> objResp = respEntity.subscribeOn(Schedulers.boundedElastic());

                    httpResp = objResp.map(AppResponse->{
                        String error = AppResponse.getError();
                        int StatCode = AppResponse.getStatCode();
                        XrayAppFeaturesResponse body = AppResponse.getResult();

                        if(error != null && !error.isEmpty()){
                            _xrayRequestLogsDBService.UpdateRequestLog(data.getId(),body != null ? body.toString(): null, Status.ERROR, ((body != null && body.getErrors() != null && body.getErrors().length > 0) ? body.getErrors()[0]: ":")+error);
                        }
                        if (body != null) {
                            if(body.getErrors().length > 0){
                                _xrayRequestLogsDBService.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED_WITH_ERROR,body.getErrors()[0]);
                            }else{
//                                String allResponse = body.toString();
//                                String getUpdatedOrCreated = (body.getUpdatedOrCreatedTests() != null && body.getUpdatedOrCreatedTests().length> 0) ? body.getUpdatedOrCreatedTests()[0].toString(): null;
//                                String getUpdatedOrCreatedPreconditions = (body.getUpdatedOrCreatedPreconditions() != null && body.getUpdatedOrCreatedPreconditions().length > 0) ? body.getUpdatedOrCreatedPreconditions()[0].toString(): null;
                                _xrayRequestLogsDBService.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED, null);
                            }
                        }
                        return new AppResponse<Boolean>(AppResponse.isSuccess(), ((body != null && body.getErrors() != null && body.getErrors().length > 0) ? body.getErrors()[0]: ":")+error,StatCode,AppResponse.isSuccess());
                    });
                }else if(data.getOperation().equals(OperationEnum.PublishCucumberToXray.name()) || data.getOperation().equals(OperationEnum.PublishJunitToXray.name())){
                    Mono<AppResponse<XrayAppResponse>> respEntity =  client.postMultipartRequest(xRayFeatureEndpoint, null, builder, XrayAppResponse.class, XrayAppResponse.class, data.getToken());
                    Mono<AppResponse<XrayAppResponse>> objResp = respEntity.subscribeOn(Schedulers.boundedElastic());

                      httpResp = objResp.map(AppResponse->{
                        String error = AppResponse.getError();
                        int StatCode = AppResponse.getStatCode();
                        XrayAppResponse body = AppResponse.getResult();

                        if(error != null && !error.isEmpty()){
                            _xrayRequestLogsDBService.UpdateRequestLog(data.getId(),body != null ? body.toString(): null, Status.ERROR, error + (body != null ? body.getError(): ""));
                        }else if (body != null){
                            if(body.getError() != null){
                                _xrayRequestLogsDBService.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED_WITH_ERROR,body.getError());
                            }else{
                                _xrayRequestLogsDBService.UpdateRequestLog(data.getId(),body.toString(), Status.COMPLETED, null);
                            }
                        }
                        return new AppResponse<Boolean>(AppResponse.isSuccess(), (body != null ? body.getError(): ":")+error,StatCode,AppResponse.isSuccess());
                    });
                }
            }else{
                System.out.println("No data found from queue object for processing!");
            }
        }catch(Exception e){
            CompletableFuture.runAsync(() -> {
                ActivityLog activityLog = new ActivityLog().AddNew("REPORT_PUBLISH_ERROR", String.format("Failed to publish report for id: %s |Error: %s |Data: %s", data.getId(), e.getMessage(), data.toJson()));
                _activityLogRepository.saveAndFlush(activityLog);
            }, Executors.newCachedThreadPool());
        }
        return httpResp;
    }


//#region HELPERS
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
