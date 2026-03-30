package com.dati.semantic.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
