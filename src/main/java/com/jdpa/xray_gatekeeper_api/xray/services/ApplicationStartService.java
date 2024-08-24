package com.jdpa.xray_gatekeeper_api.xray.services;

import com.jdpa.xray_gatekeeper_api.xray.models.ApplicationStart;
import com.jdpa.xray_gatekeeper_api.xray.repository.ApplicationStartRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApplicationStartService {

    @Autowired
    private ApplicationStartRepository _applicationStatrRepository;

    /***
     * Log to database everytime the application newly starts up
     */
    @PostConstruct
    public void initialize() {
            ApplicationStart start = new ApplicationStart();
            start.setStartTime(LocalDateTime.now());
            _applicationStatrRepository.save(start);
    }
}
