package com.dati.semantic.domain.service;

import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.dao.SemanticSearchDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SemanticIndexService {

    private final SemanticSearchDAO semanticSearchDAO;

    public SemanticIndexService(SemanticSearchDAO semanticSearchDAO) {
        this.semanticSearchDAO = semanticSearchDAO;
    }

    public void save(SemanticSearchDocument doc) {
        doc.setUpdatedTime(LocalDateTime.now());
        if (doc.getCreatedTime() == null) {
            doc.setCreatedTime(doc.getUpdatedTime());
        }
        semanticSearchDAO.save(doc);
    }

    public void saveBatch(List<SemanticSearchDocument> docs) {
        LocalDateTime now = LocalDateTime.now();
        docs.forEach(doc -> {
            doc.setUpdatedTime(now);
            if (doc.getCreatedTime() == null) {
                doc.setCreatedTime(now);
            }
        });
        semanticSearchDAO.saveAll(docs);
    }

    public void deleteByEntityTableIds(List<String> tableIds) {
        semanticSearchDAO.deleteByEntity_TableIdIn(tableIds);
    }

    public void deleteByEntityTableId(String tableId) {
        deleteByEntityTableIds(List.of(tableId));
    }

    public void deleteByEntity_SubjectId(String subjectId) {
        semanticSearchDAO.deleteByEntity_SubjectId(subjectId);
    }

    public void deleteById(String id) {
        semanticSearchDAO.deleteById(id);
    }

    public void deleteByTableFieldAndType(String tableId, String field, SemanticEntityType type) {
        semanticSearchDAO.deleteByEntity_TableIdAndEntity_FieldAndType(tableId, field, type);
    }

    public List<SemanticSearchDocument> findByTableFieldAndType(String tableId, String field, SemanticEntityType type) {
        return semanticSearchDAO.findByEntity_TableIdAndEntity_FieldAndType(tableId, field, type);
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

    private Page<SemanticSearchDocument> paginateList(List<SemanticSearchDocument> docs, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), docs.size());
        if (start > docs.size()) {
            return new PageImpl<>(List.of(), pageable, docs.size());
        }
        return new PageImpl<>(docs.subList(start, end), pageable, docs.size());
    }

}