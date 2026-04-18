package com.dati.datasource.domain.service;

import com.dati.base.pojo.PageReq;
import com.dati.config.ColumnValueConfig;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
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
import java.util.Optional;
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

    public ColumnValueService(
            ColumnInfoDAO columnInfoDAO,
            TableInfoDAO tableInfoDAO,
            JdbcMetaService jdbcMetaService,
            SemanticIndexService semanticIndexService,
            ColumnValueConfig columnValueConfig) {
        this.columnInfoDAO = columnInfoDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
        this.columnValueConfig = columnValueConfig;
    }

    public void extractValues(String datasourceId, String columnId, boolean overwrite) throws SQLException {
        ColumnInfoPO columnPO = columnInfoDAO.findById(columnId)
                .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnId));

        String tableId = columnPO.getTableId();
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
                columnName, getTableName(tableId), sampleLimit);

        List<Map<String, Object>> results = jdbcMetaService.executeSql(datasourceId, sql);

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

    public void saveValues(String columnId, List<ValueItem> values, List<String> deletedIds) {
        if (deletedIds != null && !deletedIds.isEmpty()) {
            for (String id : deletedIds) {
                semanticIndexService.deleteById(id);
            }
        }

        if (values != null && !values.isEmpty()) {
            ColumnInfoPO columnPO = columnInfoDAO.findById(columnId)
                    .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnId));
            List<SemanticSearchDocument> docs = new ArrayList<>();
            for (ValueItem item : values) {
                EntityReference entity = EntityReference.builder()
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

    public Page<ValueItem> getValues(String columnId, PageReq pageReq, String keyword) {
        ColumnInfoPO columnPO = columnInfoDAO.findById(columnId)
                .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnId));

        Page<SemanticSearchDocument> docsPage = semanticIndexService.findByTableFieldAndTypePaginated(
                columnPO.getTableId(),
                columnPO.getName(),
                SemanticEntityType.FIELD_VALUE,
                keyword,
                pageReq.toPageRequest()
        );

        return docsPage.map(ColumnValueService::toValueItem);
    }

    private String getTableName(String tableId) {
        return Optional.ofNullable(tableId)
                .flatMap(tableInfoDAO::findById)
                .map(TableInfoPO::getName)
                .orElseThrow(() -> new IllegalStateException("Table not found: " + tableId));
    }

    @Data
    public static class ValueItem {
        private String id;
        private String value;
        private List<String> synonyms = new ArrayList<>();
    }
}
