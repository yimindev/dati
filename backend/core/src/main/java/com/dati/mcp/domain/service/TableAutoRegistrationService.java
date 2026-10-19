package com.dati.mcp.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.ColumnMapper;
import com.dati.datasource.repository.mapper.TableMapper;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.Column;
import com.dati.db.Table;
import com.dati.db.analysis.TableRef;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Persists metadata for tables created through schema-scoped SQL. */
@Slf4j
@Service
public class TableAutoRegistrationService {

    private static final int DESCRIPTION_MAX_LENGTH = 1024;

    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;
    private final JdbcMetaService jdbcMetaService;
    private final SemanticIndexService semanticIndexService;

    public TableAutoRegistrationService(TableInfoDAO tableInfoDAO,
                                        ColumnInfoDAO columnInfoDAO,
                                        JdbcMetaService jdbcMetaService,
                                        SemanticIndexService semanticIndexService) {
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
    }

    @Transactional
    public List<String> registerCreatedTables(String dsId,
                                              Set<TableRef> createdTables,
                                              @Nullable String defaultSchema,
                                              @Nullable String serviceId) {
        if (createdTables == null || createdTables.isEmpty()) {
            return List.of();
        }

        String actor = currentActor(serviceId);
        List<String> registeredTableIds = new ArrayList<>();
        List<SemanticSearchDocument> docs = new ArrayList<>();

        for (TableRef ref : createdTables) {
            String schema = McpDataScopeSchemas.effectiveSchema(ref, defaultSchema);
            if (schema == null) {
                log.warn("Skip auto-registration for {}: cannot resolve schema", ref.qualifiedName());
                continue;
            }

            Table physicalTable = findPhysicalTable(dsId, schema, ref.name());
            if (physicalTable == null) {
                log.warn("Skip auto-registration for {}.{}: physical table not found",
                        schema, ref.name());
                continue;
            }

            if (tableInfoDAO.findFirstByDataSourceIdAndSchemaAndNameIgnoreCase(
                    dsId, schema, physicalTable.name()).isPresent()) {
                continue;
            }

            List<Column> columns = readColumns(dsId, schema, physicalTable.name());
            if (columns == null || columns.isEmpty()) {
                log.warn("Skip auto-registration for {}.{}: no column metadata",
                        schema, physicalTable.name());
                continue;
            }

            registeredTableIds.add(saveTable(dsId, schema, physicalTable, actor, columns, docs));
        }

        saveIndexDocs(dsId, docs);
        return registeredTableIds;
    }

    @Nullable
    private Table findPhysicalTable(String dsId, String schema, String tableName) {
        try {
            List<Table> tables = jdbcMetaService.getTables(dsId, schema);
            for (Table table : tables) {
                if (table.name().equals(tableName)) {
                    return table;
                }
            }
            for (Table table : tables) {
                if (table.name().equalsIgnoreCase(tableName)) {
                    return table;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to read physical tables for data source {} schema {}: {}",
                    dsId, schema, e.getMessage());
            return null;
        }
    }

    @Nullable
    private List<Column> readColumns(String dsId, String schema, String tableName) {
        try {
            return jdbcMetaService.getColumns(dsId, schema, tableName);
        } catch (Exception e) {
            log.warn("Failed to read columns for {}.{}: {}", schema, tableName, e.getMessage());
            return null;
        }
    }

    private String saveTable(String dsId, String schema, Table physicalTable, String actor,
                             List<Column> columns, List<SemanticSearchDocument> docs) {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setDatasourceId(dsId);
        tableInfo.setSchema(schema);
        tableInfo.setName(physicalTable.name());
        tableInfo.setDescription(truncate(physicalTable.comment()));
        tableInfo.setAliases(new ArrayList<>());
        tableInfo.setCreatedBy(actor);
        tableInfo.setUpdatedBy(actor);

        TableInfoPO savedTable = tableInfoDAO.save(TableMapper.toTableInfoPO(tableInfo));
        String tableId = savedTable.getId();

        docs.add(SemanticSearchDocument.builder()
                .id("table:" + tableId)
                .type(SemanticEntityType.TABLE)
                .keywords(List.of(tableInfo.getName()))
                .description(tableInfo.getDescription())
                .entity(EntityReference.builder()
                        .datasourceId(dsId)
                        .tableId(tableId)
                        .tableName(tableInfo.getName())
                        .build())
                .build());

        for (Column column : columns) {
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setTableId(tableId);
            columnInfo.setName(column.name());
            columnInfo.setColumnType(column.type());
            columnInfo.setDescription(truncate(column.comment()));
            columnInfo.setAliases(new ArrayList<>());
            columnInfo.setCreatedBy(actor);
            columnInfo.setUpdatedBy(actor);

            ColumnInfoPO savedColumn = columnInfoDAO.save(ColumnMapper.toColumnInfoPO(columnInfo));
            docs.add(SemanticSearchDocument.builder()
                    .id("field:" + savedColumn.getId())
                    .type(SemanticEntityType.FIELD)
                    .keywords(List.of(columnInfo.getName()))
                    .description(columnInfo.getDescription())
                    .entity(EntityReference.builder()
                            .datasourceId(dsId)
                            .tableId(tableId)
                            .tableName(tableInfo.getName())
                            .field(columnInfo.getName())
                            .build())
                    .build());
        }

        return tableId;
    }

    private void saveIndexDocs(String dsId, List<SemanticSearchDocument> docs) {
        if (docs.isEmpty()) {
            return;
        }
        try {
            semanticIndexService.saveBatch(docs);
        } catch (Exception e) {
            log.warn("Failed to index auto-registered tables for data source {}: {}",
                    dsId, e.getMessage());
        }
    }

    private String currentActor(@Nullable String serviceId) {
        User user = RequestContext.getUser();
        if (user != null && user.getId() != null && !user.getId().isBlank()) {
            return user.getId();
        }
        return serviceId != null && !serviceId.isBlank() ? "mcp:" + serviceId : "mcp";
    }

    @Nullable
    private String truncate(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= DESCRIPTION_MAX_LENGTH
                ? value
                : value.substring(0, DESCRIPTION_MAX_LENGTH);
    }
}
