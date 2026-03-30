package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.mapper.TermRelationMapper;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.TermPO;
import com.dati.semantic.repository.po.TermRelationPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TermService {

    private final TermDAO termDAO;
    private final TermRelationDAO termRelationDAO;
    private final SubjectTableDAO subjectTableDAO;
    private final SemanticIndexService semanticIndexService;

    public TermService(TermDAO termDAO, TermRelationDAO termRelationDAO,
                       SubjectTableDAO subjectTableDAO, SemanticIndexService semanticIndexService) {
        this.termDAO = termDAO;
        this.termRelationDAO = termRelationDAO;
        this.subjectTableDAO = subjectTableDAO;
        this.semanticIndexService = semanticIndexService;
    }

    @Transactional
    public Term createTerm(String subjectId, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Term name cannot be null or empty");
        }
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();

        TermPO termPO = new TermPO();
        termPO.setId(id);
        termPO.setSubjectId(subjectId);
        termPO.setName(name);
        termPO.setDescription(description);
        termPO.setCreatedAt(now);
        termPO.setUpdatedAt(now);
        termDAO.save(termPO);

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("term:" + id)
                .type(SemanticEntityType.TERM)
                .keywords(List.of(name))
                .description(description)
                .entity(EntityReference.builder().subjectId(subjectId).build())
                .build();
        semanticIndexService.save(doc);

        return toTerm(termPO);
    }

    @Transactional
    public Term updateTerm(String id, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Term name cannot be null or empty");
        }
        TermPO termPO = termDAO.findById(id)
                .orElseThrow(() -> new DatiException("Term not found: " + id));

        termPO.setName(name);
        termPO.setDescription(description);
        termPO.setUpdatedAt(Instant.now());
        termDAO.save(termPO);

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("term:" + id)
                .type(SemanticEntityType.TERM)
                .keywords(List.of(name))
                .description(description)
                .entity(EntityReference.builder().subjectId(termPO.getSubjectId()).build())
                .build();
        semanticIndexService.save(doc);

        return toTerm(termPO);
    }

    @Transactional
    public void deleteTerm(String id) {
        if (!termDAO.existsById(id)) {
            throw new DatiException("Term not found: " + id);
        }
        termRelationDAO.deleteByTermId(id);
        termDAO.deleteById(id);
        semanticIndexService.deleteById("term:" + id);
    }

    @Transactional
    public void linkEntity(String termId, SemanticEntityType entityType, String tableId, String fieldName) {
        TermPO termPO = termDAO.findById(termId)
                .orElseThrow(() -> new DatiException("Term not found: " + termId));

        if (entityType == SemanticEntityType.FIELD && fieldName == null) {
            throw new IllegalArgumentException("fieldName is required for FIELD entity type");
        }

        if (!subjectTableDAO.existsBySubjectIdAndTableId(termPO.getSubjectId(), tableId)) {
            throw new IllegalStateException("Table does not belong to subject");
        }

        TermRelationPO relationPO = TermRelationMapper.toPO(termId, entityType, tableId, fieldName);
        relationPO.setCreatedAt(Instant.now());
        termRelationDAO.save(relationPO);
    }

    @Transactional
    public void unlinkEntity(String termId, String tableId, String fieldName) {
        if (fieldName == null) {
            termRelationDAO.deleteByTermIdAndTableId(termId, tableId);
        } else {
            termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName)
                    .ifPresent(relation -> termRelationDAO.delete(relation));
        }
    }

    @Transactional(readOnly = true)
    public List<Term> getTermsBySubject(String subjectId) {
        return termDAO.findBySubjectId(subjectId).stream()
                .map(this::toTerm)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TermRelation> getTermRelations(String termId) {
        return termRelationDAO.findByTermId(termId).stream()
                .map(this::toTermRelation)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Term getTermById(String id) {
        TermPO termPO = termDAO.findById(id)
                .orElseThrow(() -> new DatiException("Term not found: " + id));
        return toTerm(termPO);
    }

    private Term toTerm(TermPO po) {
        Term term = new Term();
        term.setId(po.getId());
        term.setSubjectId(po.getSubjectId());
        term.setName(po.getName());
        term.setDescription(po.getDescription());
        term.setCreatedAt(po.getCreatedAt());
        term.setUpdatedAt(po.getUpdatedAt());
        return term;
    }

    private TermRelation toTermRelation(TermRelationPO po) {
        return TermRelation.builder()
                .id(po.getId())
                .termId(po.getTermId())
                .entityType(po.getEntityType())
                .tableId(po.getTableId())
                .fieldName(po.getFieldName())
                .build();
    }

}