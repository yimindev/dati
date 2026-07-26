package com.dati.datasource.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.BasePO;
import com.dati.base.pojo.PageReq;
import com.dati.common.StringUtils;
import com.dati.db.Column;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.mapper.TableMapper;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.ColumnMapper;
import com.dati.datasource.repository.po.ColumnInfoPO;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ColumnService {

    private final ColumnInfoDAO columnInfoDAO;

    private final TableInfoDAO tableInfoDAO;

    private final JdbcMetaService jdbcMetaService;

    private final SemanticIndexService semanticIndexService;

    public ColumnService(ColumnInfoDAO columnInfoDAO, TableInfoDAO tableInfoDAO, JdbcMetaService jdbcMetaService, SemanticIndexService semanticIndexService) {
        this.columnInfoDAO = columnInfoDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
    }

    public Page<ColumnInfo> getColumns(PageReq pageReq, String tableId, String keyword) {
        Sort sortBy = Sort.by(Sort.Direction.ASC, BasePO.Fields.createdAt);
        if (StringUtils.isEmpty(keyword)) {
            return columnInfoDAO.findByTableId(tableId, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
        }
        return columnInfoDAO.findByTableIdAndNameContaining(tableId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
    }

    public void updateColumn(String id, ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = columnInfoDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, "Column not found: " + id));

        boolean wasEnabled = columnInfoPO.isExtractValueEnabled();

        if (columnInfo.getName() != null) {
            columnInfoPO.setName(columnInfo.getName());
        }
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
        boolean nowEnabled = columnInfoPO.isExtractValueEnabled();
        columnInfoDAO.save(columnInfoPO);

        // 从开启变为禁用，清理该列的值数据
        if (wasEnabled && !nowEnabled) {
            semanticIndexService.deleteByTableFieldAndType(
                    columnInfoPO.getTableId(),
                    columnInfoPO.getName(),
                    SemanticEntityType.FIELD_VALUE
            );
        }

        TableInfo tableInfo = TableMapper.toTableInfo(tableInfoDAO.findById(columnInfoPO.getTableId()).orElseThrow());
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
                .description(columnInfo.getDescription())
                .entity(entity)
                .build();
        semanticIndexService.save(doc);
    }

    @Transactional
    public void syncColumns(String datasourceId, String tableId, boolean overwriteExisting) throws SQLException {
        TableInfo tableInfo = TableMapper.toTableInfo(tableInfoDAO.findById(tableId)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, "Table not found: " + tableId)));
        
        Map<String, ColumnInfoPO> existingColumns = columnInfoDAO.findByTableId(tableId)
                .stream()
                .collect(Collectors.toMap(ColumnInfoPO::getName, Function.identity()));
        
        List<Column> dbColumns = jdbcMetaService.getColumns(datasourceId, null, tableInfo.getSchema(), tableInfo.getName());
        
        columnInfoDAO.deleteByTableId(tableId);
        
        User user = RequestContext.getUser();
        String userId = user != null ? user.getId() : null;
        
        List<ColumnInfoPO> columnInfoPOList = dbColumns.stream().map(column -> {
            ColumnInfoPO columnInfoPO = new ColumnInfoPO();
            columnInfoPO.setTableId(tableId);
            columnInfoPO.setName(column.name());
            columnInfoPO.setColumnType(column.type());
            
            ColumnInfoPO existing = existingColumns.get(column.name());
            
            if (existing != null) {
                columnInfoPO.setAliases(existing.getAliases());
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
            
            columnInfoPO.setCreatedBy(userId);
            columnInfoPO.setUpdatedBy(userId);
            return columnInfoPO;
        }).toList();
        
        List<ColumnInfoPO> savedList = columnInfoDAO.saveAll(columnInfoPOList);
        
        semanticIndexService.deleteByEntityTableId(tableId);
        
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
                    .entity(entity)
                    .build();
        }).toList();
        semanticIndexService.saveBatch(docs);
    }

}
