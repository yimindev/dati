package com.dati.mcp.repository.po;

import com.dati.base.pojo.BasePO;
import com.dati.mcp.domain.model.McpDataScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "mcp_service_data_scope")
public class McpServiceDataScopePO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 64)
    private String serviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private McpDataScopeType scopeType;

    @Column(nullable = false)
    private String referenceId;


}
