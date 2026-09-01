package com.dati.datasource.domain.model;

import java.util.List;

public record DataSourceDef(String id, String name, String dbType,
                             String defaultSchema, String description,
                             List<TableDef> tables) {}
