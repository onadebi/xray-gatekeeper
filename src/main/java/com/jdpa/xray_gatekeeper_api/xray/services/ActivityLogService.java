package com.jdpa.xray_gatekeeper_api.xray.services;


import com.jdpa.xray_gatekeeper_api.xray.models.ActivityLog;
import com.jdpa.xray_gatekeeper_api.xray.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository _activityLogRepository;

    @Autowired
    public ActivityLogService(ActivityLogRepository _activityLogRepository) {
        this._activityLogRepository = _activityLogRepository;
    }


    @Async
    public void save(String operation, String message) {
        try {
            CompletableFuture.runAsync(() -> {
                ActivityLog activityLog = new ActivityLog().AddNew(operation, message);
                _activityLogRepository.saveAndFlush(activityLog);
            }, Executors.newCachedThreadPool());
        }catch (Exception e) {
            System.err.println("[ActivityLogService] Save Error: "+e.getMessage());
        }
    }


}
