package com.dati.permission.server.pojo;

import lombok.Data;

@Data
public class AclEntryVO {

    private String id;
    private String principalType;
    private String principalId;
    private String principalName;
    private String permission;
    private String createdBy;
}
