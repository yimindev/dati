package com.dati.semantic.domain.service;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.dao.SemanticSearchDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maintains the semantic search index.
 *
 * <p>The relational database is the source of truth; this index is derived data. Index
 * writes and deletes are therefore best-effort: Elasticsearch failures are logged and never
 * propagated, because the business transaction has already succeeded and the index is derived
 * data that can be rebuilt from the relational store. Programming errors still propagate.
 * Search queries still fail normally when Elasticsearch is unavailable.
 *
 * <p>Callers persist and flush business data before invoking the write methods, so database
 * constraint violations surface before the index is touched.
 */
@Slf4j
@Service
public class SemanticIndexService {

    private final SemanticSearchDAO semanticSearchDAO;

    public SemanticIndexService(SemanticSearchDAO semanticSearchDAO) {
        this.semanticSearchDAO = semanticSearchDAO;
    }

    public void save(SemanticSearchDocument doc) {
        runIndexUpdate("save " + doc.getId(), () -> {
            LocalDateTime now = utcNow();
            doc.setUpdatedTime(now);
            if (doc.getCreatedTime() == null) {
                doc.setCreatedTime(existingCreatedTime(doc.getId()).orElse(now));
            }
            semanticSearchDAO.save(doc);
        });
    }

    public void saveBatch(List<SemanticSearchDocument> docs) {
        runIndexUpdate("save batch of " + docs.size(), () -> {
            LocalDateTime now = utcNow();
            List<String> idsToLookup = docs.stream()
                    .filter(doc -> doc.getCreatedTime() == null && doc.getId() != null)
                    .map(SemanticSearchDocument::getId)
                    .toList();
            Map<String, LocalDateTime> existingCreatedTimes = new HashMap<>();
            if (!idsToLookup.isEmpty()) {
                semanticSearchDAO.findAllById(idsToLookup).forEach(existingDoc -> {
                    if (existingDoc.getCreatedTime() != null) {
                        existingCreatedTimes.put(existingDoc.getId(), existingDoc.getCreatedTime());
                    }
                });
            }
            docs.forEach(doc -> {
                doc.setUpdatedTime(now);
                if (doc.getCreatedTime() == null) {
                    doc.setCreatedTime(existingCreatedTimes.getOrDefault(doc.getId(), now));
                }
            });
            semanticSearchDAO.saveAll(docs);
        });
    }

    private Optional<LocalDateTime> existingCreatedTime(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return semanticSearchDAO.findById(id)
                .map(SemanticSearchDocument::getCreatedTime)
                .filter(createdTime -> createdTime != null);
    }

    public void deleteByEntityTableIds(List<String> tableIds) {
        runIndexUpdate("delete by table ids " + tableIds, () -> semanticSearchDAO.deleteByEntity_TableIdIn(tableIds));
    }

    public void deleteByEntityTableId(String tableId) {
        deleteByEntityTableIds(List.of(tableId));
    }

    public void deleteByEntity_SubjectId(String subjectId) {
        runIndexUpdate("delete by subject " + subjectId, () -> semanticSearchDAO.deleteByEntity_SubjectId(subjectId));
    }

    public void deleteById(String id) {
        runIndexUpdate("delete " + id, () -> semanticSearchDAO.deleteById(id));
    }

    public void deleteByTableFieldAndType(String tableId, String field, SemanticEntityType type) {
        runIndexUpdate("delete field " + tableId + "." + field + " (" + type + ")",
                () -> semanticSearchDAO.deleteByEntity_TableIdAndEntity_FieldAndType(tableId, field, type));
    }

    public void deleteByTableIdAndType(String tableId, SemanticEntityType type) {
        runIndexUpdate("delete table " + tableId + " (" + type + ")",
                () -> semanticSearchDAO.deleteByEntity_TableIdAndType(tableId, type));
    }

    public List<SemanticSearchDocument> findByTableFieldAndType(String tableId, String field, SemanticEntityType type) {
        return semanticSearchDAO.findByEntity_TableIdAndEntity_FieldAndType(tableId, field, type);
    }

    /** 查询某表下指定类型的文档，不区分字段，限制返回数量。 */
    public List<SemanticSearchDocument> findByTableIdAndType(String tableId, SemanticEntityType type, int maxSize) {
        return semanticSearchDAO.findByEntity_TableIdAndType(tableId, type.name(),
                PageRequest.of(0, maxSize)).getContent();
    }

    public Page<SemanticSearchDocument> findByTableFieldAndTypePaginated(
            String tableId, String field, SemanticEntityType type, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            // 当 keyword 为空时，查询所有并手动分页
            List<SemanticSearchDocument> allDocs = semanticSearchDAO
                    .findByEntity_TableIdAndEntity_FieldAndType(tableId, field, type);
            return paginateList(allDocs, pageable);
        }
        return semanticSearchDAO.searchByTableFieldAndKeyword(
                tableId, field, keyword, type.name(), pageable);
    }

    /**
     * Full-text search across all entity types, filtered by data source / subject scope.
     * @param maxResults max ES documents to return
     */
    public List<SemanticSearchDocument> searchMetadata(
            List<String> keywords,
            List<String> datasourceIds,
            List<String> subjectIds,
            int maxResults) {

        if (keywords == null || keywords.isEmpty()) return List.of();

        String queryString = String.join(" ", keywords);
        if (datasourceIds.isEmpty() && subjectIds.isEmpty()) return List.of();

        return semanticSearchDAO.searchByKeyword(queryString, datasourceIds, subjectIds, PageRequest.of(0, maxResults))
                .getContent();
    }

    private Page<SemanticSearchDocument> paginateList(List<SemanticSearchDocument> docs, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), docs.size());
        if (start > docs.size()) {
            return new PageImpl<>(List.of(), pageable, docs.size());
        }
        return new PageImpl<>(docs.subList(start, end), pageable, docs.size());
    }

    /**
     * Runs an index update without letting an Elasticsearch failure escape into the caller's
     * business transaction. Other runtime errors still propagate so programming mistakes are
     * not hidden. The error is logged with enough context to identify the entity.
     */
    private void runIndexUpdate(String operation, Runnable action) {
        try {
            action.run();
        } catch (DataAccessException e) {
            log.error("Semantic index update failed: {}", operation, e);
        }
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
