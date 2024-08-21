package com.jdpa.xray_gatekeeper_api.xray.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "requestLogs")
public class XRayRequestLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_names", columnDefinition = "TEXT")
    private String fileNames;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "retries_count")
    private Integer retriesCount;

    @Column(name = "operation", columnDefinition = "TEXT")
    private String operation;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "error", columnDefinition = "TEXT", nullable = true)
    private String error;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING,
        CANCELED,
        COMPLETED,
        COMPLETED_WITH_ERROR,
        ERROR
    }

    public XRayRequestLogs AddNew(String filePath, String fileNames, Status status,String operation, String data) {
        XRayRequestLogs newLog = new XRayRequestLogs();
        newLog.setFilePath(filePath);
        newLog.setFileNames(fileNames);
        newLog.setStatus(status);
        newLog.setOperation(operation);
        newLog.setData(data);
        return newLog;
    }
}
