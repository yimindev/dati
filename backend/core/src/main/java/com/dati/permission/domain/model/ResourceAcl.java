package com.dati.permission.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceAcl extends BaseResource {

    private ResourceType resourceType;
    private String resourceId;
    private PrincipalType principalType;
    private String principalId;
    private String principalName;
    private Permission permission;
}
