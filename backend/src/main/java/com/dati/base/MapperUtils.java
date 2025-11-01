package com.dati.base;

import com.dati.base.pojo.BaseResource;
import com.dati.base.pojo.BaseResourcePO;

public class MapperUtils {
    
    public static void copyBaseInfo(BaseResource resource, BaseResourcePO resourcePO) {
        if (resource.getId() != null) {
            resourcePO.setId(resource.getId());
        }
        if (resource.getName() != null) {
            resourcePO.setName(resource.getName());
        }
        if (resource.getDescription() != null) {
            resourcePO.setDescription(resource.getDescription());
        }
        if (resource.getCreatedBy() != null) {
            resourcePO.setCreatedBy(resource.getCreatedBy());
        }
        if (resource.getCreatedAt() != null) {
            resourcePO.setCreatedAt(resource.getCreatedAt());
        }
        if (resource.getUpdatedBy() != null) {
            resourcePO.setUpdatedBy(resource.getUpdatedBy());
        }
        if (resource.getUpdatedAt() != null) {
            resourcePO.setUpdatedAt(resource.getUpdatedAt());
        }
    }
    
    public static void copyBaseInfo(BaseResourcePO resourcePO, BaseResource resource) {
        resource.setId(resourcePO.getId());
        resource.setName(resourcePO.getName());
        resource.setDescription(resourcePO.getDescription());
        resource.setCreatedBy(resourcePO.getCreatedBy());
        resource.setCreatedAt(resourcePO.getCreatedAt());
        resource.setUpdatedBy(resourcePO.getUpdatedBy());
        resource.setUpdatedAt(resourcePO.getUpdatedAt());
    }
    
}
