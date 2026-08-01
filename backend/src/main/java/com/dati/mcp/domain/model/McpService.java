package com.dati.mcp.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class McpService extends BaseResource {

    private String code;

    private McpServiceStatus status;

    private String activeVersionId;

    private Integer activeVersionNumber;

}
