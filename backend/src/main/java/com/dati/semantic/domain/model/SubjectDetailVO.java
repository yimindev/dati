package com.dati.semantic.domain.model;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDetailVO {
    private Subject subject;
    private List<SubjectTable> tables;
}
