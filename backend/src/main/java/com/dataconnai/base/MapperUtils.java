package com.dataconnai.base;

import com.dataconnai.base.pojo.BaseResource;
import com.dataconnai.base.pojo.BaseResourcePO;

public class MapperUtils {
    
    public static void copyBaseInfo(BaseResource source, BaseResourcePO target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setCreatedBy(source.getCreatedBy());
        if (source.getCreatedAt() != null) {
            target.setCreatedAt(source.getCreatedAt());
        }
        target.setUpdatedBy(source.getUpdatedBy());
        if (source.getUpdatedAt() != null) {
            target.setUpdatedAt(source.getUpdatedAt());
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
