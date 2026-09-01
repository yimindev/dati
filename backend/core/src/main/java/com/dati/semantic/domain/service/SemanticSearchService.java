package com.dati.semantic.domain.service;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.datasource.domain.service.TableMetadataService.TableMeta;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.TermRelationPO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Full-text metadata search orchestration.
 * Handles ES document merging, term relation resolution, batch lookups, and
 * data-source grouping. Returns domain DTOs independent of MCP pojos.
 */
@Service
public class SemanticSearchService {

    private static final int MAX_RESULTS = 50;

    private final SemanticIndexService semanticIndexService;
    private final TermRelationDAO termRelationDAO;
    private final TableMetadataService tableMetadataService;
    private final DataSourceService dataSourceService;
    private final TermService termService;

    public SemanticSearchService(SemanticIndexService semanticIndexService,
                                  TermRelationDAO termRelationDAO,
                                  TableMetadataService tableMetadataService,
                                  DataSourceService dataSourceService,
                                  TermService termService) {
        this.semanticIndexService = semanticIndexService;
        this.termRelationDAO = termRelationDAO;
        this.tableMetadataService = tableMetadataService;
        this.dataSourceService = dataSourceService;
        this.termService = termService;
    }

    public record SearchResult(List<DataSourceGroup> dataSources,
                                List<TermService.TermInfo> terms) {}

    public record DataSourceGroup(String dataSourceId, String dataSourceName,
                                  String dbType, String defaultSchema,
                                  String description, List<TableMeta> tables) {}

    public SearchResult search(List<String> keywords,
                                List<String> datasourceIds,
                                List<String> subjectIds) {
        List<SemanticSearchDocument> docs = semanticIndexService.searchMetadata(
                keywords, datasourceIds, subjectIds, MAX_RESULTS);

        Set<String> tableIdSet = new LinkedHashSet<>();
        Set<String> termIdSet = new LinkedHashSet<>();

        for (SemanticSearchDocument doc : docs) {
            var entity = doc.getEntity();
            if (entity == null) continue;
            switch (doc.getType()) {
                case TABLE, FIELD, FIELD_VALUE -> {
                    if (entity.getTableId() != null) tableIdSet.add(entity.getTableId());
                }
                case TERM -> {
                    String rawId = doc.getId();
                    if (rawId != null && rawId.startsWith("term:"))
                        termIdSet.add(rawId.substring(5));
                }
                case SUBJECT -> { /* 忽略 */ }
            }
        }

        // 术语关联的表
        for (String termId : termIdSet) {
            termRelationDAO.findByTermId(termId).stream()
                    .map(TermRelationPO::getTableId)
                    .filter(Objects::nonNull)
                    .forEach(tableIdSet::add);
        }

        // 批量查表 → 合并匹配值 → 按 ds 分组
        List<TableMeta> tableMetas = mergeWithMatches(
                tableMetadataService.getTableMetasByIds(tableIdSet), docs);
        Map<String, List<TableMeta>> grouped = tableMetas.stream()
                .collect(Collectors.groupingBy(TableMeta::dataSourceId));
        Map<String, DataSourceService.DsBrief> dsMap = dataSourceService.getDataSourceBriefs(grouped.keySet());

        List<DataSourceGroup> dataSources = grouped.entrySet().stream()
                .map(e -> {
                    DataSourceService.DsBrief b = dsMap.get(e.getKey());
                    return new DataSourceGroup(e.getKey(),
                            b != null ? b.name() : e.getKey(),
                            b != null ? b.dbType() != null ? b.dbType().name() : null : null,
                            b != null ? b.defaultSchema() : null,
                            b != null ? b.description() : null,
                            e.getValue());
                })
                .toList();

        // 批量查术语
        List<TermService.TermInfo> terms = termService.getTermsWithSubject(termIdSet);

        return new SearchResult(dataSources, terms);
    }

    /**
     * Merges FIELD_VALUE hit values from ES into column sampleValues.
     * Matched values come first; if fewer than 5, random existing values fill up.
     * If matched values already reach or exceed 5, random values are discarded.
     */
    List<TableMeta> mergeWithMatches(List<TableMeta> metas,
                                      List<SemanticSearchDocument> docs) {
        Map<String, Map<String, List<String>>> matched = new LinkedHashMap<>();
        for (SemanticSearchDocument doc : docs) {
            if (doc.getType() != SemanticEntityType.FIELD_VALUE) continue;
            var entity = doc.getEntity();
            if (entity == null || entity.getTableId() == null
                    || entity.getField() == null) continue;
            if (doc.getKeywords() == null || doc.getKeywords().isEmpty()) continue;
            matched.computeIfAbsent(entity.getTableId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(entity.getField(), k -> new ArrayList<>())
                    .addAll(doc.getKeywords());
        }
        if (matched.isEmpty()) return metas;

        return metas.stream().map(meta -> {
            Map<String, List<String>> perTable = matched.get(meta.tableId());
            if (perTable == null) return meta;
            List<ColumnDef> merged = meta.columns().stream()
                    .map(col -> mergeColumn(col, perTable.get(col.name())))
                    .toList();
            return new TableMeta(meta.tableId(), meta.schema(), meta.tableName(),
                    meta.description(), meta.aliases(), meta.dataSourceId(), merged);
        }).toList();
    }

    private static ColumnDef mergeColumn(ColumnDef col, List<String> matched) {
        if (matched == null || matched.isEmpty()) return col;
        List<String> all = new ArrayList<>(matched);
        for (String v : col.sampleValues()) {
            if (all.size() >= 5) break;
            if (!all.contains(v)) all.add(v);
        }
        return new ColumnDef(col.name(), col.type(), col.comment(),
                col.aliases(), all);
    }
}
