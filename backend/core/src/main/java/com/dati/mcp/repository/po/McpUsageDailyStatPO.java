package com.dati.mcp.repository.po;

import com.dati.base.pojo.BasePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Getter
@Setter
@FieldNameConstants
@Entity
@Table(
    name = "mcp_usage_daily_stat",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_mcp_stat_svc_tool_date", columnNames = {"service_id", "tool_name", "stat_date"})
    },
    indexes = {
        @Index(name = "idx_mcp_stat_svc_date", columnList = "service_id, stat_date")
    }
)
public class McpUsageDailyStatPO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", length = 64, nullable = false)
    private String serviceId;

    @Column(name = "tool_name", length = 64, nullable = false)
    private String toolName = "";

    @Column(name = "stat_date", length = 10, nullable = false)
    private String statDate;

    @Column(name = "total_calls", nullable = false)
    private Long totalCalls = 0L;

    @Column(name = "success_calls", nullable = false)
    private Long successCalls = 0L;

    @Column(name = "failed_calls", nullable = false)
    private Long failedCalls = 0L;

    @Column(name = "total_duration_ms", nullable = false)
    private Long totalDurationMs = 0L;

    @Column(name = "max_duration_ms", nullable = false)
    private Long maxDurationMs = 0L;
}
