package com.dati.datasource.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
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
        if (StringUtils.isBlank(keyword)) {
            return columnInfoDAO.findByTableId(tableId, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
        }
        return columnInfoDAO.findByTableIdAndNameContaining(tableId, keyword, pageReq.toPageRequest().withSort(sortBy)).map(ColumnMapper::toColumnInfo);
    }

    public void updateColumn(String id, ColumnInfo columnInfo) {
        ColumnInfoPO columnInfoPO = columnInfoDAO.findById(id).orElseThrow();
        columnInfoPO.setName(columnInfo.getName());
        columnInfoPO.setColumnType(columnInfo.getColumnType());
        columnInfoPO.setDisplayName(columnInfo.getDisplayName());
        columnInfoPO.setDescription(columnInfo.getDescription());
        columnInfoDAO.save(columnInfoPO);

        TableInfo tableInfo = TableMapper.toTableInfo(tableInfoDAO.findById(columnInfoPO.getTableId()).orElseThrow());
        EntityReference entity = EntityReference.builder()
                .tableId(tableInfo.getId())
                .tableName(tableInfo.getName())
                .field(columnInfoPO.getName())
                .build();
        List<String> columnKeywords = new ArrayList<>();
        columnKeywords.add(columnInfoPO.getName());
        if (StringUtils.isNotBlank(columnInfoPO.getDisplayName())) {
            columnKeywords.add(columnInfoPO.getDisplayName());
        }
        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("field:" + id)
                .type(SemanticEntityType.FIELD)
                .keywords(columnKeywords)
                .description(columnInfo.getDescription())
                .entity(entity)
                .build();
        semanticIndexService.save(doc);
    }

    @Transactional
    public void syncColumns(String datasourceId, String tableId) throws SQLException {
        TableInfo tableInfo = TableMapper.toTableInfo(tableInfoDAO.findById(tableId).orElseThrow());
        List<Column> columns = jdbcMetaService.getColumns(datasourceId, null, tableInfo.getSchema(), tableInfo.getName());
        
        columnInfoDAO.deleteByTableId(tableId);
        
        User user = RequestContext.getUser();
        String userId = user != null ? user.getId() : null;
        
        List<ColumnInfoPO> columnInfoPOList = columns.stream().map(column -> {
            ColumnInfoPO columnInfoPO = new ColumnInfoPO();
            columnInfoPO.setTableId(tableId);
            columnInfoPO.setName(column.name());
            columnInfoPO.setColumnType(column.type());
            columnInfoPO.setDisplayName(column.comment());
            columnInfoPO.setDescription(null);
            columnInfoPO.setCreatedBy(userId);
            columnInfoPO.setUpdatedBy(userId);
            return columnInfoPO;
        }).toList();
        
        List<ColumnInfoPO> savedList = columnInfoDAO.saveAll(columnInfoPOList);
        
        semanticIndexService.deleteByEntityTableId(tableId);
        
        List<SemanticSearchDocument> docs = savedList.stream().map(po -> {
            EntityReference entity = EntityReference.builder()
                    .tableId(tableId)
                    .tableName(tableInfo.getName())
                    .field(po.getName())
                    .build();
            List<String> columnKeywords = new ArrayList<>();
            columnKeywords.add(po.getName());
            if (StringUtils.isNotBlank(po.getDisplayName())) {
                columnKeywords.add(po.getDisplayName());
            }
            return SemanticSearchDocument.builder()
                    .id("field:" + po.getId())
                    .type(SemanticEntityType.FIELD)
                    .keywords(columnKeywords)
                    .description(po.getDescription())
                    .entity(entity)
                    .build();
        }).toList();
        semanticIndexService.saveBatch(docs);
    }

}
