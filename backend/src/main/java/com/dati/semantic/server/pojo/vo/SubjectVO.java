package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubjectVO {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
