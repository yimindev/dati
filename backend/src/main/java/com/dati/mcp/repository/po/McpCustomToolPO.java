package com.dati.mcp.repository.po;

import com.dati.base.pojo.BasePO;
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
@Table(name = "mcp_custom_tool", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"service_id", "name"})
})
public class McpCustomToolPO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false)
    private McpToolType toolType = McpToolType.PARAMETERIZED_SQL;

    @Column
    private String title;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String config;
}
