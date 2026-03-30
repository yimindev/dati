package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SubjectDetailVO {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SubjectTableVO> tables;
}
