package com.jdpa.xray_gatekeeper_api.xray.controllers;

import com.jdpa.xray_gatekeeper_api.helpers.RateLimitProtection;
import com.jdpa.xray_gatekeeper_api.helpers.Validators;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs;
import com.jdpa.xray_gatekeeper_api.xray.models.XrayAuth;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayRequestLogsDBService;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path="/api/v2/xray")
public class XrayController {

    private final XRayService _xrayService;
    private final XRayRequestLogsDBService _xrayRequestLogsDBService;

    @Autowired
    public XrayController(XRayService xrayService, XRayRequestLogsDBService xrayRequestLogsDBService) {
        this._xrayService = xrayService;
        this._xrayRequestLogsDBService = xrayRequestLogsDBService;
    }

    @RateLimitProtection
    @GetMapping(value = {"/","", "/health", "/status"})
    public AppResponse<String> HealthCheck(){
        return AppResponse.success("XRay API is up and running", 200);
    }

    @PostMapping("/authenticate")
    public Mono<ResponseEntity<AppResponse<String>>> Auth(@RequestBody XrayAuth request){
        return _xrayService.AuthenticateXRay(request)
                .map(appResponse -> ResponseEntity
                        .status(appResponse.getStatCode())
                        .body(appResponse));
    }

    @RateLimitProtection(rateLimit = 10, rateDuration = 180000)
    @PostMapping("/junit/multipart")
    public Mono<ResponseEntity<AppResponse<String>>> junit(@RequestParam("results") MultipartFile results,
                                                                    @RequestParam("info") MultipartFile info){
        String token= Validators.extractBearerToken();
        return _xrayService.PublishJunitToXray(results,info, token)
                .map(appResponse -> ResponseEntity
                        .status(appResponse.getStatCode())
                        .body(appResponse));
    }

    @PostMapping("/cucumber/multipart")
    public Mono<ResponseEntity<AppResponse<String>>> cucumber(@RequestParam("results") MultipartFile results,
                                                                    @RequestParam("info") MultipartFile info){
        String token= Validators.extractBearerToken();
        return _xrayService.PublishCucumberToXray(results,info, token)
                .map(appResponse -> ResponseEntity
                        .status(appResponse.getStatCode())
                        .body(appResponse));
    }

    @PostMapping("/feature")
    public Mono<ResponseEntity<AppResponse<String>>> projectKey(@RequestParam("file") MultipartFile file,
                                                                                 @RequestParam("projectKey") String projectKey){
        String token= Validators.extractBearerToken();
        return  _xrayService.PublishFeatureFileToXray(file,projectKey, token)
                .map(appResponse -> ResponseEntity
                        .status(appResponse.getStatCode())
                        .body(appResponse));
    }

    @DeleteMapping("/cancel/{requestId}")
    public ResponseEntity<AppResponse<String>> CancelRequest(@PathVariable long requestId){
        AppResponse<String> objResp = _xrayRequestLogsDBService.UpdateRequestStatus(requestId, XRayRequestLogs.Status.CANCELED);
        return ResponseEntity.status(objResp.getStatCode()).body(objResp);
    }
}
