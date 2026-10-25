package com.dati.mcp.server.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DataScopeRequest {

    /** Full replacement of the service data scope; an empty list clears it. */
    @NotNull
    @Valid
    private List<@NotNull DataScopeItemVO> items;

}
