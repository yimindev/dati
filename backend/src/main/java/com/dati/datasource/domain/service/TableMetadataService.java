package com.dati.datasource.domain.service;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;

/**
 * Queries platform-managed table metadata: columns, aliases, and sample dimension values.
 */
@Service
public class TableMetadataService {

    private static final int SAMPLE_VALUE_LIMIT = 5;
    private static final int VALUE_QUERY_MAX = 5000;

    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;
    private final SemanticIndexService semanticIndexService;

    public TableMetadataService(TableInfoDAO tableInfoDAO,
                                ColumnInfoDAO columnInfoDAO,
                                SemanticIndexService semanticIndexService) {
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
        this.semanticIndexService = semanticIndexService;
    }

    /**
     * Returns column metadata for a platform-managed table, or empty if the table is not registered.
     */
    public Optional<TableMeta> getTableMeta(String dsId, String schema, String tableName) {
        List<TableInfoPO> tables = tableInfoDAO.findByDataSourceId(dsId).stream()
                .filter(t -> matches(t, schema, tableName))
                .toList();
        if (tables.isEmpty()) return Optional.empty();

        TableInfoPO table = tables.getFirst();
        List<ColumnInfoPO> columns = columnInfoDAO.findByTableId(table.getId());

        List<SemanticSearchDocument> allValues =
                semanticIndexService.findByTableIdAndType(table.getId(),
                        SemanticEntityType.FIELD_VALUE, VALUE_QUERY_MAX);

        Map<String, List<String>> valuesByField = allValues.stream()
                .filter(doc -> doc.getKeywords() != null && !doc.getKeywords().isEmpty())
                .collect(groupingBy(
                        doc -> doc.getEntity().getField(),
                        java.util.stream.Collectors.mapping(
                                doc -> doc.getKeywords().getFirst(),
                                java.util.stream.Collectors.collectingAndThen(
                                        java.util.stream.Collectors.toList(),
                                        list -> list.size() <= SAMPLE_VALUE_LIMIT ? list
                                                : list.subList(0, SAMPLE_VALUE_LIMIT)))));

        List<ColumnDef> columnDefs = columns.stream()
                .map(c -> new ColumnDef(
                        c.getName(),
                        c.getColumnType(),
                        c.getDescription(),
                        c.getAliases(),
                        valuesByField.getOrDefault(c.getName(), List.of())))
                .toList();

        return Optional.of(new TableMeta(table.getSchema(), table.getName(),
                table.getDescription(), table.getAliases(), columnDefs));
    }

    private boolean matches(TableInfoPO table, String schema, String tableName) {
        return tableName.equals(table.getName())
                && (schema == null || schema.equals(table.getSchema()));
    }

    public record TableMeta(String schema, String tableName, String description,
                             List<String> aliases, List<ColumnDef> columns) {}
}
