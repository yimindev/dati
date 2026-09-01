package com.dati.base;

import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.BaseResource;
import com.dati.base.pojo.BaseResourcePO;

public class MapperUtils {
    
    public static void copyBaseResourceInfo(BaseResource resource, BaseResourcePO resourcePO) {
        if (resource.getName() != null) {
            resourcePO.setName(resource.getName());
        }
        if (resource.getDescription() != null) {
            resourcePO.setDescription(resource.getDescription());
        }
        copyBaseInfo(resource, resourcePO);
    }

    public static void copyBaseInfo(BaseResource resource, BasePO basePO) {
        if (resource.getId() != null) {
            basePO.setId(resource.getId());
        }
        if (resource.getCreatedBy() != null) {
            basePO.setCreatedBy(resource.getCreatedBy());
        }
        if (resource.getCreatedAt() != null) {
            basePO.setCreatedAt(resource.getCreatedAt());
        }
        if (resource.getUpdatedBy() != null) {
            basePO.setUpdatedBy(resource.getUpdatedBy());
        }
        if (resource.getUpdatedAt() != null) {
            basePO.setUpdatedAt(resource.getUpdatedAt());
        }
    }
    
    public static void copyBaseResourceInfo(BaseResourcePO resourcePO, BaseResource resource) {
        resource.setName(resourcePO.getName());
        resource.setDescription(resourcePO.getDescription());
        copyBaseInfo(resourcePO, resource);
    }

    public static void copyBaseInfo(BasePO basePO, BaseResource resource) {
        resource.setId(basePO.getId());
        resource.setCreatedBy(basePO.getCreatedBy());
        resource.setCreatedAt(basePO.getCreatedAt());
        resource.setUpdatedBy(basePO.getUpdatedBy());
        resource.setUpdatedAt(basePO.getUpdatedAt());
    }
    
}
