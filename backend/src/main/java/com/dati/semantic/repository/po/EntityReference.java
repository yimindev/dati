package com.dati.semantic.repository.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityReference {

    @Field(type = FieldType.Keyword)
    private String subjectId;

    @Field(type = FieldType.Keyword)
    private String tableId;

    @Field(type = FieldType.Keyword)
    private String tableName;

    /**
     * 字段名（如果是字段级别的引用）
     */
    @Field(type = FieldType.Keyword)
    private String field;
}
