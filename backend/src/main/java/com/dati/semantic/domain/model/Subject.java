package com.dati.semantic.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private List<String> aliases = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
