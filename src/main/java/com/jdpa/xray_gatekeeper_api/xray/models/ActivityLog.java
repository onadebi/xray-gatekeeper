package com.jdpa.xray_gatekeeper_api.xray.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "activityLog")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "operation", columnDefinition = "TEXT")
    private String operation;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public ActivityLog AddNew(String operation, String message) {
        ActivityLog newLog = new ActivityLog();
        newLog.setMessage(message);
        newLog.setOperation(operation);
        return newLog;
    }

}
