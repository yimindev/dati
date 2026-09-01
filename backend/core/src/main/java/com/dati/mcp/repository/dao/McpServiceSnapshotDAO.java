package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpServiceSnapshotPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface McpServiceSnapshotDAO extends JpaRepository<McpServiceSnapshotPO, String> {

    List<McpServiceSnapshotPO> findAllByServiceIdOrderByVersionNumberDesc(String serviceId);

    Optional<McpServiceSnapshotPO> findByServiceIdAndVersionNumber(String serviceId, Integer versionNumber);

    @Query("SELECT MAX(s.versionNumber) FROM McpServiceSnapshotPO s WHERE s.serviceId = :serviceId")
    Integer findMaxVersionNumberByServiceId(@Param("serviceId") String serviceId);

    void deleteAllByServiceId(String serviceId);
}
