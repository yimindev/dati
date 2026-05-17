package com.dati.mcp.server.pojo;

import lombok.Data;

import java.util.List;

@Data
public class DataScopeRequest {

    private List<DataScopeItemVO> items;

}
