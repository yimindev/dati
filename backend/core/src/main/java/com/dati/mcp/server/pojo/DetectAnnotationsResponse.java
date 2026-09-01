package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectAnnotationsResponse {
    @JsonProperty("read_only")
    private Boolean readOnly;

    @JsonProperty("idempotent")
    private Boolean idempotent;

    @JsonProperty("destructive")
    private Boolean destructive;

    @JsonProperty("detected_operation")
    private String detectedOperation;
}
