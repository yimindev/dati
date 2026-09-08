package com.dati.mcp.repository.po;

import com.dati.base.pojo.BasePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Getter
@Setter
@FieldNameConstants
@Entity
@Table(
    name = "mcp_invocation_log",
    indexes = {
        @Index(name = "idx_mcp_log_svc_created", columnList = "service_id, created_at"),
        @Index(name = "idx_mcp_log_created", columnList = "created_at")
    }
)
public class McpInvocationLogPO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", length = 64, nullable = false)
    private String serviceId;

    @Column(name = "method", length = 32, nullable = false)
    private String method;

    @Column(name = "tool_name", length = 64)
    private String toolName;

    @Column(name = "caller_id", length = 64)
    private String callerId;

    @Column(name = "caller_name", length = 64)
    private String callerName;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "stat_date", length = 10, nullable = false)
    private String statDate;
}
