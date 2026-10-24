package com.dati.datasource.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Result of a batch table import: how many tables were added and their generated ids. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchAddTablesResponse {
    private int addedCount;
    private List<String> tableIds;
}
