package com.dati.mcp.server.pojo;

import com.dati.base.pojo.BaseResourceVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class McpServiceSnapshotVO extends BaseResourceVO {

    private String serviceId;

    private Integer versionNumber;

    private String releaseNote;

}
