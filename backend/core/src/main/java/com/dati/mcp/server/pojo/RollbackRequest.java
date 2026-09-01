package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RollbackRequest {

    @NotNull
    @JsonProperty("target_version_number")
    private Integer targetVersionNumber;

    @JsonProperty("release_note")
    private String releaseNote;

}
