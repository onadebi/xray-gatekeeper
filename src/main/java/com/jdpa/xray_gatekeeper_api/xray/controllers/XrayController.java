package com.jdpa.xray_gatekeeper_api.xray.controllers;

import com.jdpa.xray_gatekeeper_api.helpers.Validators;
import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.XrayAppResponse;
import com.jdpa.xray_gatekeeper_api.xray.models.XrayAuth;
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

    @Autowired
    public XrayController(XRayService xrayService) {
        this._xrayService = xrayService;
    }

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

    @PostMapping("/junit/multipart")
    public Mono<ResponseEntity<AppResponse<XrayAppResponse>>> junit(@RequestParam("results") MultipartFile results,
                                                                    @RequestParam("info") MultipartFile info){
        String token= Validators.extractBearerToken();
        return _xrayService.PublishJunitToXray(results,info, token)
                .map(appResponse -> ResponseEntity
                        .status(appResponse.getStatCode())
                        .body(appResponse));
    }

    @PostMapping("/cucumber/multipart")
    public Mono<ResponseEntity<AppResponse<XrayAppResponse>>> cucumber(@RequestParam("results") MultipartFile results,
                                                                    @RequestParam("info") MultipartFile info){
        String token= Validators.extractBearerToken();
        return _xrayService.PublishCucumberToXray(results,info, token)
                .map(appResponse -> ResponseEntity
                        .status(appResponse.getStatCode())
                        .body(appResponse));
    }
}
