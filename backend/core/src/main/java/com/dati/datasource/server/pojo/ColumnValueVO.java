package com.dati.datasource.server.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ColumnValueVO {

    private String id;

    private String value;

    private List<String> synonyms = new ArrayList<>();
}
