package com.dati.mcp.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class McpServiceVO extends BaseResourceVO {

    private String status;

    private String endpointPath;

    private int toolCount;

}
