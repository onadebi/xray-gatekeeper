package com.jdpa.xray_gatekeeper_api.xray.repository;

import com.jdpa.xray_gatekeeper_api.xray.models.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository  extends JpaRepository<ActivityLog, Long> {
}
