package com.dati.mcp.repository.dao;

import com.dati.mcp.repository.po.McpUsageDailyStatPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface McpUsageDailyStatDAO extends JpaRepository<McpUsageDailyStatPO, String> {

    List<McpUsageDailyStatPO> findByServiceIdAndStatDateGreaterThanEqualOrderByStatDateAsc(String serviceId, String sinceDate);

    @Modifying
    @Transactional
    @Query("""
        UPDATE McpUsageDailyStatPO s
        SET s.totalCalls = s.totalCalls + :total,
            s.successCalls = s.successCalls + :success,
            s.failedCalls = s.failedCalls + :failed,
            s.totalDurationMs = s.totalDurationMs + :duration,
            s.maxDurationMs = CASE WHEN :maxDuration > s.maxDurationMs THEN :maxDuration ELSE s.maxDurationMs END
        WHERE s.serviceId = :serviceId AND s.toolName = :toolName AND s.statDate = :statDate
    """)
    int incrementStat(@Param("serviceId") String serviceId,
                      @Param("toolName") String toolName,
                      @Param("statDate") String statDate,
                      @Param("total") long total,
                      @Param("success") long success,
                      @Param("failed") long failed,
                      @Param("duration") long duration,
                      @Param("maxDuration") long maxDuration);

    @Modifying
    @Transactional
    @Query("DELETE FROM McpUsageDailyStatPO s WHERE s.statDate < :before")
    int deleteByStatDateBefore(@Param("before") String before);
}
