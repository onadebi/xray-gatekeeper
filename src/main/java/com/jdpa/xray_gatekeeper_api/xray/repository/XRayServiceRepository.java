package com.jdpa.xray_gatekeeper_api.xray.repository;

import com.jdpa.xray_gatekeeper_api.xray.dtos.RequestStatusDTO;
import com.jdpa.xray_gatekeeper_api.xray.models.XRayRequestLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface XRayServiceRepository extends JpaRepository<XRayRequestLogs, Long> {

    @Query("SELECT new com.jdpa.xray_gatekeeper_api.xray.dtos.RequestStatusDTO(COUNT(rl.id), rl.status) " +
            "FROM XRayRequestLogs rl " +
            "WHERE rl.updatedAt BETWEEN :from AND :to " +
            "GROUP BY rl.status")
    List<RequestStatusDTO> getXRayRequestsStatusByDateRange(LocalDateTime from, LocalDateTime to);

    @Query("SELECT new com.jdpa.xray_gatekeeper_api.xray.dtos.RequestStatusDTO(COUNT(rl.id), rl.operation) " +
            "FROM XRayRequestLogs rl " +
            "WHERE rl.updatedAt BETWEEN :from AND :to " +
            "GROUP BY rl.operation")
    List<RequestStatusDTO> getXRayRequestsOperationsByDateRange(LocalDateTime from, LocalDateTime to);
}
