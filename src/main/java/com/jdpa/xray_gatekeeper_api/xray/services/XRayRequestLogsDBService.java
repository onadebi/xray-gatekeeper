package com.jdpa.xray_gatekeeper_api.xray.services;

import com.jdpa.xray_gatekeeper_api.xray.dtos.AppResponse;
import com.jdpa.xray_gatekeeper_api.xray.dtos.OperationEnum;
import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs;
import com.jdpa.xray_gatekeeper_api.xray.repository.XRayServiceRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class XRayRequestLogsDBService {

    private final XRayServiceRepository _xrayServiceRepository;

    public XRayRequestLogsDBService(XRayServiceRepository _xrayServiceRepository) {
        this._xrayServiceRepository = _xrayServiceRepository;
    }

//    @Async
    protected long SaveRequestLog(String filePath, String fileNames, XRayRequestLogs.Status status, OperationEnum operation, String data){
        XRayRequestLogs newObj = new XRayRequestLogs().AddNew(filePath, fileNames, status, operation.name(),data);
        _xrayServiceRepository.saveAndFlush(newObj);
        System.out.printf("Operation: %s | ID: %s%n", operation, newObj.getId());
        return  newObj.getId();
    }

    @Async
    protected void UpdateRequestLog(long id, String response, XRayRequestLogs.Status status, String error){
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

    public AppResponse<String> UpdateRequestStatus(long id, XRayRequestLogs.Status status){
        AppResponse<String> objResp = new AppResponse<String>("", null,304, false);
        try {
            Optional<XRayRequestLogs> optionalLog = _xrayServiceRepository.findById(id);
            if (optionalLog.isPresent()) {
                XRayRequestLogs log = optionalLog.get();
                if(log.getStatus().equals(status)){
                    return new AppResponse<String>(String.format("Status is already: %s.", status.name()), null, 304, true);
                }
                if(log.getStatus().equals(XRayRequestLogs.Status.COMPLETED)){
                    return new AppResponse<String>("Cannot Cancel a Completed request. Operation not allowed.", null, 304, false);
                }
                log.setStatus(status);
                _xrayServiceRepository.save(log);
                objResp.setSuccess(true);
                objResp.setStatCode(200);
                objResp.setResult(String.format("Record with id %s Updated successfully.",id));
            }else{
                objResp.setStatCode(404);
                objResp.setResult(String.format("No Record found for id: [%s]",id));
            }
        }catch(Exception ex){
            System.out.println("Update request log failed. Error: " + ex.getMessage());
            objResp.setStatCode(501);
            objResp.setResult(String.format("Failed to update record id %s. Error: %s", id, ex.getMessage()));
        }
        return objResp;
    }

    public AppResponse<XRayRequestLogs> GetRequestLogBy(long id){
        AppResponse<XRayRequestLogs> objResp = new AppResponse<>();
        try{
            Optional<XRayRequestLogs> optionalLog = _xrayServiceRepository.findById(id);
            if (optionalLog.isPresent()) {
                XRayRequestLogs log = optionalLog.get();
                objResp.setSuccess(true);
                objResp.setStatCode(200);
                objResp.setResult(log);
            }else{
                objResp.setResult(null);
                objResp.setSuccess(false);
                objResp.setStatCode(404);
            }
        }catch(Exception ex){
            objResp.setResult(null);
            objResp.setSuccess(false);
            objResp.setStatCode(404);
            System.out.printf("Get request failed for ID: %s. Error: %s%n", id, ex.getMessage());
        }
        return objResp;
    }


}
