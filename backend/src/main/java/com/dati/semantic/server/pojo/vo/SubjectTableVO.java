package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubjectTableVO {
    private String id;
    private String subjectId;
    private String tableId;
    private LocalDateTime createdAt;
}
