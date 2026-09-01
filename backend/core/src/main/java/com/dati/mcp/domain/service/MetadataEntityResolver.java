package com.dati.mcp.domain.service;

import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves metadata entities (dsId+schema+table → tableId, +column → columnId)
 * for the metadata update tools. Schema matching follows GET_TABLE_INFO:
 * a null schema matches any table with that name in the data source.
 */
@Component
public class MetadataEntityResolver {

    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;

    public MetadataEntityResolver(TableInfoDAO tableInfoDAO, ColumnInfoDAO columnInfoDAO) {
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
    }

    public Optional<TableTarget> resolveTable(String dsId, String schema, String tableName) {
        return tableInfoDAO.findByDataSourceId(dsId).stream()
            .filter(t -> tableName.equals(t.getName())
                && (schema == null || schema.equals(t.getSchema())))
            .findFirst()
            .map(t -> new TableTarget(t.getId(), t.getDescription(), t.getAliases()));
    }

    public Optional<ColumnTarget> resolveColumn(String dsId, String schema, String table, String column) {
        return resolveTable(dsId, schema, table)
            .flatMap(tt -> columnInfoDAO.findByTableId(tt.tableId()).stream()
                .filter(c -> column.equals(c.getName()))
                .findFirst()
                .map(c -> new ColumnTarget(c.getId(), tt.tableId(),
                    c.getDescription(), c.getAliases())));
    }

    /** Resolved table: id plus current writeable metadata (for old-value capture). */
    public record TableTarget(String tableId, String description, List<String> aliases) {}

    /** Resolved column: id plus current writeable metadata (for old-value capture). */
    public record ColumnTarget(String columnId, String tableId, String description, List<String> aliases) {}
}
