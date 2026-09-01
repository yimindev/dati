package com.dati.mcp.server.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataScopeResponse {

    private List<DataScopeItemVO> items;
    private List<DataSourceRefVO> resolvedDataSources = new ArrayList<>();

}
