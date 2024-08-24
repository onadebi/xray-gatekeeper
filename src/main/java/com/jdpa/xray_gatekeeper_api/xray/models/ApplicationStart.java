package com.jdpa.xray_gatekeeper_api.xray.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "applicationStart")
public class ApplicationStart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private LocalDateTime startTime;
    private String hostName;

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
        hostName = this.getHostName();
    }

    private String getHostName() {
        String hostName = System.getenv("HOSTNAME") != null ? System.getenv("HOSTNAME") : "localhost";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostName = localHost.getHostName();
        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException - Hostname could not be determined: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error - Hostname could not be determined: " + e.getMessage());
        }
        return hostName;
    }
}
