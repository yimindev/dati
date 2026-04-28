package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.mapper.TermMapper;
import com.dati.semantic.repository.mapper.TermRelationMapper;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.TermPO;
import com.dati.semantic.repository.po.TermRelationPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TermService {

    private final TermDAO termDAO;
    private final TermRelationDAO termRelationDAO;
    private final SubjectTableDAO subjectTableDAO;
    private final TableInfoDAO tableInfoDAO;
    private final SemanticIndexService semanticIndexService;

    public TermService(TermDAO termDAO, TermRelationDAO termRelationDAO,
                       SubjectTableDAO subjectTableDAO, TableInfoDAO tableInfoDAO,
                       SemanticIndexService semanticIndexService) {
        this.termDAO = termDAO;
        this.termRelationDAO = termRelationDAO;
        this.subjectTableDAO = subjectTableDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.semanticIndexService = semanticIndexService;
    }

    @Transactional
    public Term createTerm(String subjectId, String name, String description, List<String> aliases) {
        TermPO termPO = TermMapper.toPO(subjectId, name, description, aliases);
        termDAO.save(termPO);

        String id = termPO.getId();

        List<String> keywords = new java.util.ArrayList<>();
        keywords.add(name);
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("term:" + id)
                .type(SemanticEntityType.TERM)
                .keywords(keywords.stream().distinct().toList())
                .description(description)
                .entity(EntityReference.builder().subjectId(subjectId).build())
                .build();
        semanticIndexService.save(doc);

        return TermMapper.toTerm(termPO);
    }

    @Transactional
    public Term updateTerm(String id, String name, String description, List<String> aliases) {
        TermPO termPO = termDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_TERM_NOT_FOUND, id));

        termPO.setName(name);
        termPO.setDescription(description);
        termPO.setAliases(aliases != null ? aliases : new java.util.ArrayList<>());
        termDAO.save(termPO);

        List<String> keywords = new java.util.ArrayList<>();
        keywords.add(name);
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("term:" + id)
                .type(SemanticEntityType.TERM)
                .keywords(keywords.stream().distinct().toList())
                .description(description)
                .entity(EntityReference.builder().subjectId(termPO.getSubjectId()).build())
                .build();
        semanticIndexService.save(doc);

        return TermMapper.toTerm(termPO);
    }

    @Transactional
    public void deleteTerm(String id) {
        if (!termDAO.existsById(id)) {
            throw new DatiException(ErrorCode.SM_TERM_NOT_FOUND, id);
        }
        termRelationDAO.deleteByTermId(id);
        termDAO.deleteById(id);
        semanticIndexService.deleteById("term:" + id);
    }

    @Transactional
    public void linkEntity(String termId, SemanticEntityType entityType, String tableId, String fieldName) {
        TermPO termPO = termDAO.findById(termId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_TERM_NOT_FOUND, termId));

        if (entityType == SemanticEntityType.FIELD && fieldName == null) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "fieldName is required");
        }

        if (!subjectTableDAO.existsBySubjectIdAndTableId(termPO.getSubjectId(), tableId)) {
            throw new DatiException(ErrorCode.SM_TABLE_NOT_IN_SUBJECT, tableId, termPO.getSubjectId());
        }

        TermRelationPO relationPO = TermRelationMapper.toPO(termId, entityType, tableId, fieldName);
        termRelationDAO.save(relationPO);
    }

    @Transactional
    public void unlinkEntity(String termId, String tableId, String fieldName) {
        if (fieldName == null) {
            termRelationDAO.deleteByTermIdAndTableId(termId, tableId);
        } else {
            termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName)
                    .ifPresent(termRelationDAO::delete);
        }
    }

    @Transactional(readOnly = true)
    public List<Term> getTermsBySubject(String subjectId) {
        return termDAO.findBySubjectId(subjectId).stream()
                .map(TermMapper::toTerm)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TermRelation> getTermRelations(String termId) {
        List<TermRelationPO> relationPOList = termRelationDAO.findByTermId(termId);
        Map<String, TableInfoPO> tableInfoMap = getTableInfoMap(termId, relationPOList);
        return relationPOList.stream()
                .map(relation -> toTermRelation(relation, tableInfoMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Term getTermById(String id) {
        TermPO termPO = termDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_TERM_NOT_FOUND, id));
        return TermMapper.toTerm(termPO);
    }

    @Transactional(readOnly = true)
    public Term getTermByIdWithRelations(String id) {
        Term term = getTermById(id);
        List<TermRelation> relations = getTermRelations(id);
        term.setRelations(relations);
        return term;
    }

    private Map<String, TableInfoPO> getTableInfoMap(String termId, List<TermRelationPO> relations) {
        Set<String> tableIds = relations.stream()
                .map(TermRelationPO::getTableId)
                .filter(tableId -> tableId != null && !tableId.isBlank())
                .collect(Collectors.toSet());
        if (tableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return tableInfoDAO.findAllById(tableIds).stream()
                    .collect(Collectors.toMap(TableInfoPO::getId, table -> table));
        } catch (Exception e) {
            log.warn("Failed to load table metadata for termId={}, tableIds={}", termId, tableIds, e);
            return Collections.emptyMap();
        }
    }

    private TermRelation toTermRelation(TermRelationPO po, Map<String, TableInfoPO> tableInfoMap) {
        String tableName = null;
        String schema = null;
        TableInfoPO tableInfo = tableInfoMap.get(po.getTableId());
        if (tableInfo != null) {
            tableName = tableInfo.getName();
            schema = tableInfo.getSchema();
        }

        return TermRelation.builder()
                .id(po.getId())
                .termId(po.getTermId())
                .entityType(po.getEntityType())
                .tableId(po.getTableId())
                .fieldName(po.getFieldName())
                .tableName(tableName)
                .schema(schema)
                .build();
    }

}
