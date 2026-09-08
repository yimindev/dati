package com.dati.mcp.server.pojo;

import lombok.Data;

import java.time.Instant;

@Data
public class McpInvocationLogVO {

    private String id;

    private String serviceId;

    private String method;

    private String toolName;

    private String callerId;

    private String callerName;

    private String clientIp;

    private Boolean success;

    private Long durationMs;

    private String errorMessage;

    private String statDate;

    private Instant createdAt;
}
