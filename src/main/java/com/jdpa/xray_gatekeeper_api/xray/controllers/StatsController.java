package com.jdpa.xray_gatekeeper_api.xray.controllers;


import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.RequestStatusDTO;
import com.jdpa.xray_gatekeeper_api.xray.dtos.UserRequestStatusDTO;
import com.jdpa.xray_gatekeeper_api.xray.dtos.reports.ReportsCategoryEnum;
import com.jdpa.xray_gatekeeper_api.xray.services.XRayRequestLogsDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/api/v2/xray/stats")
public class StatsController {

    private final XRayRequestLogsDBService _xrayRequestLogsDBService;

    @Autowired
    public StatsController(XRayRequestLogsDBService _xrayRequestLogsDBService) {
        this._xrayRequestLogsDBService = _xrayRequestLogsDBService;
    }

    @GetMapping(value = {"/","", "/health", "/status"})
    public AppResponse<String> HealthCheck(){
        return AppResponse.success("XRayAPI STATS is up and running", 200);
    }

    @PostMapping("/requests/status")
    public ResponseEntity<AppResponse<List<RequestStatusDTO>>> RequestsStatusLog(@RequestBody UserRequestStatusDTO request){
        AppResponse<List<RequestStatusDTO>> objResp = _xrayRequestLogsDBService.GetOverallReportsPublished(request, ReportsCategoryEnum.ByStatus);
        return ResponseEntity.status(objResp.getStatCode()).body(objResp);
    }

    @PostMapping("/requests/operations")
    public ResponseEntity<AppResponse<List<RequestStatusDTO>>> RequestsOperationsLog(@RequestBody UserRequestStatusDTO request){
        AppResponse<List<RequestStatusDTO>> objResp = _xrayRequestLogsDBService.GetOverallReportsPublished(request, ReportsCategoryEnum.ByOperation);
        return ResponseEntity.status(objResp.getStatCode()).body(objResp);
    }
}
