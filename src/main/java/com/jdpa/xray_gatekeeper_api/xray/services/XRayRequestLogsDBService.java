package com.jdpa.xray_gatekeeper_api.xray.services;

import com.jdpa.xray_gatekeeper_api.xray.dtos.OperationEnum;
import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs;
import com.jdpa.xray_gatekeeper_api.xray.repository.XRayServiceRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class XRayRequesLogsDBService {

    private final XRayServiceRepository _xrayServiceRepository;

    public XRayRequesLogsDBService(XRayServiceRepository _xrayServiceRepository) {
        this._xrayServiceRepository = _xrayServiceRepository;
    }

    @Async
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

}
