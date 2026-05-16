package com.dati.mcp.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import com.dati.mcp.domain.model.McpServiceStatus;
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
@Table(name = "mcp_service")
public class McpServicePO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private McpServiceStatus status;

}
