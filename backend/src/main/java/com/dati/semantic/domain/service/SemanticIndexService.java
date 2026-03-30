package com.dati.semantic.domain.service;

import com.dati.semantic.repository.dao.SemanticSearchDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
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

}