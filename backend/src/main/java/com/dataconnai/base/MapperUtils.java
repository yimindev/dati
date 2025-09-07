package com.dataconnai.base;

import com.dataconnai.base.pojo.BaseResource;
import com.dataconnai.base.pojo.BaseResourcePO;

import java.time.ZoneId;

public class MapperUtils {
    
    public static void copyBaseInfo(BaseResource source, BaseResourcePO target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setCreatedBy(source.getCreatedBy());
        if (source.getCreatedAt() != null) {
            target.setCreatedAt(source.getCreatedAt().toInstant());
        }
        target.setUpdatedBy(source.getUpdatedBy());
        if (source.getUpdatedAt() != null) {
            target.setUpdatedAt(source.getUpdatedAt().toInstant());
        }
    }
    
    public static void copyBaseInfo(BaseResourcePO source, BaseResource target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedAt(source.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
        target.setUpdatedBy(source.getUpdatedBy());
        target.setUpdatedAt(source.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
    
}
