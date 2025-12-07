package com.dati.datasource.repository.converter;

import com.dati.common.JsonUtils;
import com.dati.common.StringUtils;
import com.dati.datasource.domain.model.ColumnDef;
import jakarta.persistence.AttributeConverter;

import java.util.List;

public class ColumnListConverter implements AttributeConverter<List<ColumnDef>, String> {
    @Override
    public String convertToDatabaseColumn(List<ColumnDef> columnDefs) {
        if (columnDefs == null || columnDefs.isEmpty()) {
            return "[]";
        }
        return JsonUtils.toJson(columnDefs);
    }

    @Override
    public List<ColumnDef> convertToEntityAttribute(String s) {
        if (StringUtils.isBlank(s)) {
            return List.of();
        }
        return JsonUtils.toList(s, ColumnDef.class);
    }
}
