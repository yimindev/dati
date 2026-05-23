package com.dati.mcp.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import com.dati.mcp.domain.model.McpToolType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "mcp_prebuilt_tool_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"service_id", "tool_type"})
})
public class McpPrebuiltToolConfigPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false, length = 30)
    private McpToolType toolType;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String config;
}
