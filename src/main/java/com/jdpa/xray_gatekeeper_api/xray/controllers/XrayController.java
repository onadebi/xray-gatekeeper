package com.jdpa.xray_gatekeeper_api.xray.controllers;

import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.models.XrayAuth;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
