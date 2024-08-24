package com.jdpa.xray_gatekeeper_api.xray.repository;

import com.jdpa.xray_gatekeeper_api.xray.models.ApplicationStart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStartRepository extends JpaRepository<ApplicationStart, Long> {
}
