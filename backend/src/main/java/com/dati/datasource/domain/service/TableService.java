package com.dati.datasource.domain.service;

import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.BaseResourcePO;
import com.dati.base.pojo.PageReq;
import com.dati.common.StringUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.ColumnMapper;
import com.dati.datasource.repository.mapper.TableMapper;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.datasource.server.pojo.AddTableRequest;
import com.dati.db.Column;
import com.dati.db.Table;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TableService {

    private final TableInfoDAO tableInfoDAO;
    private final ColumnInfoDAO columnInfoDAO;
    private final JdbcMetaService jdbcMetaService;
    private final SemanticIndexService semanticIndexService;

    public TableService(TableInfoDAO tableInfoDAO, ColumnInfoDAO columnInfoDAO, JdbcMetaService jdbcMetaService, SemanticIndexService semanticIndexService) {
        this.tableInfoDAO = tableInfoDAO;
        this.columnInfoDAO = columnInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
    }

    public Page<TableInfo> getTables(PageReq pageReq, String datasourceId, String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, BasePO.Fields.createdAt);
        if (StringUtils.isEmpty(keyword)) {
            return tableInfoDAO.findByDataSourceId(datasourceId, pageReq.toPageRequest().withSort(sortBy)).map(TableMapper::toTableInfo);
        }
        return tableInfoDAO.findByDataSourceIdAndNameContaining(datasourceId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(TableMapper::toTableInfo);
    }

    public List<String> getAddedTableNames(String datasourceId) {
        return tableInfoDAO.findByDataSourceId(datasourceId).stream()
                .map(BaseResourcePO::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> batchAddTables(String datasourceId, List<AddTableRequest> tables) {
        if (tables.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tableIds = new ArrayList<>();
        List<SemanticSearchDocument> docsToSave = new ArrayList<>();

        Map<String, String> tableCommentMap;
        try {
            List<Table> dbTables = jdbcMetaService.getTables(datasourceId, null, tables.getFirst().getSchema());
            tableCommentMap = dbTables.stream()
                    .collect(Collectors.toMap(Table::name, t -> t.comment() != null ? t.comment() : ""));
        } catch (SQLException e) {
            throw new DatiException(ErrorCode.DS_SYNC_FAILED, e.getMessage());
        }

        for (AddTableRequest request : tables) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setDatasourceId(datasourceId);
            tableInfo.setName(request.getName());
            tableInfo.setSchema(request.getSchema());
            tableInfo.setDescription(tableCommentMap.getOrDefault(request.getName(), null));
            tableInfo.setAliases(new ArrayList<>());
            tableInfo.setCreatedBy(RequestContext.getUser().getId());
            tableInfo.setUpdatedBy(RequestContext.getUser().getId());

            TableInfoPO savedTablePO = tableInfoDAO.save(TableMapper.toTableInfoPO(tableInfo));
            String tableId = savedTablePO.getId();
            tableIds.add(tableId);

            EntityReference tableEntity = EntityReference.builder()
                    .tableId(tableId)
                    .tableName(tableInfo.getName())
                    .build();
            List<String> tableKeywords = new ArrayList<>();
            tableKeywords.add(tableInfo.getName());
            if (tableInfo.getAliases() != null) {
                tableKeywords.addAll(tableInfo.getAliases());
            }
            docsToSave.add(SemanticSearchDocument.builder()
                    .id("table:" + tableId)
                    .type(SemanticEntityType.TABLE)
                    .keywords(tableKeywords.stream().distinct().toList())
                    .description(tableInfo.getDescription())
                    .entity(tableEntity)
                    .build());

            try {
                List<Column> columns = jdbcMetaService.getColumns(datasourceId, null, request.getSchema(), request.getName());
                for (Column column : columns) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setTableId(tableId);
                    columnInfo.setName(column.name());
                    columnInfo.setColumnType(column.type());
                    columnInfo.setDescription(column.comment());
                    columnInfo.setAliases(new ArrayList<>());
                    columnInfo.setCreatedBy(RequestContext.getUser().getId());
                    columnInfo.setUpdatedBy(RequestContext.getUser().getId());
                    ColumnInfoPO savedColumnPO = columnInfoDAO.save(ColumnMapper.toColumnInfoPO(columnInfo));

                    EntityReference columnEntity = EntityReference.builder()
                            .tableId(tableId)
                            .tableName(tableInfo.getName())
                            .field(column.name())
                            .build();
                    List<String> columnKeywords = new ArrayList<>();
                    columnKeywords.add(column.name());
                    if (columnInfo.getAliases() != null) {
                        columnKeywords.addAll(columnInfo.getAliases());
                    }
                    docsToSave.add(SemanticSearchDocument.builder()
                            .id("field:" + savedColumnPO.getId())
                            .type(SemanticEntityType.FIELD)
                            .keywords(columnKeywords.stream().distinct().toList())
                            .description(columnInfo.getDescription())
                            .entity(columnEntity)
                            .build());
                }
            } catch (Exception e) {
                throw new DatiException(ErrorCode.DS_SYNC_FAILED, request.getName() + ": " + e.getMessage());
            }
        }

        semanticIndexService.saveBatch(docsToSave);
        return tableIds;
    }

    @Transactional
    public void deleteTables(List<String> tableIds) {
        columnInfoDAO.deleteByTableIdIn(tableIds);
        tableInfoDAO.deleteAllById(tableIds);
        semanticIndexService.deleteByEntityTableIds(tableIds);
    }

    @Transactional
    public void deleteTable(String tableId) {
        deleteTables(List.of(tableId));
    }

    @Transactional
    public void updateTable(String tableId, TableInfo tableInfo) {
        TableInfoPO existingPO = tableInfoDAO.findById(tableId).orElseThrow();

        existingPO.setAliases(tableInfo.getAliases());
        existingPO.setDescription(tableInfo.getDescription());
        tableInfoDAO.save(existingPO);

        EntityReference entity = EntityReference.builder()
                .tableId(tableId)
                .tableName(existingPO.getName())
                .build();

        List<String> tableKeywords = new ArrayList<>();
        tableKeywords.add(existingPO.getName());
        if (tableInfo.getAliases() != null) {
            tableKeywords.addAll(tableInfo.getAliases());
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("table:" + tableId)
                .type(SemanticEntityType.TABLE)
                .keywords(tableKeywords.stream().distinct().toList())
                .description(tableInfo.getDescription())
                .entity(entity)
                .build();

        semanticIndexService.save(doc);
    }
}
