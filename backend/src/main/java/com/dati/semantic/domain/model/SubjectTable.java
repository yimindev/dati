package com.dati.semantic.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTable {
    private String id;
    private String subjectId;
    private String tableId;
    private String tableName;
    private String displayName;
    private LocalDateTime createdAt;
}
