package com.dati.mcp.server.pojo;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class DataScopeRequest {

    @Valid
    private List<DataScopeItemVO> items;

}
