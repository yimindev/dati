package com.dati.mcp.server.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PublishRequest {

    @JsonProperty("release_note")
    private String releaseNote;

}
