package com.dati.datasource.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.BasePO;
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
import com.dati.db.Column;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.service.PermissionService;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ColumnService {

    private final ColumnInfoDAO columnInfoDAO;
    private final TableInfoDAO tableInfoDAO;
    private final JdbcMetaService jdbcMetaService;
    private final SemanticIndexService semanticIndexService;
    private final PermissionService permissionService;
    private final TermRelationDAO termRelationDAO;

    public ColumnService(ColumnInfoDAO columnInfoDAO, TableInfoDAO tableInfoDAO,
                         JdbcMetaService jdbcMetaService, SemanticIndexService semanticIndexService,
                         PermissionService permissionService, TermRelationDAO termRelationDAO) {
        this.columnInfoDAO = columnInfoDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
        this.permissionService = permissionService;
        this.termRelationDAO = termRelationDAO;
    }

    public Page<ColumnInfo> getColumns(PageReq pageReq, String datasourceId, String tableId, String keyword) {
        TableInfoPO tablePO = requireTableInPath(datasourceId, tableId);
        permissionService.requireDataSource(tablePO.getDataSourceId(), Permission.VIEW);

        Sort sortBy = Sort.by(Sort.Direction.ASC, BasePO.Fields.createdAt);
        if (StringUtils.isEmpty(keyword)) {
            return columnInfoDAO.findByTableId(tableId, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
        }
        return columnInfoDAO.findByTableIdAndKeyword(tableId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
    }

    @Transactional
    public void updateColumn(String datasourceId, String tableId, String id, ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = columnInfoDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_COLUMN_NOT_FOUND, id));
        if (!Objects.equals(columnInfoPO.getTableId(), tableId)) {
            throw new DatiException(ErrorCode.DS_COLUMN_NOT_FOUND, id);
        }
        TableInfoPO tablePO = requireTableInPath(datasourceId, tableId);
        permissionService.requireDataSource(tablePO.getDataSourceId(), Permission.EDIT);

        boolean wasEnabled = columnInfoPO.isExtractValueEnabled();

        if (columnInfo.getColumnType() != null) {
            columnInfoPO.setColumnType(columnInfo.getColumnType());
        }
        if (columnInfo.getAliases() != null) {
            columnInfoPO.setAliases(columnInfo.getAliases());
        }
        if (columnInfo.getDescription() != null) {
            columnInfoPO.setDescription(columnInfo.getDescription());
        }
        if (columnInfo.getExtractValueEnabled() != null) {
            columnInfoPO.setExtractValueEnabled(columnInfo.getExtractValueEnabled());
        }
        if (columnInfo.getUpdatedBy() != null) {
            columnInfoPO.setUpdatedBy(columnInfo.getUpdatedBy());
        }
        boolean nowEnabled = columnInfoPO.isExtractValueEnabled();
        // Flush before updating the derived field document.
        columnInfoDAO.saveAndFlush(columnInfoPO);

        // 从开启变为禁用，清理该列的值数据
        if (wasEnabled && !nowEnabled) {
            semanticIndexService.deleteByTableFieldAndType(
                    columnInfoPO.getTableId(),
                    columnInfoPO.getName(),
                    SemanticEntityType.FIELD_VALUE
            );
        }

        TableInfo tableInfo = TableMapper.toTableInfo(tablePO);
        EntityReference entity = EntityReference.builder()
                .datasourceId(tableInfo.getDatasourceId())
                .tableId(tableInfo.getId())
                .tableName(tableInfo.getName())
                .field(columnInfoPO.getName())
                .build();
        List<String> columnKeywords = new ArrayList<>();
        columnKeywords.add(columnInfoPO.getName());
        if (columnInfoPO.getAliases() != null) {
            columnKeywords.addAll(columnInfoPO.getAliases());
        }
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("field:" + id)
                .type(SemanticEntityType.FIELD)
                .keywords(columnKeywords.stream().distinct().toList())
                .description(columnInfoPO.getDescription())
                .entity(entity)
                .build();
        semanticIndexService.save(doc);
    }

    @Transactional
    public void syncColumns(String datasourceId, String tableId, boolean overwriteExisting) throws SQLException {
        TableInfoPO tablePO = requireTableInPath(datasourceId, tableId);
        permissionService.requireDataSource(tablePO.getDataSourceId(), Permission.EDIT);
        TableInfo tableInfo = TableMapper.toTableInfo(tablePO);
        
        Map<String, ColumnInfoPO> existingColumns = columnInfoDAO.findByTableId(tableId)
                .stream()
                .collect(Collectors.toMap(ColumnInfoPO::getName, Function.identity()));
        
        List<Column> dbColumns = jdbcMetaService.getColumns(datasourceId, tableInfo.getSchema(), tableInfo.getName());

        Set<String> newColumnNames = dbColumns.stream().map(Column::name).collect(Collectors.toSet());
        List<ColumnInfoPO> removedColumns = existingColumns.values().stream()
                .filter(po -> !newColumnNames.contains(po.getName()))
                .toList();

        // 只删除消失的列，保留既有列的 ID 与审计信息（列身份稳定，外部引用不失效）
        if (!removedColumns.isEmpty()) {
            termRelationDAO.deleteByTableIdAndFieldNameIn(tableId,
                    removedColumns.stream().map(ColumnInfoPO::getName).toList());
            columnInfoDAO.deleteAll(removedColumns);
        }
        
        User user = RequestContext.getUser();
        String userId = user != null ? user.getId() : null;
        
        List<ColumnInfoPO> columnInfoPOList = dbColumns.stream().map(column -> {
            // 仍存在的列复用既有 PO（保留 id/createdBy/createdAt/aliases/extractValueEnabled），新列才新建
            ColumnInfoPO existing = existingColumns.get(column.name());
            ColumnInfoPO columnInfoPO = existing != null ? existing : new ColumnInfoPO();
            if (existing == null) {
                columnInfoPO.setTableId(tableId);
                columnInfoPO.setCreatedBy(userId);
            }
            columnInfoPO.setName(column.name());
            columnInfoPO.setColumnType(column.type());
            
            if (existing != null) {
                String dbComment = column.comment();
                if (overwriteExisting && StringUtils.isNotEmpty(dbComment)) {
                    columnInfoPO.setDescription(dbComment);
                } else {
                    columnInfoPO.setDescription(existing.getDescription());
                }
            } else {
                String dbComment = column.comment();
                if (StringUtils.isNotEmpty(dbComment)) {
                    columnInfoPO.setDescription(dbComment);
                }
            }
            
            columnInfoPO.setUpdatedBy(userId);
            return columnInfoPO;
        }).toList();
        
        List<ColumnInfoPO> savedList = columnInfoDAO.saveAll(columnInfoPOList);
        // Flush column changes before rebuilding the derived field documents.
        columnInfoDAO.flush();
        
        // FIELD 结构索引整表重建；重建前保留既有文档的 createdTime（删除后无法再从索引读回）
        Map<String, LocalDateTime> existingCreatedTimes = semanticIndexService
                .findByTableIdAndType(tableId, SemanticEntityType.FIELD, 10_000)
                .stream()
                .filter(doc -> doc.getCreatedTime() != null
                        && doc.getEntity() != null
                        && doc.getEntity().getField() != null)
                .collect(Collectors.toMap(
                        doc -> doc.getEntity().getField(),
                        SemanticSearchDocument::getCreatedTime,
                        (first, second) -> first));
        semanticIndexService.deleteByTableIdAndType(tableId, SemanticEntityType.FIELD);
        for (ColumnInfoPO removed : removedColumns) {
            semanticIndexService.deleteByTableFieldAndType(tableId, removed.getName(), SemanticEntityType.FIELD_VALUE);
        }
        
        List<SemanticSearchDocument> docs = savedList.stream().map(po -> {
            EntityReference entity = EntityReference.builder()
                    .datasourceId(datasourceId)
                    .tableId(tableId)
                    .tableName(tableInfo.getName())
                    .field(po.getName())
                    .build();
            List<String> columnKeywords = new ArrayList<>();
            columnKeywords.add(po.getName());
            if (po.getAliases() != null) {
                columnKeywords.addAll(po.getAliases());
            }
            return SemanticSearchDocument.builder()
                    .id("field:" + po.getId())
                    .type(SemanticEntityType.FIELD)
                    .keywords(columnKeywords.stream().distinct().toList())
                    .description(po.getDescription())
                    .createdTime(existingCreatedTimes.get(po.getName()))
                    .entity(entity)
                    .build();
        }).toList();
        semanticIndexService.saveBatch(docs);
    }

    /**
     * Loads the table and verifies it belongs to the data source in the request path. A
     * mismatch is reported as "table not found" so a path from another data source never
     * operates on it.
     */
    private TableInfoPO requireTableInPath(String datasourceId, String tableId) {
        TableInfoPO table = tableInfoDAO.findById(tableId)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_TABLE_NOT_FOUND, tableId));
        if (!Objects.equals(table.getDataSourceId(), datasourceId)) {
            throw new DatiException(ErrorCode.DS_TABLE_NOT_FOUND, tableId);
        }
        return table;
    }

}
