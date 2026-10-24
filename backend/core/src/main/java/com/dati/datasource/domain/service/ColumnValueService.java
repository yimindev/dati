package com.dati.datasource.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.base.pojo.PageReq;
import com.dati.config.ColumnValueConfig;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.service.PermissionService;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ColumnValueService {

    private final ColumnInfoDAO columnInfoDAO;
    private final TableInfoDAO tableInfoDAO;
    private final JdbcMetaService jdbcMetaService;
    private final SemanticIndexService semanticIndexService;
    private final ColumnValueConfig columnValueConfig;
    private final PermissionService permissionService;

    public ColumnValueService(
            ColumnInfoDAO columnInfoDAO,
            TableInfoDAO tableInfoDAO,
            JdbcMetaService jdbcMetaService,
            SemanticIndexService semanticIndexService,
            ColumnValueConfig columnValueConfig,
            PermissionService permissionService) {
        this.columnInfoDAO = columnInfoDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
        this.columnValueConfig = columnValueConfig;
        this.permissionService = permissionService;
    }

    public void extractValues(String datasourceId, String tableId, String columnId, boolean overwrite)
            throws SQLException {
        ColumnPath path = requireColumnInPath(datasourceId, tableId, columnId);
        ColumnInfoPO columnPO = path.column();
        TableInfoPO tablePO = path.table();
        permissionService.requireDataSource(tablePO.getDataSourceId(), Permission.EDIT);

        String columnName = columnPO.getName();

        if (overwrite) {
            List<ValueItem> existing = getAllValuesForExtract(columnPO);
            for (ValueItem item : existing) {
                semanticIndexService.deleteById(item.getId());
            }
        }

        Integer sampleLimit = columnValueConfig.getColumnValueSampleLimit();
        Integer lengthLimit = columnValueConfig.getColumnValueLengthLimit();

        String sql = String.format("SELECT DISTINCT %s FROM %s LIMIT %d",
                columnName, tablePO.getName(), sampleLimit);

        List<Map<String, Object>> results = jdbcMetaService.executeSql(tablePO.getDataSourceId(), sql);

        Set<String> existingValues = overwrite
                ? Collections.emptySet()
                : getAllValuesForExtract(columnPO).stream()
                        .map(ValueItem::getValue)
                        .collect(Collectors.toSet());

        List<SemanticSearchDocument> docs = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Object value = row.get(columnName);
            if (value != null) {
                String strValue = value.toString();
                if (strValue.length() > lengthLimit) {
                    strValue = strValue.substring(0, lengthLimit);
                }
                if (existingValues.contains(strValue)) {
                    continue;
                }
                String id = UUID.randomUUID().toString();
                EntityReference entity = EntityReference.builder()
                        .datasourceId(tablePO.getDataSourceId())
                        .tableId(tableId)
                        .field(columnName)
                        .build();
                SemanticSearchDocument doc = SemanticSearchDocument.builder()
                        .id(id)
                        .type(SemanticEntityType.FIELD_VALUE)
                        .keywords(List.of(strValue))
                        .entity(entity)
                        .build();
                docs.add(doc);
            }
        }
        semanticIndexService.saveBatch(docs);
    }

    private List<ValueItem> getAllValuesForExtract(ColumnInfoPO columnPO) {
        List<SemanticSearchDocument> docs = semanticIndexService.findByTableFieldAndType(
                columnPO.getTableId(), columnPO.getName(), SemanticEntityType.FIELD_VALUE);
        return docs.stream().map(ColumnValueService::toValueItem).toList();
    }

    private static ValueItem toValueItem(SemanticSearchDocument doc) {
        ValueItem item = new ValueItem();
        item.setId(doc.getId());
        List<String> keywords = doc.getKeywords();
        if (keywords != null && !keywords.isEmpty()) {
            item.setValue(keywords.getFirst());
            if (keywords.size() > 1) {
                item.setSynonyms(keywords.subList(1, keywords.size()));
            }
        }
        return item;
    }

    public void saveValues(String datasourceId, String tableId, String columnId,
                           List<ValueItem> values, List<String> deletedIds) {
        ColumnPath path = requireColumnInPath(datasourceId, tableId, columnId);
        ColumnInfoPO columnPO = path.column();
        String actualDatasourceId = path.table().getDataSourceId();
        permissionService.requireDataSource(actualDatasourceId, Permission.EDIT);

        if (deletedIds != null && !deletedIds.isEmpty()) {
            for (String id : deletedIds) {
                semanticIndexService.deleteById(id);
            }
        }

        if (values != null && !values.isEmpty()) {
            List<SemanticSearchDocument> docs = new ArrayList<>();
            for (ValueItem item : values) {
                EntityReference entity = EntityReference.builder()
                        .datasourceId(actualDatasourceId)
                        .tableId(columnPO.getTableId())
                        .field(columnPO.getName())
                        .build();

                List<String> keywords = new ArrayList<>();
                keywords.add(item.getValue());
                if (item.getSynonyms() != null) {
                    keywords.addAll(item.getSynonyms());
                }

                String id = (item.getId() == null || item.getId().isEmpty())
                        ? UUID.randomUUID().toString()
                        : item.getId();

                SemanticSearchDocument doc = SemanticSearchDocument.builder()
                        .id(id)
                        .type(SemanticEntityType.FIELD_VALUE)
                        .keywords(keywords.stream().distinct().toList())
                        .entity(entity)
                        .build();
                docs.add(doc);
            }
            semanticIndexService.saveBatch(docs);
        }
    }

    public Page<ValueItem> getValues(String datasourceId, String tableId, String columnId,
                                     PageReq pageReq, String keyword) {
        ColumnPath path = requireColumnInPath(datasourceId, tableId, columnId);
        ColumnInfoPO columnPO = path.column();
        permissionService.requireDataSource(path.table().getDataSourceId(), Permission.VIEW);

        Page<SemanticSearchDocument> docsPage = semanticIndexService.findByTableFieldAndTypePaginated(
                columnPO.getTableId(),
                columnPO.getName(),
                SemanticEntityType.FIELD_VALUE,
                keyword,
                pageReq.toPageRequest()
        );

        return docsPage.map(ColumnValueService::toValueItem);
    }

    private ColumnPath requireColumnInPath(String datasourceId, String tableId, String columnId) {
        ColumnInfoPO column = columnInfoDAO.findById(columnId)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_COLUMN_NOT_FOUND, columnId));
        if (!Objects.equals(column.getTableId(), tableId)) {
            throw new DatiException(ErrorCode.DS_COLUMN_NOT_FOUND, columnId);
        }

        TableInfoPO table = tableInfoDAO.findById(tableId)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_TABLE_NOT_FOUND, tableId));
        if (!Objects.equals(table.getDataSourceId(), datasourceId)) {
            throw new DatiException(ErrorCode.DS_TABLE_NOT_FOUND, tableId);
        }
        return new ColumnPath(column, table);
    }

    private record ColumnPath(ColumnInfoPO column, TableInfoPO table) {
    }

    @Data
    public static class ValueItem {
        private String id;
        private String value;
        private List<String> synonyms = new ArrayList<>();
    }
}
