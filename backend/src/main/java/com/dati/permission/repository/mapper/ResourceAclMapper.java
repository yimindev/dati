package com.dati.permission.repository.mapper;

import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceAcl;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.po.ResourceAclPO;

public final class ResourceAclMapper {

    private ResourceAclMapper() {}

    public static ResourceAclPO toPO(ResourceAcl acl) {
        ResourceAclPO po = new ResourceAclPO();
        po.setId(acl.getId());
        po.setResourceType(acl.getResourceType().name());
        po.setResourceId(acl.getResourceId());
        po.setPrincipalType(acl.getPrincipalType().name());
        po.setPrincipalId(acl.getPrincipalId());
        po.setPermission(acl.getPermission());
        return po;
    }

    public static ResourceAcl toModel(ResourceAclPO po) {
        ResourceAcl acl = new ResourceAcl();
        acl.setId(po.getId());
        acl.setResourceType(ResourceType.valueOf(po.getResourceType()));
        acl.setResourceId(po.getResourceId());
        acl.setPrincipalType(PrincipalType.valueOf(po.getPrincipalType()));
        acl.setPrincipalId(po.getPrincipalId());
        acl.setPermission(po.getPermission());
        return acl;
    }
}
