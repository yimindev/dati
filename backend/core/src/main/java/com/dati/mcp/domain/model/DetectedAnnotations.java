package com.dati.mcp.domain.model;

public record DetectedAnnotations(
    Boolean readOnly,
    Boolean idempotent,
    Boolean destructive,
    String detectedOperation
) {}
