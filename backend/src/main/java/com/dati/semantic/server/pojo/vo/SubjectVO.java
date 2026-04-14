package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SubjectVO {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private List<String> aliases = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
