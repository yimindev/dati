package com.dati.semantic.repository.po;

import com.dati.semantic.domain.SemanticEntityType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "semantic_search")
@Setting(shards = 3)
public class SemanticSearchDocument {
    
    @Id
    private String id;
    
    /**
     * 关键词列表
     * 表: 存储原始表名、表别名等
     * 字段: 存储字段名、字段别名等
     * 字段值: 存储具体的字段值
     * 术语: 存储术语名称
     * 主题: 存储主题名称
     * 同义词: 存储同义词列表
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private List<String> keywords;
    
    /**
     * 详细描述文本
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;
    
    /**
     * 文档类型
     */
    @Field(type = FieldType.Keyword)
    private SemanticEntityType type;
    
    /**
     * 关联的实体
     */
    @Field(type = FieldType.Nested)
    private EntityReference entity;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonProperty("createdTime")
    private LocalDateTime createdTime;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonProperty("updatedTime")
    private LocalDateTime updatedTime;

}
