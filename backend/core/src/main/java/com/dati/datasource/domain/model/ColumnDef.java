package com.dati.datasource.domain.model;

import java.util.List;

public record ColumnDef(String name, String type, String comment,
                        List<String> aliases, List<String> sampleValues) {}
