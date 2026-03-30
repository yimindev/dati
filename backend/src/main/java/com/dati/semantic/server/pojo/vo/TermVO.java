package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.Instant;

@Data
public class TermVO {
    private String id;
    private String subjectId;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
